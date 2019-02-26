package io.yggdrash.core.p2p;

import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;

import java.util.List;
import java.util.concurrent.Future;

public interface PeerHandler {
    List<Peer> findPeers(BranchId branchId, Peer targetPeer);

    String ping(BranchId branchId, Peer owner, String message);

    Peer getPeer();

    void stop();

    //List<BlockHusk> simpleSyncBlock(BranchId branchId, long offset);

    //List<TransactionHusk> simpleSyncTransaction(BranchId branchId);

    //void simpleBroadcastBlock(BlockHusk blockHusk);

    //void simpleBroadcastTransaction(TransactionHusk txHusk);

    Future<List<BlockHusk>> syncBlock(BranchId branchId, long offset);

    Future<List<TransactionHusk>> syncTx(BranchId branchId);

    void broadcastBlock(BlockHusk blockHusk);

    void broadcastTx(TransactionHusk txHusk);

    void biDirectTest(int seq, String msg);
}
