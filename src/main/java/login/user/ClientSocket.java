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
package login.user;

import common.JobAccessor;
import common.OrionConfig;
import common.user.CharacterData;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import login.ChannelEntry;
import login.LoginApp;
import login.LoginPacket;
import login.PinCodeValidator;
import login.WorldEntry;
import login.user.item.NewCharEquipType;
import network.LoginAcceptor;
import network.SocketCodec;
import network.database.LoginDB;
import network.encryption.ClientCyphers;
import network.packet.ByteBufOutPacket;
import network.packet.ClientPacket;
import network.packet.InPacket;
import network.packet.InitializationVector;
import network.packet.LoopbackPacket;
import network.packet.OutPacket;
import network.packet.Packet;
import util.Logger;
import util.Pointer;
import util.Utilities;

/**
 * @author Eric
 */
public class ClientSocket extends SimpleChannelInboundHandler {

  private static final int MAX_CHARACTERS = 15;

  private final Channel channel;
  private final Lock lockSend;

  private int loginState;
  private int failCount;
  private int accountID;
  public short worldID;
  private byte channelID;
  public int characterID;
  private byte gender;
  private byte gradeCode;
  private String nexonClubID;
  private int birthDate;//Doesn't exist, this is KSSN instead.
  private boolean closePosted;
  private final Map<Integer, String> characters;

  private int localSocketSN;
  private int ssn;
  private int ipBlockType;
  private long aliveReqSent;
  private long lastAliveAck;
  private boolean firstAliveAck;
  private long ipCheckRequestSent;
  private boolean tempBlockedIP;
  private boolean processed;

