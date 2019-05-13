package io.yggdrash.core.consensus;

import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.store.BlockKeyStore;

import java.util.Map;

public interface ConsensusBlockChain<T, V> {
    BranchId getBranchId();

    Consensus getConsensus();

    BlockKeyStore getBlockKeyStore();

    ConsensusBlock<T> getGenesisBlock();

    Map<String, V> getUnConfirmedData();

    ConsensusBlock<T> addBlock(ConsensusBlock<T> block);

    boolean isValidator(String addr);

    BlockChainManager<T> getBlockChainManager();
}
