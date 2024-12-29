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
package network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import network.encryption.MapleAESOFB;
import network.encryption.MapleCustomEncryption;
import network.packet.Packet;

/**
 * The server-end networking encoder. Sends the incoming socket buffer list to the remote socket.
 *
 * @author Eric
 */
public class SocketEncoder extends MessageToByteEncoder<Packet> {

  private final MapleAESOFB sendCypher;

  public SocketEncoder(MapleAESOFB sendCypher) {
    this.sendCypher = sendCypher;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, Packet in, ByteBuf out) {
    byte[] packet = in.getBytes();
    out.writeBytes(getEncodedHeader(packet.length));

    MapleCustomEncryption.encryptData(packet);
    sendCypher.crypt(packet);
    out.writeBytes(packet);
  }

  private byte[] getEncodedHeader(int length) {
    return sendCypher.getPacketHeader(length);
  }
}
