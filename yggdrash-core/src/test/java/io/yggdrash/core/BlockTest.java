package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.account.Wallet;
import io.yggdrash.core.genesis.GenesisBlock;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BlockTest {

    private static final Logger log = LoggerFactory.getLogger(BlockTest.class);

    private final byte[] chain = new byte[20];
    private final byte[] version = new byte[8];
    private final byte[] type = new byte[8];
    private final byte[] prevBlockHash = new byte[32];

    private final Wallet wallet = TestUtils.wallet();
    private Block block1;

    @Before
    public void init() throws Exception {
        JsonObject jsonParams1 = new JsonObject();
        jsonParams1.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2000");
        jsonParams1.addProperty("amount", "10000000");

        JsonObject jsonObject1 = new JsonObject();
        jsonObject1.addProperty("method", "transfer");
        jsonObject1.add("params", jsonParams1);

        JsonObject jsonParams2 = new JsonObject();
        jsonParams2.addProperty("address", "5db10750e8caff27f906b41c71b3471057dd2001");
        jsonParams2.addProperty("amount", "5000000");

        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("method", "transfer");
        jsonObject2.add("params", jsonParams2);

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(jsonObject1);
        jsonArray.add(jsonObject2);

        TransactionBody txBody = new TransactionBody(jsonArray);

        long timestamp = TimeUtils.time();
        TransactionHeader txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);
        TransactionSignature txSig = new TransactionSignature(wallet, txHeader.getHashForSigning());

        Transaction tx1 = new Transaction(txHeader, txSig.getSignature(), txBody);
        Transaction tx2 = tx1.clone();

        List<Transaction> txs1 = new ArrayList<>();
        txs1.add(tx1);
        txs1.add(tx2);

        log.debug("txs=" + txs1.toString());

        BlockBody blockBody1 = new BlockBody(txs1);

        timestamp = TimeUtils.time();
        long index = 0;
        BlockHeader blockHeader = new BlockHeader(
                chain, version, type, prevBlockHash, index, timestamp,
                blockBody1.getMerkleRoot(), blockBody1.length());

        log.debug(blockHeader.toString());

        BlockSignature blockSig = new BlockSignature(wallet, blockHeader.getHashForSigning());
        block1 = new Block(blockHeader, blockSig.getSignature(), blockBody1);
    }

    @Test
    public void shouldBeLoadedBranchJsonFile() throws IOException {
        File genesisFile = new File(Objects.requireNonNull(getClass().getClassLoader()
                .getResource("branch-sample.json")).getFile());

        GenesisBlock genesisBlock = new GenesisBlock(new FileInputStream(genesisFile));
        Assertions.assertThat(genesisBlock.getBlock()).isNotNull();
        Assertions.assertThat(genesisBlock.getBlock().getIndex()).isEqualTo(0);
    }

    @Test
    public void testBlockConstructor() throws Exception {
        BlockHeader blockHeader2 = block1.getHeader().clone();
        BlockBody blockBody2 = block1.getBody().clone();
        BlockSignature blockSig2 = new BlockSignature(wallet, blockHeader2.getHashForSigning());
        Block block2 = new Block(blockHeader2, blockSig2.getSignature(), blockBody2);

        assertTrue(block2.verify());
        assertEquals(block1.toJsonObject(), block2.toJsonObject());

        Block block3 = new Block(
                blockHeader2.clone(), wallet, block2.getBody().clone());

        assertTrue(block3.verify());
        assertEquals(block1.toJsonObject(), block3.toJsonObject());

        Block block4 = new Block(block1.toJsonObject());
        assertTrue(block4.verify());
        assertEquals(block1.toJsonObject().toString(), block4.toJsonObject().toString());
    }

    @Test
    public void testBlockClone() throws Exception {
        Block block2 = block1.clone();
        log.debug("block2=" + block2.toJsonObject());

        assertEquals(block1.getHashHexString(), block2.getHashHexString());
        assertEquals(block1.toJsonObject().toString(), block2.toJsonObject().toString());
        assertArrayEquals(block1.getSignature(), block2.getSignature());
    }

    @Test
    public void testBlockKey() throws Exception {
        Block block2 = block1.clone();
        log.debug("block2 pubKey=" + block2.getPubKeyHexString());

        assertEquals(block1.getPubKeyHexString(), block2.getPubKeyHexString());
        assertArrayEquals(block1.getPubKey(), block2.getPubKey());
        assertArrayEquals(block1.getPubKey(), wallet.getPubicKey());

        log.debug("block1 author address=" + block1.getAddressHexString());
        log.debug("block2 author address=" + block2.getAddressHexString());
        log.debug("wallet address=" + wallet.getHexAddress());
        assertEquals(block1.getAddressHexString(), block2.getAddressHexString());
        assertEquals(block1.getAddressHexString(), wallet.getHexAddress());
        assertTrue(block1.verify());
        assertTrue(block2.verify());
    }
}
