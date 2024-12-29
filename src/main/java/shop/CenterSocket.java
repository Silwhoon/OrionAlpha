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
package shop;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.json.JsonObject;
import network.ShopAcceptor;
import network.packet.ByteBufInPacket;
import network.packet.ByteBufOutPacket;
import network.packet.CenterPacket;
import network.packet.InPacket;
import network.packet.OutPacket;
import network.packet.Packet;
import shop.user.ClientSocket;
import shop.user.User;
import util.Logger;
import util.Utilities;

/**
 * @author Eric
 */
public class CenterSocket extends SimpleChannelInboundHandler {

  private Channel channel;
  private Bootstrap bootstrap;
  private final EventLoopGroup workerGroup;
  private final Lock lock;
  private final Lock lockSend;
  private boolean closePosted;
  private String worldName;
  private String addr;
  private int port;

  public CenterSocket() {
    this.worldName = "";
    this.addr = "";
    this.closePosted = false;
    this.lock = new ReentrantLock();
    this.lockSend = new ReentrantLock();
    this.workerGroup = new NioEventLoopGroup();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    Logger.logReport("Center socket closed");
    try {
      postCloseMessage();
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
    super.exceptionCaught(ctx, cause);
  }

  public void connect() {
    try {
      bootstrap = new Bootstrap()
          .group(workerGroup)
          .channel(NioSocketChannel.class)
          .option(ChannelOption.SO_KEEPALIVE, true)
          .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
              ch.pipeline().addLast(
                  new CenterDecoder(),
                  CenterSocket.this,
                  new CenterEncoder()
              );
            }
          });

      // Connect to the Center socket
      channel = bootstrap.connect(getAddr(), getPort())
          .syncUninterruptibly()
          .channel()
          .closeFuture()
          .channel();

      // Send the Center Server the Login server information request
      OutPacket packet = new ByteBufOutPacket(CenterPacket.InitShopSvr);
      packet.encodeString(ShopApp.getInstance().getAddr());
      packet.encodeShort(ShopApp.getInstance().getPort());
      sendPacket(packet);

      Logger.logReport("Center socket connected successfully");
    } catch (Exception ex) {
      ex.printStackTrace(System.err);
    }
  }

  public String getAddr() {
    return addr;
  }

  public int getPort() {
    return port;
  }

  public String getWorldName() {
    return worldName;
  }

  public void init(JsonObject data) {
    this.addr = data.getString("ip", "127.0.0.1");
    this.port = data.getInt("port", 8383);
    this.worldName = data.getString("worldName", "OrionAlpha");
  }

  public void onGameMigrateResult(InPacket packet) {
    int characterID = packet.decodeInt();

    User user = User.findUser(characterID);
    if (user != null) {
      if (packet.decodeBoolean()) {
        user.sendPacket(
            ClientSocket.onMigrateCommand(false, Utilities.netIPToInt32(packet.decodeString()),
                packet.decodeShort()));
      } else {
        // Might as well close socket or something tbh..
      }
    }
  }

  public void postCloseMessage() {
    lock.lock();
    try {
      if (!closePosted) {
        closePosted = true;
        channel.close();
        workerGroup.shutdownGracefully();
      }
    } finally {
      lock.unlock();
    }
  }

  public void processPacket(InPacket packet) {
    if (ShopAcceptor.getInstance() != null) {
      final byte type = packet.decodeByte();

      switch (type) {
        case CenterPacket.GameMigrateRes:
          onGameMigrateResult(packet);
          break;
        default: {
          Logger.logReport("Packet received: %s", packet);
        }
      }
    }
  }

  public void sendPacket(Packet packet) {
    if (channel != null && channel.isActive()) {
      lockSend.lock();
      try {
        channel.writeAndFlush(packet);
      } finally {
        lockSend.unlock();
      }
    }
  }

  private static class CenterDecoder extends ReplayingDecoder<Void> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
      int length = in.readInt();

      byte[] src = new byte[length];
      in.readBytes(src);

      InPacket packet = new ByteBufInPacket(Unpooled.wrappedBuffer(src));
      out.add(packet);
    }
  }

  private static class CenterEncoder extends MessageToByteEncoder<byte[]> {

    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] message, ByteBuf out) throws Exception {
      out.writeInt(message.length);
      out.writeBytes(Arrays.copyOf(message, message.length));
    }
  }
}
