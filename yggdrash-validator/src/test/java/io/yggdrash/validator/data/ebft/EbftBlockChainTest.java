package io.yggdrash.validator.data.ebft;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.StoreTestUtils;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.common.utils.SerializationUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.core.blockchain.BlockImpl;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.validator.TestUtils;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EbftBlockChainTest {

    private static final Logger log = LoggerFactory.getLogger(EbftBlockChainTest.class);

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
    private BlockChainManager<EbftProto.EbftBlock> blockChainManager;

    private EbftBlock ebftBlock0;
    private EbftBlock ebftBlock1;
    private EbftBlock ebftBlock2;
    private EbftBlock ebftBlock3;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        defaultConfig = new DefaultConfig();

        wallet0 = new Wallet(null, "tmp/",
                "test0" + TimeUtils.time(), "Aa1234567890!");
        wallet1 = new Wallet(null, "tmp/",
                "test1" + TimeUtils.time(), "Aa1234567890!");
        wallet2 = new Wallet(null, "tmp/",
                "test2" + TimeUtils.time(), "Aa1234567890!");
        wallet3 = new Wallet(null, "tmp/",
                "test3" + TimeUtils.time(), "Aa1234567890!");

        block0 = this.genesisBlock();
        block1 = new TestUtils(wallet0).sampleBlock(block0.getIndex() + 1, block0.getHash());
        block2 = new TestUtils(wallet0).sampleBlock(block1.getIndex() + 1, block1.getHash());
        block3 = new TestUtils(wallet0).sampleBlock(block2.getIndex() + 1, block2.getHash());

        StoreTestUtils.clearTestDb();

        this.ebftBlockChain = new EbftBlockChain(block0, StoreTestUtils.getTestPath(),
                "/ebftKey",
                "/ebftBlock");
        this.blockChainManager = ebftBlockChain.getBlockChainManager();
        this.ebftBlock0 = new EbftBlock(this.block0);

        List<ByteString> consensusList1 = new ArrayList<>();
        consensusList1.add(wallet0.signByteString(block1.getHash().getBytes(), true));
        consensusList1.add(wallet1.signByteString(block1.getHash().getBytes(), true));
        consensusList1.add(wallet2.signByteString(block1.getHash().getBytes(), true));
        consensusList1.add(wallet3.signByteString(block1.getHash().getBytes(), true));
        this.ebftBlock1 = new EbftBlock(this.block1, consensusList1);

        List<ByteString> consensusList2 = new ArrayList<>();
        consensusList2.add(wallet0.signByteString(block2.getHash().getBytes(), true));
        consensusList2.add(wallet1.signByteString(block2.getHash().getBytes(), true));
        consensusList2.add(wallet2.signByteString(block2.getHash().getBytes(), true));
        consensusList2.add(wallet3.signByteString(block2.getHash().getBytes(), true));
        this.ebftBlock2 = new EbftBlock(this.block2, consensusList2);

        List<ByteString> consensusList3 = new ArrayList<>();
        consensusList3.add(wallet0.signByteString(block3.getHash().getBytes(), true));
        consensusList3.add(wallet1.signByteString(block3.getHash().getBytes(), true));
        consensusList3.add(wallet2.signByteString(block3.getHash().getBytes(), true));
        consensusList3.add(wallet3.signByteString(block3.getHash().getBytes(), true));
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

        return new BlockImpl(new Gson().fromJson(genesisString, JsonObject.class));
    }

    @Test
    public void constuctorTest() {
        assertNotNull(this.ebftBlockChain);
        assertEquals(0L, this.blockChainManager.getLastIndex());
    }

    @Test
    public void getterTest() {
        assertNotNull(this.ebftBlockChain.getBranchId());
        assertNotNull(this.ebftBlockChain.getBlockKeyStore());
        assertNotNull(this.ebftBlockChain.getGenesisBlock());
        assertNotNull(this.ebftBlockChain.getGenesisBlock());
        assertEquals(0, this.ebftBlockChain.getUnConfirmedData().size());
        assertNotNull(this.blockChainManager.getLastConfirmedBlock());
    }

    @Test
    public void addBlockTest() {
        this.ebftBlockChain.addBlock(ebftBlock1);
        assertEquals(1L, this.blockChainManager.getLastIndex());

        this.ebftBlockChain.addBlock(ebftBlock2);
        assertEquals(2L, this.blockChainManager.getLastIndex());

        this.ebftBlockChain.addBlock(ebftBlock3);
        assertEquals(3L, this.blockChainManager.getLastIndex());
    }

}


