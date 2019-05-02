package io.yggdrash.core.consensus;

import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.store.BlockKeyStore;
import io.yggdrash.core.store.ConsensusBlockStore;
import io.yggdrash.core.store.TransactionStore;

import java.util.Map;

public interface ConsensusBlockChain<T, V> {
    BranchId getBranchId();

    Consensus getConsensus();

    BlockKeyStore getBlockKeyStore();

    ConsensusBlockStore<T> getBlockStore();

    TransactionStore getTransactionStore();

    ConsensusBlock<T> getGenesisBlock();

    ConsensusBlock<T> getLastConfirmedBlock();

    Map<String, V> getUnConfirmedData();

    ConsensusBlock<T> addBlock(ConsensusBlock<T> block);

    boolean isValidator(String addr);
}
