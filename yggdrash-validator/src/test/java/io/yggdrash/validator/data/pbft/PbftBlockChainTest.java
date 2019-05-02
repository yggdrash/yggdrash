package io.yggdrash.validator.data.pbft;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.yggdrash.StoreTestUtils;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.common.utils.SerializationUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockImpl;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.validator.TestUtils;
import io.yggdrash.validator.store.pbft.PbftBlockStore;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.util.ArrayList;
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

        wallet0 = new Wallet(null, "tmp/",
                "test0" + TimeUtils.time(), "Password1234!");
        wallet1 = new Wallet(null, "tmp/",
                "test1" + TimeUtils.time(), "Password1234!");
        wallet2 = new Wallet(null, "tmp/",
                "test2" + TimeUtils.time(), "Password1234!");
        wallet3 = new Wallet(null, "tmp/",
                "test3" + TimeUtils.time(), "Password1234!");

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

        PbftMessage prePrepare = makePbftMessage(PBFT_PREPREPARE, block, wallet0);
        return new PbftMessageSet(prePrepare, prepareMap, commitMap, null);
    }

    private Block genesisBlock() {
        String genesisString;
        ClassPathResource cpr = new ClassPathResource("genesis/genesis.json");
        try {
            byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
            genesisString = SerializationUtil.deserializeString(bdata);
        } catch (IOException e) {
            throw new NotValidateException("Error genesisFile");
        }

        return new BlockImpl(new Gson().fromJson(genesisString, JsonObject.class));
    }

    @Test
    public void constuctorTest() {
        assertNotNull(this.pbftBlockChain);
        assertEquals(0L, this.pbftBlockChain.getLastConfirmedBlock().getIndex());
    }

    @Test
    public void getterTest() {
        assertNotNull(this.pbftBlockChain.getBranchId());
        assertNotNull(this.pbftBlockChain.getBlockKeyStore());
        assertNotNull(this.pbftBlockChain.getBlockStore());
        assertNotNull(this.pbftBlockChain.getGenesisBlock());
        assertNotNull(this.pbftBlockChain.getGenesisBlock());
        assertEquals(0, this.pbftBlockChain.getUnConfirmedData().size());
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
            newBlock = new PbftBlock(block, PbftMessageSet.forGenesis());

            pbftBlockChain.getBlockKeyStore().put(newBlock.getIndex(), newBlock.getHash().getBytes());
            pbftBlockChain.getBlockStore().addBlock(newBlock);
        }

        List blockList = getBlockList(pbftBlockChain.getBlockStore(), 1, 100);
        assertEquals(blockList.size(), count);
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

    /**
     * Get BlockList from BlockStore with index, count.
     *
     * @param index index of block (0 <= index)
     * @param count count of blocks (1 < count <= 100)
     * @return list of Block
     */
    private List<ConsensusBlock<PbftProto.PbftBlock>> getBlockList(PbftBlockStore store, long index, long count) {
        List<ConsensusBlock<PbftProto.PbftBlock>> blockList = new ArrayList<>();
        if (index < 0L || count < 1L || count > 100L) {
            log.debug("index or count is not valid");
            return blockList;
        }

        for (long l = index; l < index + count; l++) {
            try {
                blockList.add(store.getBlockByIndex(l));
            } catch (Exception e) {
                break;
            }
        }

        return blockList;
    }

}
