package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.proto.Proto;

import java.util.List;

public interface PeerHandler {
    List<String> findPeers(Peer peer);

    Peer getPeer();

    void stop();

    String ping(String message, Peer peer);

    List<BlockHusk> syncBlock(BranchId branchId, long offset);

    List<TransactionHusk> syncTransaction(BranchId branchId);

    void broadcastTransaction(Proto.Transaction[] txs);

    void broadcastBlock(Proto.Block[] blocks);
}
