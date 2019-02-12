package io.yggdrash.validator.data.pbft;

import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.validator.util.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PbftMessageTest {

    private static final Logger log = LoggerFactory.getLogger(PbftMessageTest.class);
    private static final String PREPREPARE = "PREPREPA";
    private static final String PREPARE = "PREPAREM";
    private static final String COMMIT = "COMMITMS";
    private static final String VIEWCHANGE = "VIEWCHAN";

    private Wallet wallet;
    private Block block;
    private PbftMessage prePrepare;
    private PbftMessage prepare;
    private PbftMessage commit;
    private PbftMessage viewChange;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        wallet = new Wallet();
        block = new TestUtils(wallet).sampleBlock();

        prePrepare = new PbftMessage(PREPREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet,
                block);
        log.debug(prePrepare.toJsonObject().toString());

        prepare = new PbftMessage(PREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet,
                null);
        log.debug(prepare.toJsonObject().toString());

        commit = new PbftMessage(COMMIT,
                0L,
                0L,
                block.getHash(),
                null,
                wallet,
                null);
        log.debug(commit.toJsonObject().toString());

        viewChange = new PbftMessage(VIEWCHANGE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet,
                null);
        log.debug(viewChange.toJsonObject().toString());
    }

    @Test
    public void constuctorTest_Wallet() {
        assertNotNull(prePrepare);
        assertNotNull(prepare);
        assertNotNull(commit);
        assertNotNull(viewChange);
    }

    @Test
    public void constuctorTest_Bytes() {
        byte[] prePrepareBytes = this.prePrepare.toBinary();
        PbftMessage prePrepare = new PbftMessage(prePrepareBytes);
        log.debug(prePrepare.toJsonObject().toString());
        assertEquals(prePrepare.toJsonObject().toString(), this.prePrepare.toJsonObject().toString());

        byte[] preparePbftBytes = prepare.toBinary();
        PbftMessage prepare = new PbftMessage(preparePbftBytes);
        log.debug(prepare.toJsonObject().toString());
        assertEquals(prepare.toJsonObject().toString(), this.prepare.toJsonObject().toString());

        byte[] commitBytes = commit.toBinary();
        PbftMessage commit = new PbftMessage(commitBytes);
        log.debug(commit.toJsonObject().toString());
        assertEquals(commit.toJsonObject().toString(), this.commit.toJsonObject().toString());

        byte[] viewChangeBytes = viewChange.toBinary();
        PbftMessage viewChange = new PbftMessage(viewChangeBytes);
        log.debug(viewChange.toJsonObject().toString());
        assertEquals(viewChange.toJsonObject().toString(), this.viewChange.toJsonObject().toString());
    }

    @Test
    public void constuctorTest_JsonObect() {
        JsonObject prePrepareJsonObject = prePrepare.toJsonObject();
        PbftMessage prePrepare = new PbftMessage(prePrepareJsonObject);
        log.debug(prePrepare.toJsonObject().toString());
        assertEquals(prePrepare.toJsonObject().toString(), this.prePrepare.toJsonObject().toString());

        JsonObject prepareJsonObject = prepare.toJsonObject();
        PbftMessage prepare = new PbftMessage(prepareJsonObject);
        log.debug(prepare.toJsonObject().toString());
        assertEquals(prepare.toJsonObject().toString(), this.prepare.toJsonObject().toString());

        JsonObject commitJsonObject = commit.toJsonObject();
        PbftMessage commit = new PbftMessage(commitJsonObject);
        log.debug(commit.toJsonObject().toString());
        assertEquals(commit.toJsonObject().toString(), this.commit.toJsonObject().toString());

        JsonObject viewChangeJsonObject = viewChange.toJsonObject();
        PbftMessage viewChange = new PbftMessage(viewChangeJsonObject);
        log.debug(viewChange.toJsonObject().toString());
        assertEquals(viewChange.toJsonObject().toString(), this.viewChange.toJsonObject().toString());
    }

    @Test
    public void constuctorTest_Proto() {
        PbftProto.PbftMessage prePrepare = PbftMessage.toProto(this.prePrepare);
        PbftMessage newPrePrepare = new PbftMessage(prePrepare);
        log.debug(newPrePrepare.toJsonObject().toString());
        assertEquals(newPrePrepare.toJsonObject().toString(), this.prePrepare.toJsonObject().toString());

        PbftProto.PbftMessage prepare = PbftMessage.toProto(this.prepare);
        PbftMessage newPrepare = new PbftMessage(prepare);
        log.debug(newPrepare.toJsonObject().toString());
        assertEquals(newPrepare.toJsonObject().toString(), this.prepare.toJsonObject().toString());

        PbftProto.PbftMessage commit = PbftMessage.toProto(this.commit);
        PbftMessage newCommit = new PbftMessage(commit);
        log.debug(newCommit.toJsonObject().toString());
        assertEquals(newCommit.toJsonObject().toString(), this.commit.toJsonObject().toString());

        PbftProto.PbftMessage viewChange = PbftMessage.toProto(this.viewChange);
        PbftMessage newViewChange = new PbftMessage(viewChange);
        log.debug(newViewChange.toJsonObject().toString());
        assertEquals(newViewChange.toJsonObject().toString(), this.viewChange.toJsonObject().toString());
    }

    @Test
    public void getterTest() {
        try {
            log.debug("type: " + this.prepare.getType());
            log.debug("viewNumber: " + this.prepare.getViewNumber());
            log.debug("seqNumber: " + this.prepare.getSeqNumber());
            log.debug("hash: " + Hex.toHexString(this.prepare.getHash()));
            log.debug("hashHex: " + this.prepare.getHashHex());
            log.debug("result: " + (Arrays.equals(this.prepare.getResult(), null) ? "null" :
                    Hex.toHexString(this.prepare.getResult())));
            log.debug("signature: " + Hex.toHexString(this.prepare.getSignature()));
            log.debug("signatureHex: " + this.prepare.getSignatureHex());
            log.debug("block: " + (this.prepare.getBlock() == null ? "null" :
                    this.prepare.getBlock().toString()));
        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }
    }


}
