package io.yggdrash.validator.store;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.BlockCon;
import io.yggdrash.validator.util.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;

public class BlockConStoreBuilderTest {

    private BlockConStoreBuilder builder;
    private Wallet wallet;
    Block genesisBlock;
    BlockCon genesisBlockCon;

    private BranchId branchId;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        builder = new BlockConStoreBuilder(new DefaultConfig());
        wallet = new Wallet();
        genesisBlock = new TestUtils(wallet).sampleBlock();
        genesisBlockCon = new BlockCon(genesisBlock.getHeader().getIndex(),
                genesisBlock.getHeader().getPrevBlockHash(), genesisBlock);
        branchId = new BranchId(genesisBlock.getHash());
    }

    @Test
    public void buildBlockConStore() {
        BlockConStore store = builder.buildBlockConStore(branchId);
        store.put(genesisBlockCon.getId(), genesisBlockCon);
        assert store.contains(genesisBlockCon.getId());
        assert store.get(genesisBlockCon.getId()).equals(genesisBlockCon);
    }

}