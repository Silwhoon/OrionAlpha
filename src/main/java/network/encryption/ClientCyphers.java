package network.encryption;

import common.OrionConfig;
import network.packet.InitializationVector;

public class ClientCyphers {

  private final MapleAESOFB send;
  private final MapleAESOFB receive;

  private ClientCyphers(MapleAESOFB send, MapleAESOFB receive) {
    this.send = send;
    this.receive = receive;
  }

  public static ClientCyphers of(InitializationVector sendIv, InitializationVector receiveIv) {
    MapleAESOFB send = new MapleAESOFB(sendIv, (short) (0xFFFF - OrionConfig.CLIENT_VER));
    MapleAESOFB receive = new MapleAESOFB(receiveIv, (short) OrionConfig.CLIENT_VER);
    return new ClientCyphers(send, receive);
  }

  public MapleAESOFB getSendCypher() {
    return send;
  }

  public MapleAESOFB getReceiveCypher() {
    return receive;
  }
}