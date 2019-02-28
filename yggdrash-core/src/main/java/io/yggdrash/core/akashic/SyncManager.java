package io.yggdrash.core.akashic;

import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.p2p.PeerHandler;

public interface SyncManager {

    void syncBlock(PeerHandler peerHandler, BlockChain blockChain, long limitIndex);

    void syncTransaction(PeerHandler peerHandler, BlockChain blockChain);
}
