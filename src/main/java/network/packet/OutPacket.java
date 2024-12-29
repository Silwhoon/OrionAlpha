package network.packet;

import java.awt.Point;
import util.FileTime;

public interface OutPacket extends Packet {

  static OutPacket create(short opcode) {
    return new ByteBufOutPacket(opcode);
  }

  void encodeByte(byte value);

  void encodeByte(int value);

  void encodeBytes(byte[] value);

  void encodeShort(int value);

  void encodeInt(int value);

  void encodeLong(long value);

  void encodeDouble(double value);

  void encodeBool(boolean value);

  void encodeString(String value);

  void encodeString(String value, int size);

  void encodeFixedString(String value);

  void encodePos(Point value);

  void encodeFileTime(FileTime value);

  void skip(int numberOfBytes);
}
