package network.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.awt.Point;
import java.nio.charset.StandardCharsets;
import util.FileTime;

public class ByteBufOutPacket implements OutPacket {

  private final ByteBuf byteBuf;

  public ByteBufOutPacket() {
    this.byteBuf = Unpooled.buffer();
  }

  public ByteBufOutPacket(short op) {
    ByteBuf byteBuf = Unpooled.buffer();
    byteBuf.writeShortLE(op);
    this.byteBuf = byteBuf;
  }

  public ByteBufOutPacket(short op, int initialCapacity) {
    ByteBuf byteBuf = Unpooled.buffer(initialCapacity);
    byteBuf.writeShortLE(op);
    this.byteBuf = byteBuf;
  }

  @Override
  public byte[] getBytes() {
    return ByteBufUtil.getBytes(byteBuf);
  }

  @Override
  public void encodeByte(byte value) {
    byteBuf.writeByte(value);
  }

  @Override
  public void encodeByte(int value) {
    encodeByte((byte) value);
  }

  @Override
  public void encodeBytes(byte[] value) {
    byteBuf.writeBytes(value);
  }

  @Override
  public void encodeShort(int value) {
    byteBuf.writeShortLE(value);
  }

  @Override
  public void encodeInt(int value) {
    byteBuf.writeIntLE(value);
  }

  @Override
  public void encodeLong(long value) {
    byteBuf.writeLongLE(value);
  }

  @Override
  public void encodeDouble(double value) {
    byteBuf.writeDoubleLE(value);
  }

  @Override
  public void encodeBool(boolean value) {
    byteBuf.writeByte(value ? 1 : 0);
  }

  @Override
  public void encodeString(String value) {
    byte[] stringBytes = value.getBytes(StandardCharsets.US_ASCII);
    encodeShort((short) stringBytes.length);
    encodeBytes(stringBytes);
  }

  @Override
  public void encodeString(String value, int size) {
    byte[] src = value.getBytes(StandardCharsets.US_ASCII);

    for (int i = 0; i < size; i++) {
      if (i >= src.length) {
        encodeByte('\0');
      } else {
        encodeByte(src[i]);
      }
    }
  }

  @Override
  public void encodeFixedString(String value) {
    // TODO: Allow different charsets
    encodeBytes(value.getBytes(StandardCharsets.US_ASCII));
  }

  @Override
  public void encodePos(Point value) {
    encodeShort((short) value.getX());
    encodeShort((short) value.getY());
  }

  @Override
  public void encodeFileTime(FileTime fileTime) {
    encodeInt(fileTime.getLowDateTime());
    encodeInt(fileTime.getHighDateTime());
  }

  @Override
  public void skip(int numberOfBytes) {
    encodeBytes(new byte[numberOfBytes]);
  }
}