  public ClientSocket(Channel socket) {
    this.channel = socket;
    this.lockSend = new ReentrantLock();
    this.loginState = 1;
    this.failCount = 0;
    this.accountID = 0;
    this.worldID = -1;
    this.channelID = -1;
    this.gradeCode = 0;
    this.nexonClubID = "";
    this.localSocketSN = 0;
    this.ssn = 0;
    this.aliveReqSent = 0;
    //this.ipBlockType = IPFilter.Permit;
    this.lastAliveAck = 0;
    this.firstAliveAck = true;
    this.ipCheckRequestSent = 0;
    this.tempBlockedIP = false;
    this.processed = false;
    this.characters = new HashMap<>();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    final InitializationVector sendIv = InitializationVector.generateSend();
    final InitializationVector recvIv = InitializationVector.generateReceive();
    channel.pipeline().addBefore("ClientSocket", "AliveAck", new IdleStateHandler(20, 15, 0));
    channel.pipeline().addBefore("ClientSocket", "SocketCodec", new SocketCodec(
        ClientCyphers.of(sendIv, recvIv)));
    OutPacket packet = new ByteBufOutPacket();
    packet.encodeShort(14);
    packet.encodeShort(OrionConfig.CLIENT_VER);
    packet.encodeShort(OrionConfig.CLIENT_PATCH);
    packet.encodeByte(49);
    packet.encodeBytes(recvIv.getBytes());
    packet.encodeBytes(sendIv.getBytes());
    packet.encodeByte(OrionConfig.GAME_LOCALE);
    channel.writeAndFlush(Unpooled.wrappedBuffer(packet.getBytes()));
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    ChannelFuture writeFuture = ctx.channel().closeFuture();
    if (writeFuture != null) {
      writeFuture.awaitUninterruptibly();
    }
    closeSocket();
    ctx.channel().closeFuture().awaitUninterruptibly();
    ctx.channel().close().awaitUninterruptibly();
    super.channelInactive(ctx);
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (ctx == null || msg == null) {
      return;
    }
    InPacket packet = (InPacket) msg;
    try {
      if (packet.available() < 1) {
        return;
      }
      processPacket(packet);
    } finally {
      // TODO: What is the purpose of this?
      if (packet.getOffset() == 4) {
        //super.channelRead(ctx, msg);
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (ctx == null || (cause instanceof IOException || cause instanceof ClassCastException)) {
      return;
    }
    super.exceptionCaught(ctx, cause);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      //OnAliveAck();
    }
  }

  public void closeSocket() {
    onClose();
  }

  public String getNexonClubID() {
    return nexonClubID;
  }

  public int getAccountID() {
    return accountID;
  }

  public byte getChannelID() {
    return channelID;
  }

  public byte getGender() {
    return gender;
  }

  public byte getGradeCode() {
    return gradeCode;
  }

  public int getLocalSocketSN() {
    return localSocketSN;
  }

  public int getSSN() {
    return ssn;
  }

  public final String getSocketRemoteIP() {
    return ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress()
        .split(":")[0];
  }

  public short getWorldID() {
    return worldID;
  }

  public boolean isAdmin() {
    return this.gradeCode >= 4;
    //return (nGradeCode & 0x1) > 0;
  }

  public boolean isLimitedIP() {
    return ((this.gradeCode >> 3) & 0x1) > 0;
  }

  public void onAliveAck() {
    long cur = System.currentTimeMillis();
    if (this.aliveReqSent > 0 && (cur - this.aliveReqSent) <= 60000 * (this.firstAliveAck ? 8
        : 3)) {
      this.aliveReqSent = 0;
      this.firstAliveAck = false;
      this.lastAliveAck = cur;
    } else {
      Logger.logError("Alive check failed : Time-out");
      closeSocket();
    }
  }

  public void onClose() {
    if (this.channel == null) {
      return;
    }
    if (this.accountID != 0) {
      // Remove from AdminSocket memory for user banning. -> Ignore since we have no AdminSocket.
    }
    if (this.loginState < 7 || this.loginState >= 12) {
      this.accountID = 0;
    }
    this.loginState = 13;
    Logger.logReport("Client socket disconnected");
    LoginAcceptor.getInstance().removeSocket(this);

    this.channel.close();
  }

  public void onCheckDuplicateID(InPacket packet) {
    if (this.loginState == 8) {
      String checkedName = packet.decodeString();
      boolean nameUsed = LoginDB.rawCheckDuplicateID(checkedName, this.accountID);

      sendPacket(LoginPacket.onCheckDuplicateIDResult(checkedName, nameUsed), false);
    } else {
      postClose();
    }
  }

  public void onCheckPassword(InPacket packet) {
    if (this.loginState != 1) {
      onClose();
      return;
    }
    String id = packet.decodeString();
    String passwd = packet.decodeString();

    if (id == null || id.isEmpty()) {
      return;
    }

    int retCode = LoginDB.rawCheckPassword(id, passwd, this);
    if (retCode == 0) {
      retCode = LoginDB.rawCheckUserConnected(this.accountID);
    }

    sendPacket(LoginPacket.onCheckPasswordResult(this, retCode), false);
  }

  public void onBackToWorldSelect(InPacket packet) {
    if (this.loginState != 8) {
      onClose();
      return;
    }

    // TODO: Assign correct loginState
    this.loginState = 1;
  }

  public void onPinAction(InPacket packet) {
    if (this.loginState != 1) {
      onClose();
      return;
    }
    byte action = packet.decodeByte();
    if (action == 0) {
      // User cancelled
      // TODO: Handle log out here?
    } else if (action == 1) {
      // User submitted
      boolean firstLogin = packet.decodeBoolean();
      if (firstLogin) {
        // User has only entered their password, send them what they need to do next
        int retCode = LoginDB.rawGetPinRequest(this.accountID);
        sendPacket(LoginPacket.onCheckPinResult(retCode), false);
      } else {
        // User has entered their pin, validate it and act accordingly
        packet.seek(8); // TODO: What are we skipping over here
        String pin = packet.decodeString();

        if (!PinCodeValidator.isValidPin(pin)) {
          return;
        }

        int retCode = LoginDB.rawCheckPin(this.accountID, pin);
        sendPacket(LoginPacket.onCheckPinResult(retCode), false);
      }
    } else if (action == 2) {
      // User changed
      byte unk = packet.decodeByte(); // TODO: Can this be anything other than 0?

      // Send a 'change pin' request to the user
      // But they must have entered their existing pin correctly
      packet.seek(8); // TODO: What are we skipping over here
      String pin = packet.decodeString();

      if (!PinCodeValidator.isValidPin(pin)) {
        return;
      }

      int retCode = LoginDB.rawCheckPin(this.accountID, pin);
      if (retCode == 0) {
        // Request pin change successful
        sendPacket(LoginPacket.onCheckPinResult(1), false);
      } else {
        // Invalid pin entered
        sendPacket(LoginPacket.onCheckPinResult(2), false);
      }
    }
  }

  public void onRegisterPin(InPacket packet) {
    if (this.loginState != 1) {
      onClose();
      return;
    }

    byte action = packet.decodeByte();
    if (action == 0) {
      // User cancelled
      // TODO: Handle logout here?
      return;
    }
    // TODO: Can action be anything other than 0 or 1?

    String pin = packet.decodeString();
    if (!PinCodeValidator.isValidPin(pin)) {
      return;
    }

    if (LoginDB.rawChangePin(this.accountID, pin)) {
      // Successfully registered pin
      sendPacket(LoginPacket.onRegisterPinResult(0), false);
    } else {
      // System error
      sendPacket(LoginPacket.onRegisterPinResult(1), false);
    }
  }

  public void onRequestWorldInfo(InPacket packet) {
    if (this.loginState != 1) {
      onClose();
      return;
    }

    for (WorldEntry world : LoginApp.getInstance().getWorlds()) {
      sendPacket(LoginPacket.onSendWorldList(world), false);
    }

    sendPacket(LoginPacket.onSendWorldList(null), false);
  }

  public void onHoverWorld(InPacket packet) {
    short worldID = packet.decodeShort();
    WorldEntry pWorld = LoginApp.getInstance().getWorld(worldID);
    if (pWorld != null) {
      sendPacket(LoginPacket.onSendWorldStatus(pWorld.getPopulationStatus()));
    } else {
      // Send full world packet
      sendPacket(LoginPacket.onSendWorldStatus(2));
    }
  }

  public void onCreateNewCharacter(InPacket packet) {
    // [04 00 45 72 69 63] [21 4E 00 00] [4B 75 00 00] [8A DE 0F 00] [A2 2C 10 00] [85 5B 10 00] [F0 DD 13 00] [07 00 00 00] [06 00 00 00] [06 00 00 00] [06 00 00 00]
    if (this.loginState != 8) {
      postClose();
      return;
    }
    List<Integer> stat = new ArrayList<>(4);
    boolean ret = true;

    String charName = packet.decodeString();
    int face = packet.decodeInt();
    int hairStyle = packet.decodeInt();
    int clothes = packet.decodeInt();
    int pants = packet.decodeInt();
    int shoes = packet.decodeInt();
    int weapon = packet.decodeInt();

    // Dice Roll information
    int totStat = 0;
    for (int i = 0; i < 4; i++) {
      int val = packet.decodeInt();
      if (val < 4) {
        ret = false;
      } else {
        totStat += val;
      }
      stat.add(val);
    }
    if (totStat > 25) {
      ret = false;
    }

    // Check Character Name
    if (!LoginApp.getInstance().checkCharName(charName, true)) {
      ret = false;
    }

    // Check Character Equips
    if (!LoginApp.getInstance().checkCharEquip(this.gender, NewCharEquipType.Face, face)
        || !LoginApp.getInstance()
        .checkCharEquip(this.gender, NewCharEquipType.HairStyle, hairStyle)
        || !LoginApp.getInstance().checkCharEquip(this.gender, NewCharEquipType.Clothes, clothes)
        || !LoginApp.getInstance().checkCharEquip(this.gender, NewCharEquipType.Pants, pants)
        || !LoginApp.getInstance().checkCharEquip(this.gender, NewCharEquipType.Shoes, shoes)
        || !LoginApp.getInstance().checkCharEquip(this.gender, NewCharEquipType.Weapon, weapon)) {
      ret = false;
    }

    WorldEntry world = LoginApp.getInstance().getWorld(this.worldID);
    if (ret && world != null && world.getSocket() != null) {
      final int level = 1;
      final int job = JobAccessor.Novice.getJob();
      final int map = 0;
      final Pointer<Integer> newCharacterID = new Pointer<>(0);

      CharacterData character = null;
      int result = LoginDB.rawCreateNewCharacter(this.accountID, this.worldID, charName,
          this.gender, face, 0, hairStyle, level, job, clothes, pants, shoes, weapon, stat, map,
          newCharacterID);
      if (result == 1) {
        character = LoginDB.rawLoadCharacter(newCharacterID.get());
        result = 0;
        // Best way to always have the login's SN up-to-date.
        LoginApp.getInstance().updateItemInitSN();
      }

      sendPacket(LoginPacket.onCreateNewCharacterResult(result, character), false);
    }

    stat.clear();
  }

  public void onDeleteCharacter(InPacket packet) {
    if (this.loginState != 8) {
      postClose();
      return;
    }
    int characterId = packet.decodeInt();
    boolean ret = true;//does only the client validate this? o.O

    if (ret) {
      int result = LoginDB.rawDeleteCharacter(characterId);

      sendPacket(LoginPacket.onDeleteCharacterResult(characterId, result), false);
    }
  }

  public void onExceptionLog(InPacket packet) {
    String log = packet.decodeString();
    int time = packet.decodeInt();

    Logger.logError("[EXCEPTION : %d] %s", time, log);
  }

  public void onSelectCharacter(InPacket packet) {
    if (this.loginState == 8) {
      this.characterID = packet.decodeInt();
      String macs = packet.decodeString(); // TODO

      if (!this.characters.containsKey(this.characterID)) {
        closeSocket();
        return;
      }

      WorldEntry world = LoginApp.getInstance().getWorld(this.worldID);
      if (world != null) {
        ChannelEntry ch = world.getChannel(this.channelID);
        if (ch != null) {
          this.loginState = 9;

          sendPacket(LoginPacket.onSelectCharacterResult(0, Utilities.netIPToByteArray(ch.getAddr()),
              ch.getPort(), this.characterID), false);
        }
      }
    } else {
      postClose();
    }
  }

  public void onSelectWorld(InPacket packet) {
    this.worldID = packet.decodeByte();
    this.channelID = packet.decodeByte();

    WorldEntry pWorld = LoginApp.getInstance().getWorld(this.worldID);
    if (pWorld != null) {
      if (pWorld.getChannel(this.channelID) == null) {
        Logger.logError("User %s attempting to connect to offline channel (%d) for world (%d)", this.nexonClubID,
            this.channelID, this.worldID);
        return;
      }

      List<Integer> characterId = new ArrayList<>();
      int count = LoginDB.rawGetWorldCharList(this.accountID, this.worldID, characterId);

      List<CharacterData> avatars = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        avatars.add(LoginDB.rawLoadCharacter(characterId.get(i)));
        this.characters.put(characterId.get(i), avatars.get(i).getCharacterStat().getName());
      }

      this.loginState = 8;
      sendPacket(LoginPacket.onSelectWorldResult(0, this.ssn, avatars), false);
    } else {
      Logger.logError("User %s attempting to connect to offline world %d", this.nexonClubID,
          this.worldID);
    }
  }

