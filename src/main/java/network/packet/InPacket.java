package network.packet;

import java.awt.Point;

public interface InPacket extends Packet {

  byte decodeByte();

  int decodeByte(boolean unsigned);

  short decodeShort();

  int decodeInt();

  long decodeLong();

  boolean decodeBoolean();

  Point decodePos();

  String decodeString();

  byte[] decodeBytes(int numberOfBytes);

  void skip(int numberOfBytes);

  int available();

  void seek(int byteOffset);

  int getOffset();
}

