package io.yggdrash.validator.data.pbft;

import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.validator.TestUtils;
import org.junit.Before;
import org.junit.Test;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PbftBlockTest {

    private static final Logger log = LoggerFactory.getLogger(PbftBlockTest.class);

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

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        wallet = new Wallet(null, "tmp/",
                "test1" + TimeUtils.time(), "Password1234!");
        wallet2 = new Wallet(null, "tmp/",
                "test2" + TimeUtils.time(), "Password1234!");
        wallet3 = new Wallet(null, "tmp/",
                "test3" + TimeUtils.time(), "Password1234!");
        wallet4 = new Wallet(null, "tmp/",
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

        pbftMessageSet = new PbftMessageSet(prePrepare, prepareMap, commitMap, viewChangeMap);
        pbftMessageSet2 = new PbftMessageSet(prePrepare, null, null, null);
        pbftMessageSet3 = new PbftMessageSet(prePrepare, prepareMap, null, null);
        pbftMessageSet4 = new PbftMessageSet(prePrepare, prepareMap, commitMap, null);

        this.pbftBlock = new PbftBlock(this.block, this.pbftMessageSet);
        this.pbftBlock2 = new PbftBlock(this.block2, this.pbftMessageSet2);
        this.pbftBlock3 = new PbftBlock(this.block3, this.pbftMessageSet3);
        this.pbftBlock4 = new PbftBlock(this.block4, this.pbftMessageSet4);

    }

    @Test
    public void constuctorTest_Default() {
        log.debug(this.pbftBlock.toJsonObject().toString());
        assertNotNull(this.pbftBlock);

        log.debug(this.pbftBlock2.toJsonObject().toString());
        assertNotNull(this.pbftBlock2);

        log.debug(this.pbftBlock3.toJsonObject().toString());
        assertNotNull(this.pbftBlock3);

        log.debug(this.pbftBlock4.toJsonObject().toString());
        assertNotNull(this.pbftBlock4);
    }

    @Test
    public void constuctorTest_JsonObect() {
        {
            PbftBlock newBlock = new PbftBlock(this.pbftBlock.toJsonObject());
            log.debug(newBlock.toJsonObject().toString());
            assertEquals(newBlock, this.pbftBlock);
        }

        {
            PbftBlock newBlock = new PbftBlock(this.pbftBlock2.toJsonObject());
            log.debug(newBlock.toJsonObject().toString());
            assertEquals(newBlock, this.pbftBlock2);
        }

        {
            PbftBlock newBlock = new PbftBlock(this.pbftBlock3.toJsonObject());
            log.debug(newBlock.toJsonObject().toString());
            assertEquals(newBlock, this.pbftBlock3);
        }

        {
            PbftBlock newBlock = new PbftBlock(this.pbftBlock4.toJsonObject());
            log.debug(newBlock.toJsonObject().toString());
            assertEquals(newBlock, this.pbftBlock4);
        }
    }

    @Test
    public void constuctorTest_Bytes() {
        {
            PbftBlock newBlock = new PbftBlock(this.pbftBlock.toBinary());
            log.debug(pbftBlock.toJsonObject().toString());
            log.debug(newBlock.toJsonObject().toString());
            assertEquals(newBlock, this.pbftBlock);
        }

        {
            PbftBlock newBlock = new PbftBlock(this.pbftBlock2.toBinary());
            log.debug(newBlock.toJsonObject().toString());
            assertEquals(newBlock, this.pbftBlock2);
        }

        {
            PbftBlock newBlock = new PbftBlock(this.pbftBlock3.toBinary());
            log.debug(newBlock.toJsonObject().toString());
            assertEquals(newBlock, this.pbftBlock3);
        }

        {
            PbftBlock newBlock = new PbftBlock(this.pbftBlock4.toBinary());
            log.debug(newBlock.toJsonObject().toString());
            assertEquals(newBlock, this.pbftBlock4);
        }
    }

    @Test
    public void constuctorTest_Proto() {
        {
            PbftProto.PbftBlock newBlockProto = this.pbftBlock.getInstance();
            PbftBlock newBlock = new PbftBlock(newBlockProto);
            log.debug(newBlock.toJsonObject().toString());
            assertEquals(newBlock, this.pbftBlock);
        }

        {
            PbftProto.PbftBlock newBlockProto = this.pbftBlock2.getInstance();
            PbftBlock newBlock = new PbftBlock(newBlockProto);
            log.debug(newBlock.toJsonObject().toString());
            assertEquals(newBlock, this.pbftBlock2);
        }

        {
            PbftProto.PbftBlock newBlockProto = this.pbftBlock3.getInstance();
            PbftBlock newBlock = new PbftBlock(newBlockProto);
            log.debug(newBlock.toJsonObject().toString());
            assertEquals(newBlock, this.pbftBlock3);
        }

        {
            PbftProto.PbftBlock newBlockProto = this.pbftBlock4.getInstance();
            PbftBlock newBlock = new PbftBlock(newBlockProto);
            log.debug(newBlock.toJsonObject().toString());
            assertEquals(newBlock, this.pbftBlock4);
        }
    }

    @Test
    public void verifyTest() {
        assertTrue(PbftBlock.verify(this.pbftBlock)
                && PbftBlock.verify(this.pbftBlock2)
                && PbftBlock.verify(this.pbftBlock3)
                && PbftBlock.verify(this.pbftBlock4));
    }

    @Test
    public void cloneTest() {
        PbftBlock newPbftBlock = new PbftBlock(pbftBlock.toBinary());
        assertEquals(newPbftBlock, this.pbftBlock);

        newPbftBlock.clear();
        assertTrue(PbftBlock.verify(this.pbftBlock));
    }

}