  public void onUpdate(long time) {

  }

  public boolean postClose() {
    if (!this.closePosted) {
      this.closePosted = true;
      return false;
    } else {
      onClose();
      return true;
    }
  }

  private void processPacket(InPacket packet) {
    if (packet.available() < 1) {
      return;
    }
    final short type = packet.decodeShort();
    if (OrionConfig.LOG_PACKETS) {
      Logger.logReport("[Packet Logger] [0x%s]: %s", Integer.toHexString(type).toUpperCase(),
          packet.toString());
    }
    if (type == ClientPacket.AliveAck) {
      this.aliveReqSent = 0;
      this.lastAliveAck = packet.decodeInt();
    } else if (type == ClientPacket.AliveReq) {
      this.aliveReqSent = System.currentTimeMillis() / 1000;
      sendPacket(onAliveReq((int) this.aliveReqSent), false);
    } else if (type >= ClientPacket.BEGIN_SOCKET && type <= ClientPacket.END_SOCKET) {
      switch (type) {
        case ClientPacket.CheckPassword:
          onCheckPassword(packet);
          break;
        case ClientPacket.PinAction:
          onPinAction(packet);
          break;
        case ClientPacket.RegisterPin:
          onRegisterPin(packet);
          break;
        case ClientPacket.BackToWorldSelect:
          onBackToWorldSelect(packet);
        case ClientPacket.RequestWorldInfo:
          onRequestWorldInfo(packet);
          break;
        case ClientPacket.HoverWorld:
          onHoverWorld(packet);
          break;
        case ClientPacket.SelectWorld:
          onSelectWorld(packet);
          break;
        case ClientPacket.CheckDuplicatedID:
          onCheckDuplicateID(packet);
          break;
        case ClientPacket.CreateNewCharacter:
          onCreateNewCharacter(packet);
          break;
        case ClientPacket.DeleteCharacter:
          onDeleteCharacter(packet);
          break;
        case ClientPacket.SelectCharacter:
          onSelectCharacter(packet);
          break;
        case ClientPacket.ExceptionLog:
          onExceptionLog(packet);
          break;
        default: {
          Logger.logReport(
              "[Unidentified Packet] [0x" + Integer.toHexString(type).toUpperCase() + "]: "
                  + packet.toString());
        }
      }
    }
  }

