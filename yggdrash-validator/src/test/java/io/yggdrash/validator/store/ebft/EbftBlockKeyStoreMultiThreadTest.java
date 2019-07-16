package io.yggdrash.validator.store.ebft;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import com.google.protobuf.ByteString;
import io.yggdrash.StoreTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.TestUtils;
import io.yggdrash.validator.data.ebft.EbftBlock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(ConcurrentTestRunner.class)
public class EbftBlockKeyStoreMultiThreadTest {
    private static final Logger log = LoggerFactory.getLogger(EbftBlockKeyStoreMultiThreadTest.class);

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

        String password = "Aa1234567890!";
        wallet0 = new Wallet(null, "tmp/",
                "test0" + TimeUtils.time(), password);
        wallet1 = new Wallet(null, "tmp/",
                "test1" + TimeUtils.time(), password);
        wallet2 = new Wallet(null, "tmp/",
                "test2" + TimeUtils.time(), password);
        wallet3 = new Wallet(null, "tmp/",
                "test3" + TimeUtils.time(), password);

        this.ds =
                new LevelDbDataSource(StoreTestUtils.getTestPath(), "ebftBlockKeyStoreTest");
        this.blockKeyStore = new EbftBlockKeyStore(ds);

        this.ebftBlock = makeEbftBlock(0L, Constants.EMPTY_HASH);

        this.blockKeyStore.put(this.ebftBlock.getIndex(), this.ebftBlock.getHash().getBytes());
    }

    private Block makeBlock(long index, byte[] prevHash) {
        return new TestUtils(wallet0).sampleBlock(index, prevHash);
    }

    private List<ByteString> makeConsensusList(Block block) {
        List<ByteString> consensusList = new ArrayList<>();
        consensusList.add(wallet0.signByteString(block.getHash().getBytes(), true));
        consensusList.add(wallet1.signByteString(block.getHash().getBytes(), true));
        consensusList.add(wallet2.signByteString(block.getHash().getBytes(), true));
        consensusList.add(wallet3.signByteString(block.getHash().getBytes(), true));
        return consensusList;
    }

    private EbftBlock makeEbftBlock(long index, byte[] prevHash) {
        Block block = makeBlock(index, prevHash);
        return new EbftBlock(block, makeConsensusList(block));
    }

    @Test
    @ThreadCount(8)
    public void putTestMultiThread() {
        long testNumber = 10000;
        for (long l = 0L; l < testNumber; l++) {
            this.blockKeyStore.put(l, Constants.EMPTY_HASH);
        }
        log.debug("blockKeyStore size= " + this.blockKeyStore.size());
        assertEquals(testNumber, this.blockKeyStore.size());
    }

    @Test
    @ThreadCount(8)
    public void putMutiThreadMemoryTest() throws InterruptedException {
        TestConstants.PerformanceTest.apply();

        System.gc();
        Thread.sleep(20000);

        this.putTestMultiThread();

        System.gc();
        Thread.sleep(3000000);
    }


    @Test
    @ThreadCount(8)
    public void getMutiThreadMemoryTest() throws InterruptedException {
        TestConstants.PerformanceTest.apply();

        System.gc();
        Thread.sleep(20000);

        this.putTestMultiThread();

        System.gc();
        Thread.sleep(10000);

        for (long l = 0L; l < this.blockKeyStore.size(); l++) {
            log.debug("{} {}", l, Hex.toHexString(this.blockKeyStore.get(l)));
        }

        log.debug("blockKeyStore size= " + this.blockKeyStore.size());

        System.gc();
        Thread.sleep(3000000);
    }

    @Test
    @ThreadCount(8)
    public void containsMutiThreadMemoryTest() throws InterruptedException {
        TestConstants.PerformanceTest.apply();

        System.gc();
        Thread.sleep(20000);

        this.putTestMultiThread();

        System.gc();
        Thread.sleep(10000);

        for (long l = 0L; l < this.blockKeyStore.size(); l++) {
            if (this.blockKeyStore.contains(l)) {
                log.debug("{} {}", l, Hex.toHexString(this.blockKeyStore.get(l)));
            }
        }

        log.debug("blockKeyStore size= " + this.blockKeyStore.size());

        System.gc();
        Thread.sleep(3000000);
    }

    @After
    public void tearDown() {
        StoreTestUtils.clearTestDb();
    }

}
