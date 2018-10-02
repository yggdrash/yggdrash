package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.util.TimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BlockHeaderTest {

    private static final Logger log = LoggerFactory.getLogger(BlockHeaderTest.class);

    private final byte[] chain = new byte[20];
    private final byte[] version = new byte[8];
    private final byte[] type = new byte[8];
    private final byte[] prevBlockHash = new byte[32];
    private final long index = 0;
    private final long timestamp = TimeUtils.time();

    private BlockBody blockBody1;

    private BlockHeader blockHeader1;
    private BlockHeader blockHeader2;


    @Before
    public void init() throws Exception {

        TransactionBody txBody;
        TransactionHeader txHeader;
        Wallet wallet;
        TransactionSignature txSig;
        Transaction tx1;
        Transaction tx2;
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

        txBody = new TransactionBody(jsonArray);

        txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);

        wallet = new Wallet();

        txSig = new TransactionSignature(wallet, txHeader.getHashForSigning());

        tx1 = new Transaction(txHeader, txSig, txBody);

        tx2 = tx1.clone();

        List<Transaction> txs1 = new ArrayList<>();
        txs1.add(tx1);
        txs1.add(tx2);

        log.debug("txs=" + txs1.toString());

        blockBody1 = new BlockBody(txs1);

    }

    @Test
    public void testBlockHeader() throws Exception {
        blockHeader1 = new BlockHeader(
                chain, version, type, prevBlockHash, index, timestamp,
                blockBody1.getMerkleRoot(), blockBody1.length());

        log.debug(blockHeader1.toString());

        blockHeader2 = blockHeader1.clone();

        log.debug(blockHeader2.toString());

        assertEquals(blockHeader1.toJsonObject(), blockHeader2.toJsonObject());

        assertArrayEquals(blockHeader1.getHashForSigning(), blockHeader2.getHashForSigning());
    }

    @Test
    public void testBlockHeaderClone() throws Exception {
        blockHeader1 = new BlockHeader(
                chain, version, type, prevBlockHash, index, timestamp,
                blockBody1.getMerkleRoot(), blockBody1.length());

        blockHeader2 = blockHeader1.clone();
        log.debug("blockHeader1=" + blockHeader1.toJsonObject());
        log.debug("blockHeader2=" + blockHeader2.toJsonObject());
        assertEquals(blockHeader1.toJsonObject(), blockHeader2.toJsonObject());
    }

}
