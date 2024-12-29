/*
 * This file is part of OrionAlpha, a MapleStory Emulator Project.
 * Copyright (C) 2018 Eric Smith <notericsoft@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package game.user;

import common.OrionConfig;
import game.GameApp;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import network.SocketCodec;
import network.SocketDecoder;
import network.SocketEncoder;
import network.encryption.ClientCyphers;
import network.packet.ByteBufOutPacket;
import network.packet.ClientPacket;
import network.packet.InPacket;
import network.packet.InitializationVector;
import network.packet.LoopbackPacket;
import network.packet.OutPacket;
import network.packet.Packet;
import util.Logger;

/**
 *
 * @author Eric
 */
public class ClientSocket extends SimpleChannelInboundHandler {
    public class MigrateState {
        public static final int
                Invalid                     = 0,
                WaitMigrateIn               = 1,
                WaitCenterMigrateInResult   = 2,
                Identified                  = 3,
                WaitCenterMigrateOutResult  = 4,
                WaitMigrateOut              = 5,
                Disconnected                = 6
        ;
    }
    
    private final Lock lockSend;
    private int migrateState;
    private int characterID;
    private byte channelID;
    private User user;
    private final Channel channel;
    private int localSocketSN;
    private final AtomicBoolean closePosted;
    private final AtomicBoolean updatePosted;
    private String addr;
    private long acceptTime;
    private long migrateOut;
    
