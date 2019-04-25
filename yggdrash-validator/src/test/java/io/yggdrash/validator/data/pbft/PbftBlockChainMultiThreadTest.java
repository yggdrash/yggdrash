package io.yggdrash.validator.data.pbft;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.yggdrash.StoreTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.common.utils.SerializationUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockImpl;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import static io.yggdrash.common.config.Constants.PBFT_COMMIT;
import static io.yggdrash.common.config.Constants.PBFT_PREPARE;
import static io.yggdrash.common.config.Constants.PBFT_PREPREPARE;
import static java.lang.Thread.sleep;

@RunWith(ConcurrentTestRunner.class)
public class PbftBlockChainMultiThreadTest {
    private static final Logger log = LoggerFactory.getLogger(PbftBlockChainMultiThreadTest.class);

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

    private Block makeBlock(long index, byte[] prevHash) {
        return new TestUtils(wallet0).sampleBlock(index, prevHash);
    }

    private PbftBlock makePbftBlock(long index, byte[] prevHash) {
        Block block = makeBlock(index, prevHash);
        return new PbftBlock(block, makePbftMessageSet(block));
    }

    @Test
    @ThreadCount(8)
    public void addBlockMultiThreadTest() throws InterruptedException {
        TestConstants.PerformanceTest.apply();

        System.gc();
        sleep(20000);

        long testCount = 2000;
        for (long l = 0; l < testCount; l++) {
            long index = this.pbftBlockChain.getLastConfirmedBlock().getIndex() + 1;
            byte[] prevHash = this.pbftBlockChain.getLastConfirmedBlock().getHash().getBytes();

            PbftBlock pbftBlock = makePbftBlock(index, prevHash);
            this.pbftBlockChain.addBlock(pbftBlock);
        }

        log.debug("BlockKeyStore size: {}", this.pbftBlockChain.getBlockKeyStore().size());
        log.debug("BlockStore size: {}", this.pbftBlockChain.getBlockStore().size());

        System.gc();
        sleep(3000000);
    }
}
