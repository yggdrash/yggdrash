package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BlockHusk;

public interface CatchUpSyncEventListener {
    void catchUpRequest(BlockHusk block);
}
