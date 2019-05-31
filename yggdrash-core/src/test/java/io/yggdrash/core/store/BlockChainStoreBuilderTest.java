package io.yggdrash.core.store;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainStoreBuilderTest {

    private BlockChainStoreBuilder builder;
    private BlockChainStore bcStore;

    @Before
    public void setUp() {
        DefaultConfig config = new DefaultConfig();
        builder = BlockChainStoreBuilder.newBuilder(BranchId.NULL)
                .withDataBasePath(config.getDatabasePath())
                .withProductionMode(config.isProductionMode())
        .setConsensusAlgorithm(null)
        .setBlockStoreFactory(PbftBlockStoreMock::new);
        this.bcStore = builder.build();
    }

    @Test
    public void shouldBeBuiltMetaStore() {
        ConsensusBlock block = BlockChainTestUtils.genesisBlock();
        BranchStore store = bcStore.getBranchStore();
        store.setBestBlock(block);

        assertThat(store.getBestBlockHash()).isEqualTo(block.getHash());
    }

    @Test
    public void buildBlockStore() {
        ConsensusBlock block = BlockChainTestUtils.genesisBlock();
        ConsensusBlockStore store = builder.setBlockStoreFactory(PbftBlockStoreMock::new).buildBlockStore();
        store.put(block.getHash(), block);
        assertThat(store.contains(block.getHash())).isTrue();
        assertThat(store.get(block.getHash())).isEqualTo(block);
    }

    @Test
    public void buildTxStore() {
        Transaction tx = BlockChainTestUtils.createTransferTx();
        TransactionStore store = bcStore.getTransactionStore();
        store.put(tx.getHash(), tx);
        assertThat(store.contains(tx.getHash())).isTrue();
        assertThat(store.get(tx.getHash())).isEqualTo(tx);
    }
}