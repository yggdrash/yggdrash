package io.yggdrash.validator.data.ebft;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.validator.util.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.yggdrash.common.config.Constants.EMPTY_BYTE32;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EbftStatusTest {

    private static final Logger log = LoggerFactory.getLogger(EbftStatusTest.class);

    private DefaultConfig defaultConfig;
    private Wallet wallet0;
    private Wallet wallet1;
    private Wallet wallet2;
    private Wallet wallet3;

    private Block block0;
    private Block block1;
    private Block block11;
    private Block block12;
    private Block block2;
    private Block block21;
    private Block block22;
    private Block block3;
    private Block block31;
    private Block block32;

    private EbftBlock ebftBlock0;
    private EbftBlock ebftBlock1;
    private EbftBlock ebftBlock2;
    private EbftBlock ebftBlock3;

    private EbftStatus ebftStatus1;
    private EbftStatus ebftStatus2;
    private EbftStatus ebftStatus3;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        defaultConfig = new DefaultConfig();

        wallet0 = new Wallet(null, "/tmp/",
                "test0" + TimeUtils.time(), "Password1234!");
        wallet1 = new Wallet(null, "/tmp/",
                "test1" + TimeUtils.time(), "Password1234!");
        wallet2 = new Wallet(null, "/tmp/",
                "test2" + TimeUtils.time(), "Password1234!");
        wallet3 = new Wallet(null, "/tmp/",
                "test3" + TimeUtils.time(), "Password1234!");

        block0 = new TestUtils(wallet0).sampleBlock(0, EMPTY_BYTE32);
        block1 = new TestUtils(wallet1).sampleBlock(block0.getIndex() + 1, block0.getHash());
        block11 = new TestUtils(wallet2).sampleBlock(block0.getIndex() + 1, block0.getHash());
        block12 = new TestUtils(wallet3).sampleBlock(block0.getIndex() + 1, block0.getHash());
        block2 = new TestUtils(wallet1).sampleBlock(block1.getIndex() + 1, block1.getHash());
        block21 = new TestUtils(wallet2).sampleBlock(block1.getIndex() + 1, block1.getHash());
        block22 = new TestUtils(wallet3).sampleBlock(block1.getIndex() + 1, block1.getHash());
        block3 = new TestUtils(wallet1).sampleBlock(block2.getIndex() + 1, block2.getHash());
        block31 = new TestUtils(wallet2).sampleBlock(block2.getIndex() + 1, block2.getHash());
        block32 = new TestUtils(wallet3).sampleBlock(block2.getIndex() + 1, block2.getHash());

        this.ebftBlock0 = new EbftBlock(this.block0, null);

        List<String> consensusList1 = new ArrayList<>();
        consensusList1.add(wallet0.signHex(block1.getHash(), true));
        consensusList1.add(wallet1.signHex(block1.getHash(), true));
        consensusList1.add(wallet2.signHex(block1.getHash(), true));
        consensusList1.add(wallet3.signHex(block1.getHash(), true));
        this.ebftBlock1 = new EbftBlock(this.block1, consensusList1);

        List<EbftBlock> unConfirmedList1 = new ArrayList<>();
        unConfirmedList1.add(ebftBlock1);
        unConfirmedList1.add(new EbftBlock(block11, null));
        unConfirmedList1.add(new EbftBlock(block12, null));

        List<String> consensusList2 = new ArrayList<>();
        consensusList2.add(wallet0.signHex(block2.getHash(), true));
        consensusList2.add(wallet1.signHex(block2.getHash(), true));
        consensusList2.add(wallet2.signHex(block2.getHash(), true));
        consensusList2.add(wallet3.signHex(block2.getHash(), true));
        this.ebftBlock2 = new EbftBlock(this.block2, consensusList2);

        List<EbftBlock> unConfirmedList2 = new ArrayList<>();
        unConfirmedList2.add(ebftBlock2);
        unConfirmedList2.add(new EbftBlock(block21, null));
        unConfirmedList2.add(new EbftBlock(block22, null));

        List<String> consensusList3 = new ArrayList<>();
        consensusList3.add(wallet0.signHex(block3.getHash(), true));
        consensusList3.add(wallet1.signHex(block3.getHash(), true));
        consensusList3.add(wallet2.signHex(block3.getHash(), true));
        consensusList3.add(wallet3.signHex(block3.getHash(), true));
        this.ebftBlock3 = new EbftBlock(this.block3, consensusList3);

        List<EbftBlock> unConfirmedList3 = new ArrayList<>();
        unConfirmedList3.add(ebftBlock3);
        unConfirmedList3.add(new EbftBlock(block31, null));
        unConfirmedList3.add(new EbftBlock(block32, null));

        ebftStatus1 = new EbftStatus(block1.getIndex() - 1, unConfirmedList1, wallet1);
        ebftStatus2 = new EbftStatus(block2.getIndex() - 1, unConfirmedList2, wallet2);
        ebftStatus3 = new EbftStatus(block3.getIndex() - 1, unConfirmedList3, wallet3);

    }

    @Test
    public void constuctorTest_Default() {
        assertEquals(ebftStatus1.getUnConfirmedEbftBlockList().size(), 3);
        assertEquals(ebftStatus2.getUnConfirmedEbftBlockList().size(), 3);
        assertEquals(ebftStatus3.getUnConfirmedEbftBlockList().size(), 3);
    }

    @Test
    public void constuctorTest_Proto() {
        EbftStatus oldStatus = this.ebftStatus1;
        EbftProto.EbftStatus statusProto = EbftStatus.toProto(oldStatus);
        EbftStatus newStatus = new EbftStatus(statusProto);
        log.debug(oldStatus.toJsonObject().toString());
        log.debug(newStatus.toJsonObject().toString());
        assertTrue(oldStatus.equals(newStatus));
    }

    @Test
    public void verifyTest() {
        assertTrue(EbftStatus.verify(this.ebftStatus1));
        assertTrue(EbftStatus.verify(this.ebftStatus2));
        assertTrue(EbftStatus.verify(this.ebftStatus3));
    }
}
