package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.proto.Proto;

import java.util.List;

public interface PeerClientChannel {
    List<Proto.PeerInfo> findPeers(Peer peer);

    Peer getPeer();

    void stop();

    String ping(String message, Peer peer);

    List<Proto.Block> syncBlock(BranchId branchId, long offset);

    List<Proto.Transaction> syncTransaction(BranchId branchId);

    void broadcastTransaction(Proto.Transaction[] txs);

    void broadcastBlock(Proto.Block[] blocks);
}
