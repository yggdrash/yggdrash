package io.yggdrash.validator.store;

import io.yggdrash.StoreTestUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.util.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BlockConKeyStoreTest {
    private static final Logger log = LoggerFactory.getLogger(BlockConKeyStoreTest.class);

    private Wallet wallet;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        wallet = new Wallet();
    }

    @Test
    public void BlockConKeyStoreTest() {
        LevelDbDataSource ds =
                new LevelDbDataSource(StoreTestUtils.getTestPath(), "block-con-key-store-test");
        BlockConKeyStore blockConKeyStore = new BlockConKeyStore(ds);

        Block block = new TestUtils(wallet).sampleBlock();
        blockConKeyStore.put(block.getHeader().getIndex(), block.getHash());
        byte[] foundKey = blockConKeyStore.get(block.getHeader().getIndex());

        assertArrayEquals(block.getHash(), foundKey);
        assert (blockConKeyStore.contains(block.getHeader().getIndex()));

        log.debug("size: " + blockConKeyStore.size());
        assertEquals(blockConKeyStore.size(), 1);

        StoreTestUtils.clearTestDb();
    }

}
