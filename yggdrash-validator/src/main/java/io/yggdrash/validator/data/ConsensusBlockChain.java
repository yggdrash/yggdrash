package io.yggdrash.validator.data;

import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.validator.config.Consensus;
import io.yggdrash.validator.store.BlockKeyStore;
import io.yggdrash.validator.store.BlockStore;

import java.util.Map;

public interface ConsensusBlockChain<K, V> {
    byte[] getChain();

    Consensus getConsensus();

    BlockKeyStore getBlockKeyStore();

    BlockStore getBlockStore();

    TransactionStore getTransactionStore();

    ConsensusBlock getGenesisBlock();

    ConsensusBlock getLastConfirmedBlock();

    Map<K, V> getUnConfirmedData();

    void addBlock(ConsensusBlock block);
}
