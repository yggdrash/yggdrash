package io.yggdrash.validator.store;

import io.yggdrash.StoreTestUtils;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.store.ebft.EbftBlockKeyStore;
import io.yggdrash.validator.util.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class EbftBlockKeyStoreTest {
    private static final Logger log = LoggerFactory.getLogger(EbftBlockKeyStoreTest.class);

    private Wallet wallet;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        wallet = new Wallet();
    }

    @Test
    public void ebftBlockKeyStoreTest() {
        LevelDbDataSource ds =
                new LevelDbDataSource(StoreTestUtils.getTestPath(), "block-con-key-store-test");
        EbftBlockKeyStore ebftBlockKeyStore = new EbftBlockKeyStore(ds);

        Block block = new TestUtils(wallet).sampleBlock();
        ebftBlockKeyStore.put(block.getHeader().getIndex(), block.getHash());
        byte[] foundKey = ebftBlockKeyStore.get(block.getHeader().getIndex());

        assertArrayEquals(block.getHash(), foundKey);
        assert (ebftBlockKeyStore.contains(block.getHeader().getIndex()));

        log.debug("size: " + ebftBlockKeyStore.size());
        assertEquals(ebftBlockKeyStore.size(), 1);

        StoreTestUtils.clearTestDb();
    }

}