    public ClientSocket(Channel socket) {
        this.channel = socket;
        this.lockSend = new ReentrantLock();
        this.migrateState = MigrateState.WaitMigrateIn;
        this.characterID = 0;
        this.user = null;
        this.closePosted = new AtomicBoolean(false);
        this.updatePosted = new AtomicBoolean(false);
        this.localSocketSN = 0;
        this.addr = "";
        this.acceptTime = 0;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        final InitializationVector sendIv = InitializationVector.generateSend();
        final InitializationVector recvIv = InitializationVector.generateReceive();
        channel.pipeline().addBefore("ClientSocket", "AliveAck", new IdleStateHandler(20, 15, 0));
        channel.pipeline().addBefore("ClientSocket", "SocketCodec", new SocketCodec(ClientCyphers.of(sendIv, recvIv)));
        OutPacket packet = new ByteBufOutPacket();
        packet.encodeShort(14);
        packet.encodeShort(OrionConfig.CLIENT_VER);
        packet.encodeShort(OrionConfig.CLIENT_PATCH);
        packet.encodeByte(49);
        packet.encodeBytes(recvIv.getBytes());
        packet.encodeBytes(sendIv.getBytes());
        packet.encodeByte(OrionConfig.GAME_LOCALE);
        acceptTime = System.currentTimeMillis();
        channel.writeAndFlush(Unpooled.wrappedBuffer(packet.getBytes()));
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        try {
            onClose();
        } finally {
            ctx.channel().close();
        }
        super.channelInactive(ctx);
    }
    
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (ctx == null || msg == null) {
            return;
        }
        InPacket packet = (InPacket) msg;
        if (packet.available() < 1) {
            return;
        }
        processPacket(packet);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (ctx == null || (cause instanceof IOException || cause instanceof ClassCastException)) {
            return;
        }
        cause.printStackTrace(System.err);
        super.exceptionCaught(ctx, cause);
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                if (user != null && migrateState == MigrateState.WaitCenterMigrateOutResult) {
                    // If a user has been migrating for over 30 seconds, dc and prepare a
                    // HandleUserMigrateTimeout on the center JVM.
                    if (System.currentTimeMillis() - migrateOut > 30000) {
                        channelInactive(ctx);
                    }
                // If there's no active user and they aren't migrating, then they have no
                // excuse as to why they have no read operations for 20 seconds straight.
                } else {
                    channelInactive(ctx);
                }
            // Handle the AliveAck on the client-end if this ever occurs.
            // If the user hits over a 15-second writer timeout -> disconnect.
            } else if (e.state() == IdleState.WRITER_IDLE) {
                channel.writeAndFlush(onAliveReq((int) (System.currentTimeMillis() / 1000)));
            }
        }
    }
    
    public String getAddr() {
        return addr;
    }
    
    public game.Channel getChannel() {
        return GameApp.getInstance().getChannel(getChannelID());
    }
    
    public byte getChannelID() {
        return channelID;
    }
    
    public int getCharacterID() {
        return characterID;
    }
    
    public int getLocalSocketSN() {
        return localSocketSN;
    }
    
    public int getMigrateState() {
        return migrateState;
    }
    
    public final String getSocketRemoteIP() {
        return ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress().split(":")[0];
    }

    public User getUser() {
        return user;
    }
    
    public void onClose() {
        if (this.migrateState == MigrateState.Invalid || this.migrateState == MigrateState.WaitMigrateIn) {
            this.characterID = 0;
        }
        getChannel().getAcceptor().incRemainedSocket();
        getChannel().getAcceptor().removeSocket(this);
        if (this.user != null) {
            this.user.onSocketDestroyed(false);
            this.user.destructUser();
            this.user = null;
        }
        this.channel.close();
        getChannel().getAcceptor().decRemainedSocket();
    }
    
    public void onMigrateIn(InPacket packet) {
        if (migrateState == MigrateState.WaitMigrateIn) {
            characterID = packet.decodeInt();
            
            user = new User(this);
            if (user.getCharacterID() == characterID) {
                migrateState = MigrateState.Identified;
                if (getChannel().registerUser(user)) {
                    user.onMigrateInSuccess();
                    return;
                }
                Logger.logError("Duplicated login [%s]", addr);
            } else {
                Logger.logError("CharacterID does not match!");
            }
        }
        postClose();
    }
    
    public void onUpdate(long time) {
        if (migrateState == MigrateState.WaitMigrateIn && time - acceptTime >= GameApp.getInstance().getWaitingFirstPacket()) {
            Logger.logError("Disconnect dummy connection - %d(ms) : %s", (time - acceptTime), addr);
            postClose();
        }
        time = System.currentTimeMillis();
        if (user != null) {
            user.update(time);
        }
        updatePosted.set(false);
    }
    
    public boolean postClose() {
        if (closePosted.compareAndSet(false, true)) {
            onClose();
            return true;
        }
        return false;
    }
    
    private void processPacket(InPacket packet) {
        final byte type = packet.decodeByte();
        if (OrionConfig.LOG_PACKETS) {
            Logger.logReport("[Packet Logger] [0x%s]: %s", Integer.toHexString(type).toUpperCase(), packet.toString());
        }
        if (type == ClientPacket.AliveAck) {
            packet.decodeInt();//Acknowledged alive ack
        } else if (type == ClientPacket.AliveReq) {
            packet.decodeInt();//Received client alive req
            
            sendPacket(onAliveReq((int)(System.currentTimeMillis() / 1000)), false);
        } else if (type == ClientPacket.MigrateIn) {
            onMigrateIn(packet);
        } else {
            if (type < ClientPacket.BEGIN_USER) {
                Logger.logReport("[Unidentified Packet] [0x" + Integer.toHexString(type).toUpperCase() + "]: " + packet.toString());
                return;
            }
            processUserPacket(type, packet);
        }
    }
    
    private void processUserPacket(byte type, InPacket packet) {
        try {
            if (migrateState < MigrateState.Identified || migrateState > MigrateState.WaitMigrateOut) {
                if (migrateState != MigrateState.Disconnected) {
                    postClose();
                }
            } else {
                if (user != null) {
                    user.onPacket(type, packet);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }
    
    public void setLocalSocketSN(int sn) {
        this.localSocketSN = sn;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }
    
    public void setChannelID(int channel) {
        this.channelID = (byte) channel;
    }
    
    public void sendPacket(Packet packet, boolean force) {
        lockSend.lock();
        try {
            if (!this.closePosted.get() || force) {
                sendPacket(packet);
            }
        } finally {
            lockSend.unlock();
        }
    }
    
    private void sendPacket(Packet packet) {
        lockSend.lock();
        try {
            if (channel == null || packet == null) {
                throw new RuntimeException("fuck everything");
            }
            channel.writeAndFlush(packet);
        } finally {
            lockSend.unlock();
        }
    }
    
    /**
     * Migrates a remote client to a different game server.
     *
     * @param isGuestAccount If the requested account is a guest, crash them for hacking. (guests can't cc!)
     * @param ip The IP Address of the requested game server.
     * @param port The port the game server is on.
     * @return The server migration packet.
    */
    public static OutPacket onMigrateCommand(boolean isGuestAccount, int ip, int port) {
        OutPacket packet = new ByteBufOutPacket(LoopbackPacket.MigrateCommand);
        packet.encodeBool(!isGuestAccount);
        if (!isGuestAccount) {
            packet.encodeInt(ip);
            packet.encodeShort(port);
        }
        return packet;
    }
    
    /**
     * Sends an Alive Req to the client and an Alive Ack back to the server.
     *
     * @param time
     * @return 
    */
    public static OutPacket onAliveReq(int time) {
        OutPacket packet = new ByteBufOutPacket(LoopbackPacket.AliveReq);
        packet.encodeInt(time);
        return packet;
    }
}
