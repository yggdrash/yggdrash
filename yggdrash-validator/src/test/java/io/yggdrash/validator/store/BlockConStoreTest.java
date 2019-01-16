package io.yggdrash.validator.store;

import io.yggdrash.StoreTestUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.BlockCon;
import io.yggdrash.validator.util.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BlockConStoreTest {
    private static final Logger log = LoggerFactory.getLogger(BlockConStoreTest.class);

    private Wallet wallet;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        wallet = new Wallet();
    }

    @Test
    public void blockConStoreTest() {
        LevelDbDataSource ds =
                new LevelDbDataSource(StoreTestUtils.getTestPath(), "block-con-store-test");
        BlockConStore blockConStore = new BlockConStore(ds);
        Block block = new TestUtils(wallet).sampleBlock();
        List<String> consensusList = new ArrayList<>();
        consensusList.add(Hex.toHexString(wallet.sign(block.getHash())));

        BlockCon blockCon = new BlockCon(block.getHeader().getIndex(),
                block.getHeader().getPrevBlockHash(), block, consensusList);

        blockConStore.put(blockCon.getHash(), blockCon);
        BlockCon foundBlockCon = blockConStore.get(blockCon.getHash());

        assert (blockCon.equals(foundBlockCon));
        assert (blockConStore.contains(blockCon.getHash()));

        log.debug("size: " + blockConStore.size());
        assertEquals(blockConStore.size(), 1);

        StoreTestUtils.clearTestDb();
    }

}