  public void ResetLoginState(int loginState) {
    this.nexonClubID = "";
    this.worldID = -1;
    this.channelID = -1;
    this.characterID = 0;
    this.processed = false;
    this.ipCheckRequestSent = 0;
    this.tempBlockedIP = false;
    if (this.loginState == loginState) {
      ++this.failCount;
      if (this.failCount > 5) {
        onClose();
      }
    } else {
      this.failCount = 0;
    }
    this.loginState = loginState;
  }

  public void sendPacket(Packet packet, boolean force) {
    this.lockSend.lock();
    try {
      if (!this.closePosted || force) {
        sendPacket(packet);
      }
    } finally {
      this.lockSend.unlock();
    }
  }

  public void sendPacket(Packet packet) {
    this.lockSend.lock();
    try {
      if (this.channel == null || packet == null) {
        throw new RuntimeException("fuck everything");
      }

      this.channel.writeAndFlush(packet);
    } finally {
      this.lockSend.unlock();
    }
  }

  public void setAccountID(int accountID) {
    this.accountID = accountID;
  }

  public void setGender(byte gender) {
    this.gender = gender;
  }

  public void setGradeCode(byte grade) {
    this.gradeCode = grade;
  }

  public void setLocalSocketSN(int sn) {
    this.localSocketSN = sn;
  }

  public void setNexonClubID(String id) {
    this.nexonClubID = id;
  }

  public void setSSN(int ssn) {
    this.ssn = ssn;
  }

  /**
   * Migrates a remote client to a different game server.
   *
   * @param isGuestAccount If the requested account is a guest, crash them for hacking. (guests
   *                       can't cc!)
   * @param addr           The InetAddress of the requested channel server.
   * @param port           The port the game server is on.
   * @return The server migration packet.
   */
  public static OutPacket onMigrateCommand(boolean isGuestAccount, InetAddress addr, int port) {
    OutPacket packet = new ByteBufOutPacket(LoopbackPacket.MigrateCommand);
    packet.encodeBool(isGuestAccount);
    packet.encodeBytes(addr.getAddress());
    packet.encodeShort(port);
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
