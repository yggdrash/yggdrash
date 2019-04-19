package io.yggdrash.validator.data.ebft;

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
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.util.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

@RunWith(ConcurrentTestRunner.class)
public class EbftBlockChainMultiThreadTest {

    private static final Logger log = LoggerFactory.getLogger(EbftBlockChainMultiThreadTest.class);

    private DefaultConfig defaultConfig;
    private Wallet wallet0;
    private Wallet wallet1;
    private Wallet wallet2;
    private Wallet wallet3;

    private Block block0;
    private Block block1;
    private Block block2;
    private Block block3;

    private EbftBlockChain ebftBlockChain;

    private EbftBlock ebftBlock0;
    private EbftBlock ebftBlock1;
    private EbftBlock ebftBlock2;
    private EbftBlock ebftBlock3;

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

        block0 = this.genesisBlock();
        block1 = new TestUtils(wallet0).sampleBlock(block0.getIndex() + 1, block0.getHash());
        block2 = new TestUtils(wallet0).sampleBlock(block1.getIndex() + 1, block1.getHash());
        block3 = new TestUtils(wallet0).sampleBlock(block2.getIndex() + 1, block2.getHash());

        StoreTestUtils.clearTestDb();

        this.ebftBlockChain = new EbftBlockChain(block0, StoreTestUtils.getTestPath(),
                "/ebftKey",
                "/ebftBlock",
                "/ebftTx");

        this.ebftBlock0 = new EbftBlock(this.block0);

        List<String> consensusList1 = new ArrayList<>();
        consensusList1.add(wallet0.signHex(block1.getHash(), true));
        consensusList1.add(wallet1.signHex(block1.getHash(), true));
        consensusList1.add(wallet2.signHex(block1.getHash(), true));
        consensusList1.add(wallet3.signHex(block1.getHash(), true));
        this.ebftBlock1 = new EbftBlock(this.block1, consensusList1);

        List<String> consensusList2 = new ArrayList<>();
        consensusList2.add(wallet0.signHex(block2.getHash(), true));
        consensusList2.add(wallet1.signHex(block2.getHash(), true));
        consensusList2.add(wallet2.signHex(block2.getHash(), true));
        consensusList2.add(wallet3.signHex(block2.getHash(), true));
        this.ebftBlock2 = new EbftBlock(this.block2, consensusList2);

        List<String> consensusList3 = new ArrayList<>();
        consensusList3.add(wallet0.signHex(block3.getHash(), true));
        consensusList3.add(wallet1.signHex(block3.getHash(), true));
        consensusList3.add(wallet2.signHex(block3.getHash(), true));
        consensusList3.add(wallet3.signHex(block3.getHash(), true));
        this.ebftBlock3 = new EbftBlock(this.block3, consensusList3);

    }

    private Block genesisBlock() {
        String genesisString;
        ClassPathResource cpr = new ClassPathResource("genesis/genesis.json");
        try {
            byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
            genesisString = SerializationUtil.deserializeString(bdata);
            log.debug("geneis: " + genesisString);
        } catch (IOException e) {
            throw new NotValidateException("Error genesisFile");
        }

        return new Block(new Gson().fromJson(genesisString, JsonObject.class));
    }

    private Block makeBlock(long index, byte[] prevHash) {
        return new TestUtils(wallet0).sampleBlock(index, prevHash);
    }

    private List<String> makeConsensusList(Block block) {
        List<String> consensusList = new ArrayList<>();
        consensusList.add(wallet0.signHex(block.getHash(), true));
        consensusList.add(wallet1.signHex(block.getHash(), true));
        consensusList.add(wallet2.signHex(block.getHash(), true));
        consensusList.add(wallet3.signHex(block.getHash(), true));
        return consensusList;
    }

    private EbftBlock makeEbftBlock(long index, byte[] prevHash) {
        Block block = makeBlock(index, prevHash);
        return new EbftBlock(block, makeConsensusList(block));
    }


    @Test
    @ThreadCount(8)
    public void addBlockMultiThreadTest() throws InterruptedException {
        TestConstants.PerformanceTest.apply();

        System.gc();
        sleep(20000);

        long testCount = 2000;
        for (long l = 0; l < testCount; l++) {
            long index = this.ebftBlockChain.getLastConfirmedBlock().getIndex() + 1;
            byte[] prevHash = this.ebftBlockChain.getLastConfirmedBlock().getHash().getBytes();

            EbftBlock ebftBlock = makeEbftBlock(index, prevHash);
            this.ebftBlockChain.addBlock(ebftBlock);
        }

        log.debug("BlockKeyStore size: {}", this.ebftBlockChain.getBlockKeyStore().size());
        log.debug("BlockStore size: {}", this.ebftBlockChain.getBlockStore().size());

        System.gc();
        sleep(3000000);
    }

}


