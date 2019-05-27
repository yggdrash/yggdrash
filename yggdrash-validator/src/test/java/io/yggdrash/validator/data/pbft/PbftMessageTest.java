package io.yggdrash.validator.data.pbft;

import com.google.gson.JsonObject;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.validator.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        wallet = new Wallet(new DefaultConfig(), "Password1234!");
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
    public void constructorTest_Wallet() {
        assertNotNull(prePrepare);
        assertNotNull(prepare);
        assertNotNull(commit);
        assertNotNull(viewChange);
    }

    @Test
    public void constructorTest_Bytes() {
        byte[] prePrepareBytes = this.prePrepare.toBinary();
        PbftMessage prePrepare = new PbftMessage(prePrepareBytes);
        log.debug(prePrepare.toJsonObject().toString());
        assertEquals(prePrepare, this.prePrepare);

        byte[] preparePbftBytes = prepare.toBinary();
        PbftMessage prepare = new PbftMessage(preparePbftBytes);
        log.debug(prepare.toJsonObject().toString());
        assertEquals(prepare, this.prepare);

        byte[] commitBytes = commit.toBinary();
        PbftMessage commit = new PbftMessage(commitBytes);
        log.debug(commit.toJsonObject().toString());
        assertEquals(commit, this.commit);

        byte[] viewChangeBytes = viewChange.toBinary();
        PbftMessage viewChange = new PbftMessage(viewChangeBytes);
        log.debug(viewChange.toJsonObject().toString());
        assertEquals(viewChange, this.viewChange);
    }

    @Test
    public void constructorTest_JsonObect() {
        JsonObject prePrepareJsonObject = prePrepare.toJsonObject();
        PbftMessage prePrepare = new PbftMessage(prePrepareJsonObject);
        log.debug(prePrepare.toJsonObject().toString());
        assertEquals(prePrepare, this.prePrepare);

        JsonObject prepareJsonObject = prepare.toJsonObject();
        PbftMessage prepare = new PbftMessage(prepareJsonObject);
        log.debug(prepare.toJsonObject().toString());
        assertEquals(prepare, this.prepare);

        JsonObject commitJsonObject = commit.toJsonObject();
        PbftMessage commit = new PbftMessage(commitJsonObject);
        log.debug(commit.toJsonObject().toString());
        assertEquals(commit, this.commit);

        JsonObject viewChangeJsonObject = viewChange.toJsonObject();
        PbftMessage viewChange = new PbftMessage(viewChangeJsonObject);
        log.debug(viewChange.toJsonObject().toString());
        assertEquals(viewChange, this.viewChange);
    }

    @Test
    public void constructorTest_Proto() {
        PbftProto.PbftMessage prePrepare = PbftMessage.toProto(this.prePrepare);
        PbftMessage newPrePrepare = new PbftMessage(prePrepare);
        log.debug(newPrePrepare.toJsonObject().toString());
        assertEquals(newPrePrepare, this.prePrepare);

        PbftProto.PbftMessage prepare = PbftMessage.toProto(this.prepare);
        PbftMessage newPrepare = new PbftMessage(prepare);
        log.debug(newPrepare.toJsonObject().toString());
        assertEquals(newPrepare, this.prepare);

        PbftProto.PbftMessage commit = PbftMessage.toProto(this.commit);
        PbftMessage newCommit = new PbftMessage(commit);
        log.debug(newCommit.toJsonObject().toString());
        assertEquals(newCommit, this.commit);

        PbftProto.PbftMessage viewChange = PbftMessage.toProto(this.viewChange);
        PbftMessage newViewChange = new PbftMessage(viewChange);
        log.debug(newViewChange.toJsonObject().toString());
        assertEquals(newViewChange, this.viewChange);
    }

    @Test
    public void getterTest() {
        {
            PbftMessage message = this.prePrepare;
            log.debug("type: " + message.getType());
            log.debug("viewNumber: " + message.getViewNumber());
            log.debug("seqNumber: " + message.getSeqNumber());
            log.debug("hash: " + Hex.toHexString(message.getHash()));
            log.debug("hashHex: " + message.getHashHex());
            log.debug("result: " + (Arrays.equals(message.getResult(), null) ? "null" :
                    Hex.toHexString(message.getResult())));
            log.debug("signature: " + Hex.toHexString(message.getSignature()));
            log.debug("signatureHex: " + message.getSignatureHex());
            log.debug("block: " + (message.getBlock() == null ? "null" :
                    message.getBlock().toString()));
            log.debug("address: {}", message.getAddressHex());
        }

        {
            PbftMessage message = this.prepare;
            log.debug("type: " + message.getType());
            log.debug("viewNumber: " + message.getViewNumber());
            log.debug("seqNumber: " + message.getSeqNumber());
            log.debug("hash: " + Hex.toHexString(message.getHash()));
            log.debug("hashHex: " + message.getHashHex());
            log.debug("result: " + (Arrays.equals(message.getResult(), null) ? "null" :
                    Hex.toHexString(message.getResult())));
            log.debug("signature: " + Hex.toHexString(message.getSignature()));
            log.debug("signatureHex: " + message.getSignatureHex());
            log.debug("block: " + (message.getBlock() == null ? "null" :
                    message.getBlock().toString()));
            log.debug("address: {}", message.getAddressHex());
        }

        {
            PbftMessage message = this.commit;
            log.debug("type: " + message.getType());
            log.debug("viewNumber: " + message.getViewNumber());
            log.debug("seqNumber: " + message.getSeqNumber());
            log.debug("hash: " + Hex.toHexString(message.getHash()));
            log.debug("hashHex: " + message.getHashHex());
            log.debug("result: " + (Arrays.equals(message.getResult(), null) ? "null" :
                    Hex.toHexString(message.getResult())));
            log.debug("signature: " + Hex.toHexString(message.getSignature()));
            log.debug("signatureHex: " + message.getSignatureHex());
            log.debug("block: " + (message.getBlock() == null ? "null" :
                    message.getBlock().toString()));
            log.debug("address: {}", message.getAddressHex());
        }

        {
            PbftMessage message = this.viewChange;
            log.debug("type: " + message.getType());
            log.debug("viewNumber: " + message.getViewNumber());
            log.debug("seqNumber: " + message.getSeqNumber());
            log.debug("hash: " + Hex.toHexString(message.getHash()));
            log.debug("hashHex: " + message.getHashHex());
            log.debug("result: " + (Arrays.equals(message.getResult(), null) ? "null" :
                    Hex.toHexString(message.getResult())));
            log.debug("signature: " + Hex.toHexString(message.getSignature()));
            log.debug("signatureHex: " + message.getSignatureHex());
            log.debug("block: " + (message.getBlock() == null ? "null" :
                    message.getBlock().toString()));
            log.debug("address: {}", message.getAddressHex());
        }
    }

    @Test
    public void signVerityTest() {
        {
            PbftMessage message = this.prePrepare;
            byte[] signValue = message.sign(wallet);
            assertArrayEquals(signValue, message.getSignature());
            assertTrue(PbftVerifier.INSTANCE.verify(message));
        }

        {
            PbftMessage message = this.prepare;
            byte[] signValue = message.sign(wallet);
            assertArrayEquals(signValue, message.getSignature());
            assertTrue(PbftVerifier.INSTANCE.verify(message));
        }

        {
            PbftMessage message = this.commit;
            byte[] signValue = message.sign(wallet);
            assertArrayEquals(signValue, message.getSignature());
            assertTrue(PbftVerifier.INSTANCE.verify(message));
        }

        {
            PbftMessage message = this.viewChange;
            byte[] signValue = message.sign(wallet);
            assertArrayEquals(signValue, message.getSignature());
            assertTrue(PbftVerifier.INSTANCE.verify(message));
        }
    }

    @Test
    public void pbftMessageListTest() {
        List<PbftMessage> pbftMessageList = new ArrayList<>();
        pbftMessageList.add(this.prePrepare);
        pbftMessageList.add(this.prepare);
        pbftMessageList.add(this.commit);
        pbftMessageList.add(this.viewChange);

        PbftProto.PbftMessageList pbftMessageListProto = PbftMessage.toProtoList(pbftMessageList);
        List<PbftMessage> newPbftMessageList = PbftMessage.toPbftMessageList(pbftMessageListProto);

        for (int i = 0; i < pbftMessageList.size(); i++) {
            PbftMessage before = pbftMessageList.get(i);
            log.debug("before: " + before.toJsonObject().toString());
            PbftMessage after = newPbftMessageList.get(i);
            log.debug("after:  " + after.toJsonObject().toString());
            assertEquals(before, after);
        }
    }

    @Test
    public void cloneTest() {
        PbftMessage newPbftMessage = this.prePrepare.clone();
        assertEquals(newPbftMessage, this.prePrepare);
        newPbftMessage.clear();
        assertTrue(PbftVerifier.INSTANCE.verify(this.prePrepare));

        newPbftMessage = this.prepare.clone();
        assertEquals(newPbftMessage, this.prepare);
        newPbftMessage.clear();
        assertTrue(PbftVerifier.INSTANCE.verify(this.prepare));

        newPbftMessage = this.commit.clone();
        assertEquals(newPbftMessage, this.commit);
        newPbftMessage.clear();
        assertTrue(PbftVerifier.INSTANCE.verify(this.commit));

        newPbftMessage = this.viewChange.clone();
        assertEquals(newPbftMessage, this.viewChange);
        newPbftMessage.clear();
        assertTrue(PbftVerifier.INSTANCE.verify(this.viewChange));
    }
}
