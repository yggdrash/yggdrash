package io.yggdrash.validator.store.ebft;

import io.yggdrash.StoreTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.util.TestUtils;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.yggdrash.common.config.Constants.EMPTY_BYTE1K;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EbftBlockKeyStoreTest {
    private static final Logger log = LoggerFactory.getLogger(EbftBlockKeyStoreTest.class);

    private LevelDbDataSource ds;
    private EbftBlockKeyStore blockKeyStore;

    private Wallet wallet0;
    private Wallet wallet1;
    private Wallet wallet2;
    private Wallet wallet3;

    private EbftBlock ebftBlock;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        StoreTestUtils.clearTestDb();

        wallet0 = new Wallet(null, "tmp/",
                "test0" + TimeUtils.time(), "Password1234!");
        wallet1 = new Wallet(null, "tmp/",
                "test1" + TimeUtils.time(), "Password1234!");
        wallet2 = new Wallet(null, "tmp/",
                "test2" + TimeUtils.time(), "Password1234!");
        wallet3 = new Wallet(null, "tmp/",
                "test3" + TimeUtils.time(), "Password1234!");

        this.ds =
                new LevelDbDataSource(StoreTestUtils.getTestPath(), "ebftBlockKeyStoreTest");
        this.blockKeyStore = new EbftBlockKeyStore(ds);

        this.ebftBlock = makeEbftBlock(0L, Constants.EMPTY_HASH);

        this.blockKeyStore.put(this.ebftBlock.getIndex(), this.ebftBlock.getHash().getBytes());
    }

    private Block makeBlock(long index, byte[] prevHash) {
        return new TestUtils(wallet0).sampleBlock(index, prevHash);
    }

    private List<String> makeConsensusList(Block block) {
        List<String> consensusList = new ArrayList<>();
        consensusList.add(wallet0.signHex(block.getHash(), true));
        consensusList.add(wallet1.signHex(block.getHash(), true));
        consensusList.add(wallet2.signHex(block.getHash(), true));
        consensusList.add(wallet3.signHex(block.getHash(), true));
        return consensusList;
    }

    private EbftBlock makeEbftBlock(long index, byte[] prevHash) {
        Block block = makeBlock(index, prevHash);
        return new EbftBlock(block, makeConsensusList(block));
    }

    @Test
    public void putGetTest() {
        byte[] newHash = blockKeyStore.get(this.ebftBlock.getIndex());
        assertArrayEquals(this.ebftBlock.getHash().getBytes(), newHash);
        assertTrue(blockKeyStore.contains(this.ebftBlock.getIndex()));
        assertFalse(blockKeyStore.contains(this.ebftBlock.getIndex() + 1));
        assertFalse(blockKeyStore.contains(-1L));
        assertEquals(1, blockKeyStore.size());
    }

    @Test
    public void putTest_NegativeNumber() {
        long beforeSize = blockKeyStore.size();
        blockKeyStore.put(-1L, this.ebftBlock.getHash().getBytes());
        assertEquals(blockKeyStore.size(), beforeSize);
    }

    @Test
    public void getTest_NegativeNumber() {
        assertNull(blockKeyStore.get(-1L));
    }

    @Test
    public void memoryTest() throws InterruptedException {
        TestConstants.PerformanceTest.apply();

        System.gc();
        Thread.sleep(20000);

        long testNumber = 1000000;
        byte[] result;
        List<byte[]> resultList = new ArrayList<>();
        for (long l = 0L; l < testNumber; l++) {
            this.blockKeyStore.put(l, EMPTY_BYTE1K);
            result = this.blockKeyStore.get(l);
            resultList.add(result);
        }
        resultList.clear();
        log.debug("blockKeyStore size: " + this.blockKeyStore.size());
        assertEquals(this.blockKeyStore.size(), testNumber);

        System.gc();
        Thread.sleep(20000);
    }

    @Test
    public void closeTest() {
        blockKeyStore.close();
        TestCase.assertNull(blockKeyStore.get(this.ebftBlock.getIndex()));

        this.blockKeyStore = new EbftBlockKeyStore(ds);
        byte[] newHash = blockKeyStore.get(0L);
        assertArrayEquals(this.ebftBlock.getHash().getBytes(), newHash);
    }

    @After
    public void tearDown() {
        StoreTestUtils.clearTestDb();
    }

}
