package io.yggdrash.validator.store.pbft;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import io.yggdrash.StoreTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.pbft.PbftBlock;
import io.yggdrash.validator.data.pbft.PbftMessage;
import io.yggdrash.validator.data.pbft.PbftMessageSet;
import io.yggdrash.validator.util.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import static io.yggdrash.common.config.Constants.EMPTY_BYTE32;
import static io.yggdrash.common.config.Constants.PBFT_COMMIT;
import static io.yggdrash.common.config.Constants.PBFT_PREPARE;
import static io.yggdrash.common.config.Constants.PBFT_PREPREPARE;
import static org.junit.Assert.assertEquals;

@RunWith(ConcurrentTestRunner.class)
public class PbftBlockKeyStoreMultiThreadTest {

    private static final Logger log = LoggerFactory.getLogger(PbftBlockKeyStoreMultiThreadTest.class);

    private Wallet wallet0;
    private Wallet wallet1;
    private Wallet wallet2;
    private Wallet wallet3;

    private PbftBlock pbftBlock0;
    private PbftBlock pbftBlock1;
    private PbftBlock pbftBlock2;
    private PbftBlock pbftBlock3;

    private LevelDbDataSource ds;
    private PbftBlockKeyStore blockKeyStore;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        wallet0 = new Wallet();
        wallet1 = new Wallet(null, "/tmp/",
                "test2" + TimeUtils.time(), "Password1234!");
        wallet2 = new Wallet(null, "/tmp/",
                "test3" + TimeUtils.time(), "Password1234!");
        wallet3 = new Wallet(null, "/tmp/",
                "test4" + TimeUtils.time(), "Password1234!");

        this.pbftBlock0 = makePbftBlock(0L, EMPTY_BYTE32);
        this.pbftBlock1 = makePbftBlock(pbftBlock0.getIndex() + 1, pbftBlock0.getHash());
        this.pbftBlock2 = makePbftBlock(pbftBlock1.getIndex() + 1, pbftBlock1.getHash());
        this.pbftBlock3 = makePbftBlock(pbftBlock2.getIndex() + 1, pbftBlock2.getHash());

        StoreTestUtils.clearTestDb();

        this.ds = new LevelDbDataSource(StoreTestUtils.getTestPath(), "pbftBlockKeyStoreTest");
        this.blockKeyStore = new PbftBlockKeyStore(ds);
        this.blockKeyStore.put(this.pbftBlock0.getIndex(), this.pbftBlock0.getHash());
    }

    private Block makeBlock(long index, byte[] prevHash) {
        return new TestUtils(wallet0).sampleBlock(index, prevHash);
    }

    private PbftBlock makePbftBlock(long index, byte[] prevHash) {
        Block block = makeBlock(index, prevHash);
        return new PbftBlock(block, makePbftMessageSet(block));
    }


    private PbftMessage makePbftMessage(String type, Block block, Wallet wallet) {
        switch (type) {
            case PBFT_PREPREPARE:
                return new PbftMessage(type, block.getIndex(), block.getIndex(), block.getHash(), null, wallet, block);
            default:
                return new PbftMessage(type, block.getIndex(), block.getIndex(), block.getHash(), null, wallet, null);
        }
    }

    private PbftMessageSet makePbftMessageSet(Block block) {
        Map<String, PbftMessage> prepareMap = new TreeMap<>();
        PbftMessage prepare0 = makePbftMessage(PBFT_PREPARE, block, wallet0);
        prepareMap.put(prepare0.getSignatureHex(), prepare0);
        PbftMessage prepare1 = makePbftMessage(PBFT_PREPARE, block, wallet1);
        prepareMap.put(prepare1.getSignatureHex(), prepare1);
        PbftMessage prepare2 = makePbftMessage(PBFT_PREPARE, block, wallet2);
        prepareMap.put(prepare2.getSignatureHex(), prepare2);
        PbftMessage prepare3 = makePbftMessage(PBFT_PREPARE, block, wallet3);
        prepareMap.put(prepare3.getSignatureHex(), prepare3);

        Map<String, PbftMessage> commitMap = new TreeMap<>();
        PbftMessage commit0 = makePbftMessage(PBFT_COMMIT, block, wallet0);
        commitMap.put(commit0.getSignatureHex(), commit0);
        PbftMessage commit1 = makePbftMessage(PBFT_COMMIT, block, wallet1);
        commitMap.put(commit1.getSignatureHex(), commit1);
        PbftMessage commit2 = makePbftMessage(PBFT_COMMIT, block, wallet2);
        commitMap.put(commit2.getSignatureHex(), commit2);
        PbftMessage commit3 = makePbftMessage(PBFT_COMMIT, block, wallet3);
        commitMap.put(commit3.getSignatureHex(), commit3);

        PbftMessage prePrepare = makePbftMessage(PBFT_PREPREPARE, block, wallet0);
        return new PbftMessageSet(prePrepare, prepareMap, commitMap, null);
    }

    @Test
    @ThreadCount(4)
    public void putTestMultiThread() {
        long testNumber = 1000;
        for (long l = 0L; l < testNumber; l++) {
            this.blockKeyStore.put(l, EMPTY_BYTE32);
        }
        log.debug("blockKeyStore size= " + this.blockKeyStore.size());
        assertEquals(testNumber, this.blockKeyStore.size());
    }

    @Test
    @ThreadCount(4)
    public void putMutiThreadMemoryTest() throws InterruptedException {
        TestConstants.PerformanceTest.apply();
        System.gc();

        this.putTestMultiThread();

        System.gc();
        Thread.sleep(30000);
    }

    @Test
    @ThreadCount(4)
    public void getMutiThreadMemoryTest() throws InterruptedException {
        TestConstants.PerformanceTest.apply();
        System.gc();

        this.putTestMultiThread();

        System.gc();
        Thread.sleep(10000);

        for (long l = 0L; l < this.blockKeyStore.size(); l++) {
            log.debug("{} {}", l, Hex.toHexString(this.blockKeyStore.get(l)));
        }

        log.debug("blockKeyStore size= " + this.blockKeyStore.size());

        System.gc();
        Thread.sleep(30000);
    }


    @Test
    @ThreadCount(4)
    public void containsMutiThreadMemoryTest() throws InterruptedException {
        TestConstants.PerformanceTest.apply();
        System.gc();

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
        Thread.sleep(30000);
    }

    @After
    public void tearDown() {
        StoreTestUtils.clearTestDb();
    }

}
