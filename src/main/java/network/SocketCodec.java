package network;

import io.netty.channel.CombinedChannelDuplexHandler;
import network.encryption.ClientCyphers;

public class SocketCodec extends CombinedChannelDuplexHandler<SocketDecoder, SocketEncoder> {

  public SocketCodec(ClientCyphers clientCyphers) {
    super(new SocketDecoder(clientCyphers.getReceiveCypher()), new SocketEncoder(clientCyphers.getSendCypher()));
  }
}
