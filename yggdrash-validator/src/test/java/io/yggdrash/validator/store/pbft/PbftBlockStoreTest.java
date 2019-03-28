package io.yggdrash.validator.store.pbft;

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
import io.yggdrash.validator.util.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.yggdrash.common.config.Constants.EMPTY_BYTE32;
import static io.yggdrash.common.config.Constants.PBFT_COMMIT;
import static io.yggdrash.common.config.Constants.PBFT_PREPARE;
import static io.yggdrash.common.config.Constants.PBFT_PREPREPARE;
import static io.yggdrash.common.config.Constants.PBFT_VIEWCHANGE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PbftBlockStoreTest {
    private static final Logger log = LoggerFactory.getLogger(PbftBlockStoreTest.class);

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

    private LevelDbDataSource blockKeyDs;
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

        this.blockKeyDs = new LevelDbDataSource(StoreTestUtils.getTestPath(), "pbftBlockKeyStoreTest");
        this.blockKeyStore = new PbftBlockKeyStore(blockKeyDs);
        this.blockKeyStore.put(this.pbftBlock.getIndex(), this.pbftBlock.getHash());

        this.blockDs = new LevelDbDataSource(StoreTestUtils.getTestPath(), "pbftBlockStoreTest");
        this.blockStore = new PbftBlockStore(blockDs);
        this.blockStore.put(this.pbftBlock.getHash(), this.pbftBlock);
    }

    @Test
    public void putGetTest() {
        byte[] newHash = blockKeyStore.get(this.pbftBlock.getIndex());
        PbftBlock newBlock = blockStore.get(newHash);
        assertTrue(newBlock.equals(this.pbftBlock));
        assertTrue(blockStore.contains(this.pbftBlock.getHash()));
        assertFalse(blockStore.contains(EMPTY_BYTE32));
    }

    @Test
    public void closeTest() {
        blockStore.close();
        try {
            blockStore.get(this.pbftBlock.getHash());
        } catch (NullPointerException ne) {
            assert true;
            this.blockStore = new PbftBlockStore(blockDs);
            PbftBlock newBlock = blockStore.get(this.pbftBlock.getHash());
            assertTrue(newBlock.equals(this.pbftBlock));
            return;
        }
        assert false;
    }

    @Test
    public void memoryTest() throws InterruptedException {
        TestConstants.PerformanceTest.apply();

        long testNumber = 100000;
        PbftBlock result;
        List<PbftBlock> resultList = new ArrayList<>();

        log.debug("Before free memory: " + Runtime.getRuntime().freeMemory());
        for (long l = 0L; l < testNumber; l++) {
            this.blockStore.put(HashUtil.sha3(ByteUtil.longToBytes(l)), this.pbftBlock);
            result = this.blockStore.get(HashUtil.sha3(ByteUtil.longToBytes(l)));
            resultList.add(result);
        }

        for (PbftBlock pbftBlock : resultList) {
            pbftBlock.clear();
        }
        resultList.clear();

        log.debug("After free memory: " + Runtime.getRuntime().freeMemory());

        System.gc();
        Thread.sleep(20000);
    }

    @Test
    public void memoryTest2() throws InterruptedException {
        TestConstants.PerformanceTest.apply();

        long testNumber = 10000;

        for (long l = 1; l < testNumber; l++) {
            Block newBlock = new TestUtils(wallet).sampleBlock(l, block.getHash());
            PbftBlock newPbftBlock = new PbftBlock(newBlock, this.pbftMessageSet);

            this.blockKeyStore.put(l, newPbftBlock.getHash());
            this.blockStore.put(newPbftBlock.getHash(), newPbftBlock);
        }

        for (long l = 0; l < testNumber; l++) {
            log.debug("blockKeyStore: " + l + " " + Hex.toHexString(this.blockKeyStore.get(l)));
            log.debug("blockStore: " + this.blockStore.get(this.blockKeyStore.get(l)).getIndex());
        }

        System.gc();
        Thread.sleep(300000);
    }

    @After
    public void tearDown() {
        StoreTestUtils.clearTestDb();
    }

}
