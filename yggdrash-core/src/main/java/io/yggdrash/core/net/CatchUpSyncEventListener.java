package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.p2p.Peer;

public interface CatchUpSyncEventListener {

    void catchUpRequest(BlockHusk block);

    void catchUpRequest(BranchId branchId, long offset);

    void catchUpRequest(BranchId branchId, Peer from);
}
