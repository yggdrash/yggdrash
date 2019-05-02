package io.yggdrash.validator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.BlockImpl;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBody;
import io.yggdrash.core.blockchain.TransactionHeader;
import io.yggdrash.core.blockchain.TransactionImpl;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TestUtils {

    private final Wallet wallet;

    public TestUtils(Wallet wallet) {
        this.wallet = wallet;
    }

    private JsonObject sampleTxObject(Wallet newWallet) {
        return sampleTxObject(newWallet, new byte[20]);
    }

    private JsonObject sampleTxObject(Wallet newWallet, byte[] chain) {

        JsonArray params = new JsonArray();
        JsonObject param1 = new JsonObject();
        param1.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        JsonObject param2 = new JsonObject();
        param2.addProperty("amount", 100);
        params.add(param1);
        params.add(param2);

        JsonObject txObj = new JsonObject();
        txObj.addProperty("method", "transfer");
        txObj.add("params", params);

        return sampleTxObject(newWallet, txObj, chain);
    }

    private JsonObject sampleTxObject(Wallet newWallet, JsonObject body, byte[] chain) {

        Wallet nodeWallet;
        byte[] txSig;
        Transaction tx;

        if (newWallet == null) {
            nodeWallet = wallet;
        } else {
            nodeWallet = newWallet;
        }

        TransactionBody txBody = new TransactionBody(body);

        byte[] version = new byte[8];
        byte[] type = new byte[8];
        long timestamp = TimeUtils.time();

        TransactionHeader txHeader;
        txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);

        try {
            txSig = nodeWallet.sign(txHeader.getHashForSigning(), true);
            tx = new TransactionImpl(txHeader, txSig, txBody);

            return tx.toJsonObject();

        } catch (Exception e) {
            return null;
        }

    }

    public static String getBranchId(JsonObject branch) {
        return Hex.encodeHexString(getBranchHash(branch));
    }

    private static byte[] getBranchHash(JsonObject branch) {
        return HashUtil.sha3(getRawBranch(branch));
    }

    private static byte[] getRawBranch(JsonObject branch) {
        ByteArrayOutputStream branchStream = new ByteArrayOutputStream();
        try {
            branchStream.write(branch.get("name").getAsString().getBytes());
            branchStream.write(branch.get("property").getAsString().getBytes());
            branchStream.write(branch.get("type").getAsString().getBytes());
            branchStream.write(branch.get("timestamp").getAsString().getBytes());
            //branchStream.write(branch.get("version").getAsString().getBytes());
            branchStream.write(branch.get("versionHistory").getAsJsonArray().get(0)
                    .getAsString().getBytes());
            branchStream.write(branch.get("reference_address").getAsString().getBytes());
            branchStream.write(branch.get("reserve_address").getAsString().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return branchStream.toByteArray();
    }

    private Transaction sampleTx() {
        return new TransactionImpl(sampleTxObject(wallet));
    }

    private JsonObject sampleBlockObject(long index, byte[] prevBlockHash) {

        List<Transaction> txs1 = new ArrayList<>();
        txs1.add(sampleTx());

        BlockBody blockBody = new BlockBody(txs1);

        long timestamp = TimeUtils.time();
        BlockHeader blockHeader;
        try {
            blockHeader = new BlockHeader(
                    Constants.EMPTY_BRANCH,
                    Constants.EMPTY_BYTE8,
                    Constants.EMPTY_BYTE8,
                    prevBlockHash,
                    index,
                    timestamp,
                    blockBody.getMerkleRoot(), blockBody.getLength());

            byte[] blockSig = wallet.sign(blockHeader.getHashForSigning(), true);

            Block block = new BlockImpl(blockHeader, blockSig, blockBody);

            return block.toJsonObject();
        } catch (Exception e) {
            throw new NotValidateException();
        }
    }

    private JsonObject sampleBlockObject() {
        return sampleBlockObject(0L, Constants.EMPTY_HASH);
    }

    private JsonObject sampleBlockObject(long index) {
        return sampleBlockObject(index, Constants.EMPTY_HASH);
    }

    public Block sampleBlock() {
        return new BlockImpl(sampleBlockObject());
    }

    public Block sampleBlock(long index) {
        return new BlockImpl(sampleBlockObject(index));
    }

    public Block sampleBlock(long index, Sha3Hash prevBlockHash) {
        return sampleBlock(index, prevBlockHash.getBytes());
    }

    public Block sampleBlock(long index, byte[] prevBlockHash) {
        return new BlockImpl(sampleBlockObject(index, prevBlockHash));
    }

    public void clearTestDb() {
        String dbPath = new DefaultConfig().getDatabasePath();
        FileUtil.recursiveDelete(Paths.get(dbPath));
    }
}