package io.yggdrash.validator.store;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import io.yggdrash.StoreTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.pbft.PbftBlock;
import io.yggdrash.validator.data.pbft.PbftMessage;
import io.yggdrash.validator.data.pbft.PbftMessageSet;
import io.yggdrash.validator.store.pbft.PbftBlockKeyStore;
import io.yggdrash.validator.store.pbft.PbftBlockStore;
import io.yggdrash.validator.util.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import static io.yggdrash.common.config.Constants.PBFT_COMMIT;
import static io.yggdrash.common.config.Constants.PBFT_PREPARE;
import static io.yggdrash.common.config.Constants.PBFT_PREPREPARE;
import static io.yggdrash.common.config.Constants.PBFT_VIEWCHANGE;
import static org.junit.Assert.assertEquals;

@RunWith(ConcurrentTestRunner.class)
public class PbftBlockStoreMultiThreadTest {

    private static final Logger log = LoggerFactory.getLogger(PbftBlockStoreMultiThreadTest.class);

    private Wallet wallet;
    private Wallet wallet2;
    private Wallet wallet3;
    private Wallet wallet4;

    private Block block;
    private Block block2;
    private Block block3;
    private Block block4;

    private PbftBlock pbftBlock;
    private PbftBlock pbftBlock2;
    private PbftBlock pbftBlock3;
    private PbftBlock pbftBlock4;

    private PbftMessage prePrepare;
    private PbftMessage prepare;
    private PbftMessage prepare2;
    private PbftMessage prepare3;
    private PbftMessage prepare4;

    private PbftMessage commit;
    private PbftMessage commit2;
    private PbftMessage commit3;
    private PbftMessage commit4;

    private PbftMessage viewChange;
    private PbftMessage viewChange2;
    private PbftMessage viewChange3;
    private PbftMessage viewChange4;

    private PbftMessageSet pbftMessageSet;
    private PbftMessageSet pbftMessageSet2;
    private PbftMessageSet pbftMessageSet3;
    private PbftMessageSet pbftMessageSet4;
    private final Map<String, PbftMessage> prepareMap = new TreeMap<>();
    private final Map<String, PbftMessage> commitMap = new TreeMap<>();
    private final Map<String, PbftMessage> viewChangeMap = new TreeMap<>();

    private LevelDbDataSource ds;
    private PbftBlockKeyStore blockKeyStore;

    private LevelDbDataSource blockDs;
    private PbftBlockStore blockStore;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        wallet = new Wallet();
        wallet2 = new Wallet(null, "/tmp/",
                "test2" + TimeUtils.time(), "Password1234!");
        wallet3 = new Wallet(null, "/tmp/",
                "test3" + TimeUtils.time(), "Password1234!");
        wallet4 = new Wallet(null, "/tmp/",
                "test4" + TimeUtils.time(), "Password1234!");

        block = new TestUtils(wallet).sampleBlock();
        block2 = new TestUtils(wallet).sampleBlock(block.getIndex() + 1, block.getHash());
        block3 = new TestUtils(wallet).sampleBlock(block2.getIndex() + 1, block2.getHash());
        block4 = new TestUtils(wallet).sampleBlock(block3.getIndex() + 1, block3.getHash());

        prePrepare = new PbftMessage(PBFT_PREPREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet,
                block);
        log.debug(prePrepare.toJsonObject().toString());

        prepare = new PbftMessage(PBFT_PREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet,
                null);
        log.debug(prepare.toJsonObject().toString());

        prepare2 = new PbftMessage(PBFT_PREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet2,
                null);
        log.debug(prepare2.toJsonObject().toString());

        prepare3 = new PbftMessage(PBFT_PREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet3,
                null);
        log.debug(prepare3.toJsonObject().toString());

        prepare4 = new PbftMessage(PBFT_PREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet4,
                null);
        log.debug(prepare4.toJsonObject().toString());

        commit = new PbftMessage(PBFT_COMMIT,
                0L,
                0L,
                block.getHash(),
                null,
                wallet,
                null);
        log.debug(commit.toJsonObject().toString());

