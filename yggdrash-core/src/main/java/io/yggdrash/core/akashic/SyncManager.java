package io.yggdrash.core.akashic;

import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.net.PeerHandler;

public interface SyncManager {

    void syncBlock(PeerHandler peerHandler, BlockChain blockChain);

    void syncTransaction(PeerHandler peerHandler, BlockChain blockChain);
}
