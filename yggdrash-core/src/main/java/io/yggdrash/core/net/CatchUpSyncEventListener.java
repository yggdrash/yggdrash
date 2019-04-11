package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.consensus.Block;
import io.yggdrash.core.p2p.Peer;

public interface CatchUpSyncEventListener {

    void catchUpRequest(Block block);

    void catchUpRequest(BranchId branchId, long offset);

    void catchUpRequest(BranchId branchId, Peer from);
}
