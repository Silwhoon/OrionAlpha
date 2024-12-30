package network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import java.util.List;
import network.encryption.InvalidPacketHeaderException;
import network.encryption.MapleAESOFB;
import network.encryption.MapleCustomEncryption;
import network.packet.ByteBufInPacket;

public class SocketDecoder extends ReplayingDecoder<Void> {

  private final MapleAESOFB receiveCypher;

  public SocketDecoder(MapleAESOFB receiveCypher) {
    this.receiveCypher = receiveCypher;
  }

  /**
   * @param header Packet header - the first 4 bytes of the packet
   * @return Packet size in bytes
   */
  private static int decodePacketLength(byte[] header) {
    return (((header[1] ^ header[3]) & 0xFF) << 8) | ((header[0] ^ header[2]) & 0xFF);
  }

  @Override
  protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out) {
    final int header = in.readInt();

    if (!receiveCypher.isValidHeader(header)) {
      throw new InvalidPacketHeaderException("Attempted to decode a packet with an invalid header", header);
    }

    final int packetLength = MapleAESOFB.getPacketLength(header);
    byte[] packet = new byte[packetLength];
    in.readBytes(packet);
    receiveCypher.crypt(packet);
    MapleCustomEncryption.decryptData(packet);
    out.add(new ByteBufInPacket(Unpooled.wrappedBuffer(packet)));
  }
}