        commit2 = new PbftMessage(PBFT_COMMIT,
                0L,
                0L,
                block.getHash(),
                null,
                wallet2,
                null);
        log.debug(commit2.toJsonObject().toString());

        commit3 = new PbftMessage(PBFT_COMMIT,
                0L,
                0L,
                block.getHash(),
                null,
                wallet3,
                null);
        log.debug(commit3.toJsonObject().toString());

        commit4 = new PbftMessage(PBFT_COMMIT,
                0L,
                0L,
                block.getHash(),
                null,
                wallet4,
                null);
        log.debug(commit4.toJsonObject().toString());

        viewChange = new PbftMessage(PBFT_VIEWCHANGE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet,
                null);
        log.debug(viewChange.toJsonObject().toString());

        viewChange2 = new PbftMessage(PBFT_VIEWCHANGE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet2,
                null);
        log.debug(viewChange2.toJsonObject().toString());

        viewChange3 = new PbftMessage(PBFT_VIEWCHANGE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet3,
                null);
        log.debug(viewChange3.toJsonObject().toString());

        viewChange4 = new PbftMessage(PBFT_VIEWCHANGE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet4,
                null);
        log.debug(viewChange4.toJsonObject().toString());

        prepareMap.put(prepare.getSignatureHex(), prepare);
        prepareMap.put(prepare2.getSignatureHex(), prepare2);
        prepareMap.put(prepare3.getSignatureHex(), prepare3);
        prepareMap.put(prepare4.getSignatureHex(), prepare4);

        commitMap.put(commit.getSignatureHex(), commit);
        commitMap.put(commit2.getSignatureHex(), commit2);
        commitMap.put(commit3.getSignatureHex(), commit3);
        commitMap.put(commit4.getSignatureHex(), commit4);

        viewChangeMap.put(viewChange.getSignatureHex(), viewChange);
        viewChangeMap.put(viewChange2.getSignatureHex(), viewChange2);
        viewChangeMap.put(viewChange3.getSignatureHex(), viewChange3);
        viewChangeMap.put(viewChange4.getSignatureHex(), viewChange4);

        pbftMessageSet = new PbftMessageSet(prePrepare, prepareMap, commitMap, null);
        pbftMessageSet2 = new PbftMessageSet(prePrepare, null, null, null);
        pbftMessageSet3 = new PbftMessageSet(prePrepare, prepareMap, null, null);
        pbftMessageSet4 = new PbftMessageSet(prePrepare, prepareMap, commitMap, viewChangeMap);

        this.pbftBlock = new PbftBlock(this.block, this.pbftMessageSet);
        this.pbftBlock2 = new PbftBlock(this.block2, this.pbftMessageSet2);
        this.pbftBlock3 = new PbftBlock(this.block3, this.pbftMessageSet3);
        this.pbftBlock4 = new PbftBlock(this.block4, this.pbftMessageSet4);

        StoreTestUtils.clearTestDb();

        this.ds = new LevelDbDataSource(StoreTestUtils.getTestPath(), "pbftBlockKeyStoreTest");
        this.blockKeyStore = new PbftBlockKeyStore(ds);
        this.blockKeyStore.put(this.pbftBlock.getIndex(), this.pbftBlock.getHash());

        this.blockDs = new LevelDbDataSource(StoreTestUtils.getTestPath(), "pbftBlockStoreTest");
        this.blockStore = new PbftBlockStore(blockDs);
        this.blockStore.put(this.pbftBlock.getHash(), this.pbftBlock);

    }

    @Test
    public void putTestMultiThread() {
        long testNumber = 1000;
        for (long l = 0L; l < testNumber; l++) {
            this.blockStore.put(HashUtil.sha3(ByteUtil.longToBytes(l)), pbftBlock);
        }
        log.debug("blockStore size= " + this.blockStore.size());
        assertEquals(testNumber + 1, this.blockStore.size());
    }

    @Test
    public void putMutiThreadMemoryTest() throws InterruptedException {
        TestConstants.PerformanceTest.apply();

        System.gc();
        Thread.sleep(20000);

        this.putTestMultiThread();

        System.gc();
        Thread.sleep(3000000);
    }

    @After
    public void tearDown() {
        StoreTestUtils.clearTestDb();
    }

}
