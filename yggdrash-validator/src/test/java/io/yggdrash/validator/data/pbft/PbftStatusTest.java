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
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import static io.yggdrash.common.config.Constants.PBFT_COMMIT;
import static io.yggdrash.common.config.Constants.PBFT_PREPARE;
import static io.yggdrash.common.config.Constants.PBFT_PREPREPARE;
import static io.yggdrash.common.config.Constants.PBFT_VIEWCHANGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PbftStatusTest {

    private static final Logger log = LoggerFactory.getLogger(PbftStatusTest.class);

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

    private Map<String, PbftMessage> unConfirmedMessageMap = new TreeMap<>();
    private Map<String, PbftMessage> unConfirmedMessageMap2 = new TreeMap<>();
    private Map<String, PbftMessage> unConfirmedMessageMap3 = new TreeMap<>();
    private Map<String, PbftMessage> unConfirmedMessageMap4 = new TreeMap<>();

    private PbftStatus pbftStatus;
    private PbftStatus pbftStatus2;
    private PbftStatus pbftStatus3;
    private PbftStatus pbftStatus4;

    @Before
    public void setup() throws IOException, InvalidCipherTextException {
        wallet = new Wallet();
        wallet2 = new Wallet(null, "/tmp/",
                "test2" + TimeUtils.time(), "Password1234!");
        wallet3 = new Wallet(null, "/tmp/",
                "test3" + TimeUtils.time(), "Password1234!");
        wallet4 = new Wallet(null, "/tmp/",
                "test4" + TimeUtils.time(), "Password1234!");

        block = new TestUtils(wallet).sampleBlock(1);

        prePrepare = new PbftMessage(PBFT_PREPREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet,
                block);
        prepare = new PbftMessage(PBFT_PREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet,
                null);
        prepare2 = new PbftMessage(PBFT_PREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet2,
                null);
        prepare3 = new PbftMessage(PBFT_PREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet3,
                null);
        prepare4 = new PbftMessage(PBFT_PREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet4,
                null);
        commit = new PbftMessage(PBFT_COMMIT,
                0L,
                0L,
                block.getHash(),
                null,
                wallet,
                null);
        commit2 = new PbftMessage(PBFT_COMMIT,
                0L,
                0L,
                block.getHash(),
                null,
                wallet2,
                null);
        commit3 = new PbftMessage(PBFT_COMMIT,
                0L,
                0L,
                block.getHash(),
                null,
                wallet3,
                null);
        commit4 = new PbftMessage(PBFT_COMMIT,
                0L,
                0L,
                block.getHash(),
                null,
                wallet4,
                null);
        viewChange = new PbftMessage(PBFT_VIEWCHANGE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet,
                null);
        viewChange2 = new PbftMessage(PBFT_VIEWCHANGE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet2,
                null);
        viewChange3 = new PbftMessage(PBFT_VIEWCHANGE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet3,
                null);
        viewChange4 = new PbftMessage(PBFT_VIEWCHANGE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet4,
                null);

        unConfirmedMessageMap.put(prePrepare.getSignatureHex(), prePrepare);

        unConfirmedMessageMap2.put(prePrepare.getSignatureHex(), prePrepare);
        unConfirmedMessageMap2.put(prepare.getSignatureHex(), prepare);
        unConfirmedMessageMap2.put(prepare2.getSignatureHex(), prepare2);
        unConfirmedMessageMap2.put(prepare3.getSignatureHex(), prepare3);
        unConfirmedMessageMap2.put(prepare4.getSignatureHex(), prepare4);

        unConfirmedMessageMap3.put(prePrepare.getSignatureHex(), prePrepare);
        unConfirmedMessageMap3.put(prepare.getSignatureHex(), prepare);
        unConfirmedMessageMap3.put(prepare2.getSignatureHex(), prepare2);
        unConfirmedMessageMap3.put(prepare3.getSignatureHex(), prepare3);
        unConfirmedMessageMap3.put(prepare4.getSignatureHex(), prepare4);
        unConfirmedMessageMap3.put(commit.getSignatureHex(), commit);
        unConfirmedMessageMap3.put(commit2.getSignatureHex(), commit2);
        unConfirmedMessageMap3.put(commit3.getSignatureHex(), commit3);
        unConfirmedMessageMap3.put(commit4.getSignatureHex(), commit4);

        unConfirmedMessageMap4.put(viewChange.getSignatureHex(), viewChange);
        unConfirmedMessageMap4.put(viewChange2.getSignatureHex(), viewChange2);
        unConfirmedMessageMap4.put(viewChange3.getSignatureHex(), viewChange3);
        unConfirmedMessageMap4.put(viewChange4.getSignatureHex(), viewChange4);

        pbftStatus = new PbftStatus(1, unConfirmedMessageMap, TimeUtils.time(), wallet);
        pbftStatus2 = new PbftStatus(1, unConfirmedMessageMap2, TimeUtils.time(), wallet2);
        pbftStatus3 = new PbftStatus(1, unConfirmedMessageMap3, TimeUtils.time(), wallet3);
        pbftStatus4 = new PbftStatus(1, unConfirmedMessageMap4, TimeUtils.time(), wallet4);

    }

    @Test
    public void constuctorTest_Default() {
        assertEquals(1, pbftStatus.getUnConfirmedPbftMessageMap().size());
        assertEquals(5, pbftStatus2.getUnConfirmedPbftMessageMap().size());
        assertEquals(9, pbftStatus3.getUnConfirmedPbftMessageMap().size());
        assertEquals(4, pbftStatus4.getUnConfirmedPbftMessageMap().size());
    }

    @Test
    public void constuctorTest_Proto() {
        {
            PbftProto.PbftStatus pbftStatusProto = PbftStatus.toProto(this.pbftStatus);
            PbftStatus newPbftStatus = new PbftStatus(pbftStatusProto);
            log.debug(this.pbftStatus.toJsonObject().toString());
            log.debug(newPbftStatus.toJsonObject().toString());
            assertEquals(newPbftStatus, this.pbftStatus);
        }
        {
            PbftProto.PbftStatus pbftStatusProto = PbftStatus.toProto(this.pbftStatus2);
            PbftStatus newPbftStatus = new PbftStatus(pbftStatusProto);
            log.debug(this.pbftStatus2.toJsonObject().toString());
            log.debug(newPbftStatus.toJsonObject().toString());
            assertEquals(newPbftStatus, this.pbftStatus2);
        }
        {
            PbftProto.PbftStatus pbftStatusProto = PbftStatus.toProto(this.pbftStatus3);
            PbftStatus newPbftStatus = new PbftStatus(pbftStatusProto);
            log.debug(this.pbftStatus3.toJsonObject().toString());
            log.debug(newPbftStatus.toJsonObject().toString());
            assertEquals(newPbftStatus, this.pbftStatus3);
        }
        {
            PbftProto.PbftStatus pbftStatusProto = PbftStatus.toProto(this.pbftStatus4);
            PbftStatus newPbftStatus = new PbftStatus(pbftStatusProto);
            log.debug(this.pbftStatus4.toJsonObject().toString());
            log.debug(newPbftStatus.toJsonObject().toString());
            assertEquals(newPbftStatus, this.pbftStatus4);
        }
    }

    @Test
    public void verifyTest() {
        assertTrue(PbftStatus.verify(this.pbftStatus));
        log.debug(Hex.toHexString(this.pbftStatus.getHashForSigning()));
        log.debug(Hex.toHexString(this.pbftStatus.getSignature()));
        assertTrue(PbftStatus.verify(this.pbftStatus2));
        assertTrue(PbftStatus.verify(this.pbftStatus3));
        assertTrue(PbftStatus.verify(this.pbftStatus4));

        PbftStatus falsePbftStatus = new PbftStatus(this.pbftStatus.getIndex(),
                this.pbftStatus.getUnConfirmedPbftMessageMap(),
                TimeUtils.time(),
                this.pbftStatus.getSignature());
        log.debug(Hex.toHexString(falsePbftStatus.getHashForSigning()));
        log.debug(Hex.toHexString(this.pbftStatus.getSignature()));
        // todo: check validation with pubkey
        //assertFalse(PbftStatus.verify(falsePbftStatus));
    }

}
