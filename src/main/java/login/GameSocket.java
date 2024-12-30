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
package login;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import network.CenterAcceptor.CenterDecoder;
import network.CenterAcceptor.CenterEncoder;
import network.packet.ByteBufOutPacket;
import network.packet.CenterPacket;
import network.packet.InPacket;
import network.packet.OutPacket;
import network.packet.Packet;
import util.Logger;

/**
 *
 * @author Eric
 */
public class GameSocket extends SimpleChannelInboundHandler {
    private final Channel channel;
    private final Lock lockSend;
    private boolean closePosted;
    private String addr;
    
    public byte worldID;
    public String worldName;
    
    public GameSocket(Channel channel) {
        this.channel = channel;
        this.lockSend = new ReentrantLock();
        this.closePosted = false;
        this.addr = "";
        this.worldName = "";
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
	    channel.pipeline().addBefore("GameSocket", "CenterEncoder", new CenterEncoder());
	    channel.pipeline().addBefore("GameSocket", "CenterDecoder", new CenterDecoder());
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        try {
            onClose();
        } finally {
            ctx.channel().close();
        }
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
        super.exceptionCaught(ctx, cause);
    }
    
    public final String getAddr() {
        if (addr.isEmpty() && channel != null) {
            return ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress().split(":")[0];
        }
        return addr;
    }
    
    public void onClose() {
        LoginApp.getInstance().getCenterAcceptor().removeSocket(this);
	
	    WorldEntry world = LoginApp.getInstance().getWorld(worldID);
	    if (world != null) {
	    	world.getChannels().clear();
	    	
	    	LoginApp.getInstance().getWorlds().remove(world);
	    }
    }
    
    public void onGameConnected(InPacket packet) {
        this.worldID = packet.decodeByte();
        this.worldName = packet.decodeString();

        WorldEntry world = LoginApp.getInstance().getWorld(worldID);
        if (world == null) {
            world = new WorldEntry(this, worldID, worldName);

            LoginApp.getInstance().addWorld(world);
        }

        world.addChannel(new ChannelEntry(world.getWorldID(), packet.decodeByte(), packet.decodeString(), packet.decodeShort()));
        LoginApp.getInstance().getCenterAcceptor().addSocket(this);
    }
    
    public void onGameMigrateRequest(int characterID) {
        ShopEntry shop = LoginApp.getInstance().getShop();
        
        OutPacket packet = new ByteBufOutPacket(CenterPacket.GameMigrateRes);
        packet.encodeInt(characterID);
        if (shop != null) {
            if (shop.getUsers().containsKey(characterID)) {
                ChannelEntry ch = shop.getUsers().remove(characterID);
                
                packet.encodeBool(true);
                packet.encodeString(ch.getAddr());
                packet.encodeShort(ch.getPort());
            } else {
                packet.encodeBool(false);
            }
        } else {
            packet.encodeBool(false);
        }
        sendPacket(packet, false);
    }
    
    public void onShopConnected(InPacket packet) {
        ShopEntry shop = new ShopEntry(this, packet.decodeString(), packet.decodeShort());
            
        LoginApp.getInstance().setShop(shop);
    }
    
    public void onShopMigrateRequest(int characterID, byte worldID, byte channelID) {
        ShopEntry shop = LoginApp.getInstance().getShop();
            
        OutPacket packet = new ByteBufOutPacket(CenterPacket.ShopMigrateRes);
        packet.encodeInt(characterID);
        if (shop != null) {
            shop.getUsers().put(characterID, LoginApp.getInstance().getWorld(worldID).getChannel(channelID));
            
            packet.encodeBool(true);
            packet.encodeString(shop.getAddr());
            packet.encodeShort(shop.getPort());
            sendPacket(packet, false);
        } else {
            packet.encodeBool(false);
        }
    }
    
    public boolean postClose() {
        if (!closePosted) {
            closePosted = true;
            return false;
        } else {
            onClose();
            return true;
        }
    }
    
    public void processPacket(InPacket packet) {
        final short type = packet.decodeShort();
        
        switch (type) {
            case CenterPacket.InitGameSvr:
                onGameConnected(packet);
                break;
            case CenterPacket.InitShopSvr:
                onShopConnected(packet);
                break;
            case CenterPacket.ShopMigrateReq:
                onShopMigrateRequest(packet.decodeInt(), packet.decodeByte(), packet.decodeByte());
                break;
            case CenterPacket.GameMigrateReq:
                onGameMigrateRequest(packet.decodeInt());
                break;
            default: {
                Logger.logReport("Unidentified Center Packet [%d] : %s", type, packet.toString());
            }
        }
    }
    
    public void sendPacket(Packet packet, boolean force) {
        lockSend.lock();
        try {
            if (!closePosted || force) {
                channel.writeAndFlush(packet.getBytes());
            }
        } finally {
            lockSend.unlock();
        }
    }
}
