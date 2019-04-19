package io.yggdrash.core.consensus;

import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.store.BlockKeyStore;
import io.yggdrash.core.store.ConsensusBlockStore;
import io.yggdrash.core.store.TransactionStore;

import java.util.List;
import java.util.Map;

public interface ConsensusBlockChain<T, V> {
    BranchId getBranchId();

    Consensus getConsensus();

    BlockKeyStore getBlockKeyStore();

    ConsensusBlockStore<T> getBlockStore();

    TransactionStore getTransactionStore();

    Block<T> getGenesisBlock();

    Block<T> getLastConfirmedBlock();

    Map<String, V> getUnConfirmedData();

    Block<T> addBlock(Block<T> block);

    List<Block<T>> getBlockList(long index, long count);

}
