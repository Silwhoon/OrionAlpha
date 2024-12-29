package network.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.awt.Point;
import java.nio.charset.StandardCharsets;

public class ByteBufInPacket implements InPacket {

  private final ByteBuf byteBuf;

  public ByteBufInPacket(ByteBuf byteBuf) {
    this.byteBuf = byteBuf;
  }

  private static String insertReaderPosition(String hexDump, int index) {
    StringBuilder sb = new StringBuilder(hexDump);
    sb.insert(2 * index, '_');
    return sb.toString();
  }

  @Override
  public byte[] getBytes() {
    return ByteBufUtil.getBytes(byteBuf);
  }

  @Override
  public byte decodeByte() {
    return byteBuf.readByte();
  }

  @Override
  public int decodeByte(boolean unsigned) {
    if (!unsigned) {
      return decodeByte();
    }
    return byteBuf.readUnsignedByte();
  }

  @Override
  public short decodeShort() {
    return byteBuf.readShortLE();
  }

  @Override
  public int decodeInt() {
    return byteBuf.readIntLE();
  }

  @Override
  public long decodeLong() {
    return byteBuf.readLongLE();
  }

  @Override
  public boolean decodeBoolean() {
    return byteBuf.readBoolean();
  }

  @Override
  public Point decodePos() {
    final short x = byteBuf.readShortLE();
    final short y = byteBuf.readShortLE();
    return new Point(x, y);
  }

  @Override
  public String decodeString() {
    short length = decodeShort();
    byte[] stringBytes = new byte[length];
    byteBuf.readBytes(stringBytes);
    // TODO: Allow different charsets
    return new String(stringBytes, StandardCharsets.US_ASCII);
  }

  @Override
  public byte[] decodeBytes(int numberOfBytes) {
    byte[] bytes = new byte[numberOfBytes];
    byteBuf.readBytes(bytes);
    return bytes;
  }

  @Override
  public void skip(int numberOfBytes) {
    byteBuf.skipBytes(numberOfBytes);
  }

  @Override
  public int available() {
    return byteBuf.readableBytes();
  }

  @Override
  public void seek(int byteOffset) {
    byteBuf.readerIndex(byteOffset);
  }

  @Override
  public int getOffset() {
    return byteBuf.readerIndex();
  }

  @Override
  public String toString() {
    final int readerIndex = byteBuf.readerIndex();
    byteBuf.markReaderIndex();
    byteBuf.readerIndex(0);

    String hexDumpWithPosition = insertReaderPosition(ByteBufUtil.prettyHexDump(byteBuf).toUpperCase(),
        readerIndex);
    String toString = String.format("ByteBufInPacket[%s]", hexDumpWithPosition);

    byteBuf.resetReaderIndex();
    return toString;
  }
}
