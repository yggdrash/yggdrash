package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.util.TimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BlockSignatureTest {

    private static final Logger log = LoggerFactory.getLogger(BlockSignatureTest.class);

    private byte[] chain = new byte[20];
    private byte[] version = new byte[8];
    private byte[] type = new byte[8];
    private byte[] prevBlockHash = new byte[32];
    private long index = 0;
    private long timestamp = TimeUtils.time();

    private BlockBody blockBody1;

    private BlockHeader blockHeader1;
    private BlockHeader blockHeader2;

    private BlockSignature blockSig1;
    private BlockSignature blockSig2;

    private Wallet wallet;

    @Before
    public void init() {

        TransactionBody txBody;
        TransactionHeader txHeader;
        TransactionSignature txSig;
        Transaction tx1;
        Transaction tx2;

        try {

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

            txHeader.setTimestamp(TimeUtils.time());

            txSig = new TransactionSignature(wallet, txHeader.getHashForSignning());

            tx1 = new Transaction(txHeader, txSig, txBody);

            tx2 = tx1.clone();

            List<Transaction> txs1 = new ArrayList<>();
            txs1.add(tx1);
            txs1.add(tx2);

            log.debug("txs=" + txs1.toString());

            blockBody1 = new BlockBody(txs1);

            timestamp = TimeUtils.time() + 1;
            blockHeader1 = new BlockHeader(
                    chain, version, type, prevBlockHash, index, timestamp,
                    blockBody1.getMerkleRoot(), blockBody1.length());

            log.debug(blockHeader1.toString());

            blockHeader2 = blockHeader1.clone();

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }

    }

    @Test
    public void testBlockSignature() {

        try {
            blockSig1 = new BlockSignature(wallet, blockHeader1.getHashForSignning());
            assertArrayEquals(blockSig1.getHeaderHash(), blockHeader1.getHashForSignning());

            log.debug("blockSig1.signature=" + Hex.toHexString(blockSig1.getSignature()));
            log.debug("blockSig1.headerHash=" + Hex.toHexString(blockSig1.getHeaderHash()));
            log.debug("blockSig1.ecKeyPub="
                    + Hex.toHexString(blockSig1.getEcKeyPub().getPubKey()));

            blockSig2 = new BlockSignature(blockSig1.getSignature(), blockSig1.getHeaderHash());

            log.debug("blockSig2.signature=" + Hex.toHexString(blockSig2.getSignature()));
            log.debug("blockSig2.headerHash=" + Hex.toHexString(blockSig2.getHeaderHash()));
            log.debug("blockSig2.ecKeyPub="
                    + Hex.toHexString(blockSig2.getEcKeyPub().getPubKey()));

            assertArrayEquals(blockSig1.getEcdsaSignature().toBinary(),
                    blockSig2.getEcdsaSignature().toBinary());

            assertArrayEquals(blockSig1.getEcKeyPub().getPubKey(),
                    blockSig2.getEcKeyPub().getPubKey());

            BlockSignature blockSig3 = new BlockSignature(blockSig1.toJsonObject());
            assertEquals(blockSig1.toJsonObject().toString(), blockSig3.toJsonObject().toString());

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }
    }

    @Test
    public void testBlockSignatureClone() {

        try {
            blockSig1 = new BlockSignature(wallet, blockHeader1.getHashForSignning());

            log.debug("blockSig1.signature=" + Hex.toHexString(blockSig1.getSignature()));
            log.debug("blockSig1.headerHash=" + Hex.toHexString(blockSig1.getHeaderHash()));
            log.debug("blockSig1.ecKeyPub="
                    + Hex.toHexString(blockSig1.getEcKeyPub().getPubKey()));

            blockSig2 = blockSig1.clone();

            log.debug("blockSig2.signature=" + Hex.toHexString(blockSig2.getSignature()));
            log.debug("blockSig2.headerHash=" + Hex.toHexString(blockSig2.getHeaderHash()));
            log.debug("blockSig2.ecKeyPub="
                    + Hex.toHexString(blockSig2.getEcKeyPub().getPubKey()));

            assertArrayEquals(blockSig1.getSignature(), blockSig2.getSignature());

        } catch (Exception e) {
            log.debug(e.getMessage());
            assert false;
        }
    }
}
