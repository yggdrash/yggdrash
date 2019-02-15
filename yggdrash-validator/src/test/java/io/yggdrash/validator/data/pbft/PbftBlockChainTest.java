package io.yggdrash.validator.data.pbft;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static io.yggdrash.common.config.Constants.PBFT_COMMIT;
import static io.yggdrash.common.config.Constants.PBFT_PREPARE;
import static io.yggdrash.common.config.Constants.PBFT_PREPREPARE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PbftBlockChainTest {
    private static final Logger log = LoggerFactory.getLogger(PbftBlockChainTest.class);

    private DefaultConfig defaultConfig;
    private Wallet wallet;
    private Wallet wallet2;
    private Wallet wallet3;
    private Wallet wallet4;

    private Block block;

    private PbftBlockChain pbftBlockChain;
    private PbftBlock pbftBlock;

    private PbftMessage prePrepare;
    private PbftMessage prepare;
    private PbftMessage prepare2;
    private PbftMessage prepare3;
    private PbftMessage prepare4;

    private PbftMessage commit;
    private PbftMessage commit2;
    private PbftMessage commit3;
    private PbftMessage commit4;

    private PbftMessageSet pbftMessageSet;


    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        defaultConfig = new DefaultConfig();

        wallet = new Wallet(defaultConfig);
        wallet2 = new Wallet(null, "/tmp/", "test2", "Password1234!");
        wallet3 = new Wallet(null, "/tmp/", "test3", "Password1234!");
        wallet4 = new Wallet(null, "/tmp/", "test4", "Password1234!");

        block = this.genesisBlock();

        this.pbftBlockChain = new PbftBlockChain(block, defaultConfig);

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

        this.pbftBlock = new PbftBlock(this.block, this.pbftMessageSet);

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
        assertNotNull(this.pbftBlockChain.getHost());
        assertEquals(this.pbftBlockChain.getPort(), 32918);
        assertNotNull(this.pbftBlockChain.getBlockKeyStore());
        assertNotNull(this.pbftBlockChain.getBlockStore());
        assertNotNull(this.pbftBlockChain.getGenesisBlock());
        assertNotNull(this.pbftBlockChain.getGenesisBlock());
        assertEquals(this.pbftBlockChain.getUnConfirmedMsgMap().size(), 0);
        assertNotNull(this.pbftBlockChain.getTransactionStore());
        assertNotNull(this.pbftBlockChain.getLastConfirmedBlock());
    }

    @Test
    public void getPbftBlockListTest() {
//        int count = 100;
//        PbftBlock newBlock = this.pbftBlockChain.getGenesisBlock();
//        Block block = new TestUtils(wallet).sampleBlock(newBlock.getIndex() + 1, newBlock.getHash());
//
//        for (int i = 0; i < count; i++) {
//            block = new TestUtils(wallet).sampleBlock(block.getIndex() + 1, block.getHash());
//            PbftBlock pbftBlock = new PbftBlock(block, null);
//
//            this.pbftBlockChain.getBlockKeyStore().put(pbftBlock.getIndex(), pbftBlock.getHash());
//            this.pbftBlockChain.getBlockStore().put(pbftBlock.getHash(), pbftBlock);
//        }
//
//        List<PbftBlock> pbftBlockList = this.pbftBlockChain.getPbftBlockList(1, 100);
//        assertEquals(pbftBlockList.size(), count);
    }

}
