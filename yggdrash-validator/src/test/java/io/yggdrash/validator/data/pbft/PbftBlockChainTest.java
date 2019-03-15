package io.yggdrash.validator.data.pbft;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.yggdrash.StoreTestUtils;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.util.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.yggdrash.common.config.Constants.PBFT_COMMIT;
import static io.yggdrash.common.config.Constants.PBFT_PREPARE;
import static io.yggdrash.common.config.Constants.PBFT_PREPREPARE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PbftBlockChainTest {
    private static final Logger log = LoggerFactory.getLogger(PbftBlockChainTest.class);

    private DefaultConfig defaultConfig;
    private Wallet wallet0;
    private Wallet wallet1;
    private Wallet wallet2;
    private Wallet wallet3;

    private Block block0;
    private Block block1;
    private Block block2;
    private Block block3;

    private PbftBlockChain pbftBlockChain;
    private PbftBlock pbftBlock0;
    private PbftBlock pbftBlock1;
    private PbftBlock pbftBlock2;
    private PbftBlock pbftBlock3;

    private PbftMessageSet pbftMessageSet0;
    private PbftMessageSet pbftMessageSet1;
    private PbftMessageSet pbftMessageSet2;
    private PbftMessageSet pbftMessageSet3;


    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        defaultConfig = new DefaultConfig();

        wallet0 = new Wallet(defaultConfig);
        wallet1 = new Wallet(null, "/tmp/",
                "test2" + TimeUtils.time(), "Password1234!");
        wallet2 = new Wallet(null, "/tmp/",
                "test3" + TimeUtils.time(), "Password1234!");
        wallet3 = new Wallet(null, "/tmp/",
                "test4" + TimeUtils.time(), "Password1234!");

        block0 = this.genesisBlock();
        block1 = new TestUtils(wallet1).sampleBlock(block0.getIndex() + 1, block0.getHash());
        block2 = new TestUtils(wallet2).sampleBlock(block1.getIndex() + 1, block1.getHash());
        block3 = new TestUtils(wallet3).sampleBlock(block2.getIndex() + 1, block2.getHash());

        StoreTestUtils.clearTestDb();

        this.pbftBlockChain = new PbftBlockChain(block0, StoreTestUtils.getTestPath(),
                "/pbftKey",
                "/pbftBlock",
                "/pbftTx");

        this.pbftMessageSet0 = makePbftMessageSet(block0);
        this.pbftBlock0 = new PbftBlock(this.block0, this.pbftMessageSet0);

        this.pbftMessageSet1 = makePbftMessageSet(block1);
        this.pbftBlock1 = new PbftBlock(this.block1, this.pbftMessageSet1);

        this.pbftMessageSet2 = makePbftMessageSet(block2);
        this.pbftBlock2 = new PbftBlock(this.block2, this.pbftMessageSet2);

        this.pbftMessageSet3 = makePbftMessageSet(block3);
        this.pbftBlock3 = new PbftBlock(this.block3, this.pbftMessageSet3);
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
        PbftMessage prePrepare = makePbftMessage(PBFT_PREPREPARE, block, wallet0);

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

        return new PbftMessageSet(prePrepare, prepareMap, commitMap, null);
    }

    private Block genesisBlock() {
        String genesisString;
        ClassPathResource cpr = new ClassPathResource("genesis/genesis.json");
        try {
            byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
            genesisString = new String(bdata, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new NotValidateException("Error genesisFile");
        }

        return new Block(new Gson().fromJson(genesisString, JsonObject.class));
    }

    @Test
    public void constuctorTest() {
        assertNotNull(this.pbftBlockChain);
        assertEquals(this.pbftBlockChain.getLastConfirmedBlock().getIndex(), 0L);
    }

    @Test
    public void getterTest() {
        assertNotNull(this.pbftBlockChain.getChain());
        assertNotNull(this.pbftBlockChain.getBlockKeyStore());
        assertNotNull(this.pbftBlockChain.getBlockStore());
        assertNotNull(this.pbftBlockChain.getGenesisBlock());
        assertNotNull(this.pbftBlockChain.getGenesisBlock());
        assertEquals(this.pbftBlockChain.getUnConfirmedData().size(), 0);
        assertNotNull(this.pbftBlockChain.getTransactionStore());
        assertNotNull(this.pbftBlockChain.getLastConfirmedBlock());
    }

    @Test
    public void getPbftBlockListTest() {
        PbftBlockChain pbftBlockChain = new PbftBlockChain(block0, StoreTestUtils.getTestPath(),
                "/pbftKeyTest",
                "/pbftBlockTest",
                "/pbftTxTest");

        // todo: check speed (put, get)
        int count = 100;
        PbftBlock newBlock = pbftBlockChain.getGenesisBlock();
        Block block;

        for (int i = 0; i < count; i++) {
            block = new TestUtils(wallet0).sampleBlock(newBlock.getIndex() + 1, newBlock.getHash());
            newBlock = new PbftBlock(block, null);

            pbftBlockChain.getBlockKeyStore().put(newBlock.getIndex(), newBlock.getHash());
            pbftBlockChain.getBlockStore().put(newBlock.getHash(), newBlock);
        }

        List<PbftBlock> pbftBlockList = pbftBlockChain.getPbftBlockList(1, 100);
        assertEquals(pbftBlockList.size(), count);
    }

    @Test
    public void addBlockTest() {
        this.pbftBlockChain.addBlock(pbftBlock1);
        assertEquals(1L, this.pbftBlockChain.getLastConfirmedBlock().getIndex());

        this.pbftBlockChain.addBlock(pbftBlock2);
        assertEquals(2L, this.pbftBlockChain.getLastConfirmedBlock().getIndex());

        this.pbftBlockChain.addBlock(pbftBlock3);
        assertEquals(3L, this.pbftBlockChain.getLastConfirmedBlock().getIndex());
    }
}
