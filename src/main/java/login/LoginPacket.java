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

import common.user.CharacterData;
import common.user.DBChar;
import java.util.List;
import login.user.ClientSocket;
import network.packet.ByteBufOutPacket;
import network.packet.LoopbackPacket;
import network.packet.OutPacket;
import network.packet.Packet;

/**
 * @author Eric
 */
public class LoginPacket {

  public static Packet onCheckPasswordResult(ClientSocket socket, int result) {
    OutPacket packet = new ByteBufOutPacket(LoopbackPacket.CheckPasswordResult);
    packet.encodeInt(result);
    packet.encodeShort(0); // TODO: What is this?
    if (result == 0) {
      packet.encodeInt(socket.getAccountID());
      packet.encodeByte(socket.getGender());
      packet.encodeByte(socket.getGradeCode());
      packet.encodeByte(0x4E); // TODO: Is this Country Code?
      packet.encodeString(socket.getNexonClubID());

      // TODO: Wtf are these supposed to be?
      packet.encodeBytes(
          new byte[]{3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xDC, 0x3D, 0x0B, 0x28, 0x64,
              (byte) 0xC5, 1, 8, 0, 0, 0});

      // KMS Alpha stuff
//      packet.encodeByte(LoginApp.getInstance().getWorlds().size());
//      for (WorldEntry world : LoginApp.getInstance().getWorlds()) {
//        packet.encodeByte(world.getChannels().size());
//        for (ChannelEntry channel : world.getChannels()) {
//          packet.encodeString(world.getName() + "-" + (channel.getChannelID() + 1));
//          packet.encodeInt(channel.getUserNo());
//          packet.encodeByte(channel.getWorldID());
//          packet.encodeByte(channel.getChannelID());
//        }
//      }
    }
    return packet;
  }

  public static OutPacket onCheckPinResult(int result) {
    OutPacket packet = new ByteBufOutPacket(LoopbackPacket.PinOperation);
    packet.encodeByte(result);
    return packet;
  }

  public static OutPacket onSelectWorldResult(int msg, int ssn, List<CharacterData> characters) {
    OutPacket packet = new ByteBufOutPacket(LoopbackPacket.SelectWorldResult);
    packet.encodeByte(msg);
    if (msg == 1) {
      packet.encodeInt(ssn);

      packet.encodeByte(characters.size());
      for (CharacterData character : characters) {
        character.encode(packet, DBChar.Character | DBChar.ItemSlotEquip);
      }
    }
    return packet;
  }

  public static OutPacket onCheckDuplicateIDResult(String checkedName, boolean nameUsed) {
    OutPacket packet = new ByteBufOutPacket(LoopbackPacket.CheckDuplicatedIDResult);
    packet.encodeString(checkedName);
    packet.encodeBool(nameUsed);
    return packet;
  }

  public static OutPacket onCreateNewCharacterResult(int msg, CharacterData character) {
    OutPacket packet = new ByteBufOutPacket(LoopbackPacket.CreateNewCharacterResult);
    packet.encodeByte(msg);
    if (msg == 0) {
      character.encode(packet, DBChar.Character | DBChar.ItemSlotEquip);
    }
    return packet;
  }

  public static OutPacket onDeleteCharacterResult(int characterID, int msg) {
    OutPacket packet = new ByteBufOutPacket(LoopbackPacket.DeleteCharacterResult);
    packet.encodeInt(characterID);
    packet.encodeByte(msg);
    return packet;
  }

  public static OutPacket onSelectCharacterResult(int result, int ip, short port, int characterID) {
    OutPacket packet = new ByteBufOutPacket(LoopbackPacket.SelectCharacterResult);
    packet.encodeByte(result);
    if (result == 1) {
      packet.encodeInt(ip);
      packet.encodeShort(port);
      packet.encodeInt(characterID);
    }
    return packet;
  }
}
