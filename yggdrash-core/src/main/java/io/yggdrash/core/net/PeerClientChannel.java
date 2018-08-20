package io.yggdrash.core.net;

import io.yggdrash.proto.Pong;
import io.yggdrash.proto.Proto;

import java.util.List;

public interface PeerClientChannel {

    Peer getPeer();

    void stop();

    void stop(String ynodeUri);

    Pong ping(String message);

    List<Proto.Block> syncBlock(long offset);

    List<Proto.Transaction> syncTransaction();

    void broadcastTransaction(Proto.Transaction[] txs);

    void broadcastBlock(Proto.Block[] blocks);

    List<String> requestPeerList(String ynodeUri, int limit);

    void disconnectPeer(String ynodeUri);
}
