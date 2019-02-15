package io.yggdrash.validator.data.pbft;

import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.validator.util.TestUtils;
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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PbftMessageSetTest {

    private static final Logger log = LoggerFactory.getLogger(PbftMessageSetTest.class);

    private Wallet wallet;
    private Wallet wallet2;
    private Wallet wallet3;
    private Wallet wallet4;

    private Block block;
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
    private Map<String, PbftMessage> prepareMap = new TreeMap<>();
    private Map<String, PbftMessage> commitMap = new TreeMap<>();
    private Map<String, PbftMessage> viewChangeMap = new TreeMap<>();

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        wallet = new Wallet();
        wallet2 = new Wallet(null, "/tmp/", "test2", "Password1234!");
        wallet3 = new Wallet(null, "/tmp/", "test3", "Password1234!");
        wallet4 = new Wallet(null, "/tmp/", "test4", "Password1234!");

        block = new TestUtils(wallet).sampleBlock();

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

    }

    @Test
    public void constuctorTest_Default() {
        log.debug(pbftMessageSet.toJsonObject().toString());
        assertNotNull(this.pbftMessageSet);

        assert this.pbftMessageSet.getPrepareMap().size() == 4
                && this.pbftMessageSet.getCommitMap().size() == 4
                && this.pbftMessageSet.getViewChangeMap().size() == 4;
    }


    @Test
    public void constuctorTest_NullException() {
        PbftMessageSet pbftMessageSet = new PbftMessageSet(this.prePrepare, null, null, null);
        log.debug(pbftMessageSet.toJsonObject().toString());
        assertNotNull(pbftMessageSet);

        try {
            new PbftMessageSet(null, null, null, null);
            assert false;
        } catch (NotValidateException e) {
            log.debug(e.getMessage());
            assert true;
        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }
    }

    @Test
    public void constuctorTest_JsonObect() {
        {
            PbftMessageSet messageSet = this.pbftMessageSet;
            PbftMessageSet newMessageSet = new PbftMessageSet(messageSet.toJsonObject());
            log.debug(newMessageSet.toJsonObject().toString());
            assertEquals(messageSet.toJsonObject().toString(), newMessageSet.toJsonObject().toString());
        }

        {
            PbftMessageSet messageSet = this.pbftMessageSet2;
            PbftMessageSet newMessageSet = new PbftMessageSet(messageSet.toJsonObject());
            log.debug(newMessageSet.toJsonObject().toString());
            assertEquals(messageSet.toJsonObject().toString(), newMessageSet.toJsonObject().toString());
        }

        {
            PbftMessageSet messageSet = this.pbftMessageSet3;
            PbftMessageSet newMessageSet = new PbftMessageSet(messageSet.toJsonObject());
            log.debug(newMessageSet.toJsonObject().toString());
            assertEquals(messageSet.toJsonObject().toString(), newMessageSet.toJsonObject().toString());
        }

        {
            PbftMessageSet messageSet = this.pbftMessageSet4;
            PbftMessageSet newMessageSet = new PbftMessageSet(messageSet.toJsonObject());
            log.debug(newMessageSet.toJsonObject().toString());
            assertEquals(messageSet.toJsonObject().toString(), newMessageSet.toJsonObject().toString());
        }
    }

    @Test
    public void constuctorTest_Bytes() {
        {
            PbftMessageSet messageSet = new PbftMessageSet(
                    this.prePrepare, null, null, null);
            log.debug(messageSet.toJsonObject().toString());

            PbftMessageSet newMessageSet = new PbftMessageSet(messageSet.toBinary());
            log.debug(newMessageSet.toJsonObject().toString());
            assertArrayEquals(messageSet.toBinary(), newMessageSet.toBinary());
        }

        {
            PbftMessageSet messageSet = new PbftMessageSet(
                    this.prePrepare, this.prepareMap, null, null);
            log.debug(messageSet.toJsonObject().toString());

            PbftMessageSet newMessageSet = new PbftMessageSet(messageSet.toBinary());
            log.debug(newMessageSet.toJsonObject().toString());
            assertArrayEquals(messageSet.toBinary(), newMessageSet.toBinary());
        }

        {
            PbftMessageSet messageSet = new PbftMessageSet(
                    this.prePrepare, this.prepareMap, this.commitMap, null);
            log.debug(messageSet.toJsonObject().toString());

            PbftMessageSet newMessageSet = new PbftMessageSet(messageSet.toBinary());
            log.debug(newMessageSet.toJsonObject().toString());
            assertArrayEquals(messageSet.toBinary(), newMessageSet.toBinary());
        }

        {
            PbftMessageSet messageSet = new PbftMessageSet(
                    this.prePrepare, this.prepareMap, this.commitMap, this.viewChangeMap);
            log.debug(messageSet.toJsonObject().toString());

            PbftMessageSet newMessageSet = new PbftMessageSet(messageSet.toBinary());
            log.debug(newMessageSet.toJsonObject().toString());
            assertArrayEquals(messageSet.toBinary(), newMessageSet.toBinary());
        }
    }

    @Test
    public void constuctorTest_Proto() {
        {
            PbftMessageSet messageSet = this.pbftMessageSet;
            PbftProto.PbftMessageSet messageSetProto = PbftMessageSet.toProto(messageSet);
            PbftMessageSet newMessageSet = new PbftMessageSet(messageSetProto);
            log.debug(newMessageSet.toJsonObject().toString());
            assertArrayEquals(messageSet.toBinary(), newMessageSet.toBinary());
        }

        {
            PbftMessageSet messageSet = this.pbftMessageSet2;
            PbftProto.PbftMessageSet messageSetProto = PbftMessageSet.toProto(messageSet);
            PbftMessageSet newMessageSet = new PbftMessageSet(messageSetProto);
            log.debug(newMessageSet.toJsonObject().toString());
            assertArrayEquals(messageSet.toBinary(), newMessageSet.toBinary());
        }

        {
            PbftMessageSet messageSet = this.pbftMessageSet3;
            PbftProto.PbftMessageSet messageSetProto = PbftMessageSet.toProto(messageSet);
            PbftMessageSet newMessageSet = new PbftMessageSet(messageSetProto);
            log.debug(newMessageSet.toJsonObject().toString());
            assertArrayEquals(messageSet.toBinary(), newMessageSet.toBinary());
        }

        {
            PbftMessageSet messageSet = this.pbftMessageSet4;
            PbftProto.PbftMessageSet messageSetProto = PbftMessageSet.toProto(messageSet);
            PbftMessageSet newMessageSet = new PbftMessageSet(messageSetProto);
            log.debug(newMessageSet.toJsonObject().toString());
            assertArrayEquals(messageSet.toBinary(), newMessageSet.toBinary());
        }
    }

    @Test
    public void getterTest() {
        try {
            PbftMessageSet messageSet = this.pbftMessageSet;
            log.debug("prePrepare: " + messageSet.getPrePrepare().toJsonObject().toString());
            assertNotNull(messageSet.getPrePrepare());

            log.debug("prepareMap Size: " + messageSet.getPrepareMap().size());
            assertEquals(messageSet.getPrepareMap().size(), 4);

            log.debug("commitMap Size: " + messageSet.getCommitMap().size());
            assertEquals(messageSet.getCommitMap().size(), 4);

            log.debug("viewChangeMap Size: " + messageSet.getViewChangeMap().size());
            assertEquals(messageSet.getViewChangeMap().size(), 4);

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }

        try {
            PbftMessageSet messageSet = this.pbftMessageSet2;
            log.debug("prePrepare: " + messageSet.getPrePrepare().toJsonObject().toString());
            assertNotNull(messageSet.getPrePrepare());

            log.debug("prepareMap Size: " + messageSet.getPrepareMap().size());
            assertEquals(messageSet.getPrepareMap().size(), 0);

            log.debug("commitMap Size: " + messageSet.getCommitMap().size());
            assertEquals(messageSet.getCommitMap().size(), 0);

            log.debug("viewChangeMap Size: " + messageSet.getViewChangeMap().size());
            assertEquals(messageSet.getViewChangeMap().size(), 0);

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }

        try {
            PbftMessageSet messageSet = this.pbftMessageSet3;
            log.debug("prePrepare: " + messageSet.getPrePrepare().toJsonObject().toString());
            assertNotNull(messageSet.getPrePrepare());

            log.debug("prepareMap Size: " + messageSet.getPrepareMap().size());
            assertEquals(messageSet.getPrepareMap().size(), 4);

            log.debug("commitMap Size: " + messageSet.getCommitMap().size());
            assertEquals(messageSet.getCommitMap().size(), 0);

            log.debug("viewChangeMap Size: " + messageSet.getViewChangeMap().size());
            assertEquals(messageSet.getViewChangeMap().size(), 0);

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }

        try {
            PbftMessageSet messageSet = this.pbftMessageSet4;
            log.debug("prePrepare: " + messageSet.getPrePrepare().toJsonObject().toString());
            assertNotNull(messageSet.getPrePrepare());

            log.debug("prepareMap Size: " + messageSet.getPrepareMap().size());
            assertEquals(messageSet.getPrepareMap().size(), 4);

            log.debug("commitMap Size: " + messageSet.getCommitMap().size());
            assertEquals(messageSet.getCommitMap().size(), 4);

            log.debug("viewChangeMap Size: " + messageSet.getViewChangeMap().size());
            assertEquals(messageSet.getViewChangeMap().size(), 0);

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }
    }

    @Test
    public void verifyTest() {
        assertTrue(PbftMessageSet.verify(this.pbftMessageSet));
        assertTrue(PbftMessageSet.verify(this.pbftMessageSet2));
        assertTrue(PbftMessageSet.verify(this.pbftMessageSet3));
        assertTrue(PbftMessageSet.verify(this.pbftMessageSet4));
    }

}
