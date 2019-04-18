package io.yggdrash.core.p2p;

import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.consensus.Block;

import java.util.List;
import java.util.concurrent.Future;

public interface PeerHandler {
    List<Peer> findPeers(BranchId branchId, Peer targetPeer);

    String ping(BranchId branchId, Peer owner, String message);

    Peer getPeer();

    void stop();

    Future<List<Block>> syncBlock(BranchId branchId, long offset);

    Future<List<TransactionHusk>> syncTx(BranchId branchId);

    void broadcastBlock(Block block);

    void broadcastTx(TransactionHusk txHusk);
}
