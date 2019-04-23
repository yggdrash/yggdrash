package io.yggdrash.validator.data.ebft;

import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EbftBlockTest {
    private Wallet wallet0;
    private Wallet wallet1;
    private Wallet wallet2;
    private Wallet wallet3;

    private Block block0;
    private Block block1;
    private Block block2;
    private Block block3;

    private List<String> consensusList0 = new ArrayList<>();
    private List<String> consensusList1 = new ArrayList<>();
    private List<String> consensusList2 = new ArrayList<>();
    private List<String> consensusList3 = new ArrayList<>();

    private EbftBlock ebftBlock0;
    private EbftBlock ebftBlock1;
    private EbftBlock ebftBlock2;
    private EbftBlock ebftBlock3;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        wallet0 = new Wallet(null, "tmp/",
                "test0" + TimeUtils.time(), "Password1234!");
        wallet1 = new Wallet(null, "tmp/",
                "test1" + TimeUtils.time(), "Password1234!");
        wallet2 = new Wallet(null, "tmp/",
                "test2" + TimeUtils.time(), "Password1234!");
        wallet3 = new Wallet(null, "tmp/",
                "test3" + TimeUtils.time(), "Password1234!");

        block0 = new TestUtils(wallet0).sampleBlock(0);
        block1 = new TestUtils(wallet1).sampleBlock(block0.getIndex() + 1, block0.getHash());
        block2 = new TestUtils(wallet2).sampleBlock(block1.getIndex() + 1, block1.getHash());
        block3 = new TestUtils(wallet3).sampleBlock(block2.getIndex() + 1, block2.getHash());

        consensusList0.add(wallet0.signHex(block0.getHash().getBytes(), true));
        consensusList0.add(wallet1.signHex(block0.getHash().getBytes(), true));
        consensusList0.add(wallet2.signHex(block0.getHash().getBytes(), true));
        consensusList0.add(wallet3.signHex(block0.getHash().getBytes(), true));

        consensusList1.add(wallet0.signHex(block1.getHash().getBytes(), true));
        consensusList1.add(wallet1.signHex(block1.getHash().getBytes(), true));
        consensusList1.add(wallet2.signHex(block1.getHash().getBytes(), true));
        consensusList1.add(wallet3.signHex(block1.getHash().getBytes(), true));

        consensusList2.add(wallet0.signHex(block2.getHash().getBytes(), true));
        consensusList2.add(wallet1.signHex(block2.getHash().getBytes(), true));
        consensusList2.add(wallet2.signHex(block2.getHash().getBytes(), true));
        consensusList2.add(wallet3.signHex(block2.getHash().getBytes(), true));

        consensusList3.add(wallet0.signHex(block3.getHash().getBytes(), true));
        consensusList3.add(wallet1.signHex(block3.getHash().getBytes(), true));
        consensusList3.add(wallet2.signHex(block3.getHash().getBytes(), true));
        consensusList3.add(wallet3.signHex(block3.getHash().getBytes(), true));

        ebftBlock0 = new EbftBlock(block0, consensusList0);
        ebftBlock1 = new EbftBlock(block1, consensusList1);
        ebftBlock2 = new EbftBlock(block2, consensusList2);
        ebftBlock3 = new EbftBlock(block3, consensusList3);
    }

    @Test
    public void constructorAndVerifyTest() {
        assertTrue(EbftBlock.verify(ebftBlock0));
        assertTrue(EbftBlock.verify(ebftBlock1));
        assertTrue(EbftBlock.verify(ebftBlock2));
        assertTrue(EbftBlock.verify(ebftBlock3));

        Block block = new TestUtils(wallet0).sampleBlock();
        EbftBlock testEbftBlock1 = new EbftBlock(block);
        EbftBlock testEbftBlock2 = new EbftBlock(block);
        assertEquals(testEbftBlock1, testEbftBlock2);

        EbftBlock testEbftBlock3 = new EbftBlock(testEbftBlock1.toJsonObject());
        assertEquals(testEbftBlock1, testEbftBlock3);

        EbftBlock testEbftBlock4 = new EbftBlock(testEbftBlock1.toBinary());
        assertEquals(testEbftBlock1, testEbftBlock4);

        EbftBlock testEbftBlock5 = new EbftBlock(testEbftBlock1.getInstance());
        assertEquals(testEbftBlock1, testEbftBlock5);

        EbftBlock testEbftBlock6 = new EbftBlock(testEbftBlock1.toBinary());
        assertEquals(testEbftBlock1, testEbftBlock6);
    }

    @Test
    public void cloneTest() {
        EbftBlock newEbftBlock = new EbftBlock(ebftBlock0.toBinary());
        assertEquals(newEbftBlock, this.ebftBlock0);

        newEbftBlock.clear();
        assertTrue(EbftBlock.verify(this.ebftBlock0));
    }
}
