package io.yggdrash.validator.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.util.FileUtil;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBody;
import io.yggdrash.core.blockchain.TransactionHeader;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.Proto;
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestUtils {
    public static final String YGG_HOME = "testOutput";

    private Wallet wallet;
    private static byte[] type =
            ByteBuffer.allocate(8).putInt(0).array();
    private static byte[] version =
            ByteBuffer.allocate(8).putInt(0).array();

    public TestUtils(Wallet wallet) {
        this.wallet = wallet;
    }

    public Proto.Transaction getTransactionFixture() {
        return Transaction.toProtoTransaction(new Transaction(sampleTxObject(null)));
    }

    public Proto.Transaction[] getTransactionFixtures() {
        return new Proto.Transaction[] {getTransactionFixture(), getTransactionFixture()};
    }

    public Proto.Block getBlockFixture() {
        return getBlockFixture(999L);
    }

    public Proto.Block getBlockFixture(Long index) {
        return getBlockFixture(index,
                new Sha3Hash("9358888ca1ccd444ad11fb0ea1b5d03483f87664183c6e91ddab1b577cce2c06"));
    }

    public Proto.Block getBlockFixture(Long index, Sha3Hash prevHash) {
        try {
            Block tmpBlock = sampleBlock();
            BlockHeader tmpBlockHeader = tmpBlock.getHeader();
            BlockBody tmpBlockBody = tmpBlock.getBody();

            BlockHeader newBlockHeader = new BlockHeader(
                    tmpBlockHeader.getChain(),
                    tmpBlockHeader.getVersion(),
                    tmpBlockHeader.getType(),
                    prevHash.getBytes(),
                    index,
                    TimeUtils.time(),
                    tmpBlockBody);

            return new Block(newBlockHeader, wallet, tmpBlockBody).toProtoBlock();
        } catch (Exception e) {
            throw new NotValidateException();
        }
    }

    public TransactionHusk createTxHusk() {
        return createTxHusk(wallet);
    }

    public TransactionHusk createTxHusk(Wallet wallet) {
        return new TransactionHusk(sampleTx(wallet));
    }

    public BlockHusk createGenesisBlockHusk() {
        return createGenesisBlockHusk(wallet);
    }

    public BlockHusk createGenesisBlockHusk(Wallet wallet) {
        try {
            JsonArray jsonArrayTxBody = new JsonArray();
            jsonArrayTxBody.add(sampleTxObject(null));

            TransactionBody txBody = new TransactionBody(jsonArrayTxBody);
            TransactionHeader txHeader = new TransactionHeader(
                    new byte[20],
                    new byte[8],
                    new byte[8],
                    TimeUtils.time(),
                    txBody);

            Transaction tx = new Transaction(txHeader, wallet, txBody);
            List<Transaction> txList = new ArrayList<>();
            txList.add(tx);

            BlockBody blockBody = new BlockBody(txList);
            BlockHeader blockHeader = new BlockHeader(
                    HashUtil.sha3omit12(txBody.getBodyHash()),
                    new byte[8],
                    new byte[8],
                    new byte[32],
                    0L,
                    0L,
                    blockBody.getMerkleRoot(),
                    blockBody.length());

            Block coreBlock = new Block(blockHeader, wallet, blockBody);

            return new BlockHusk(Block.toProtoBlock(coreBlock));
        } catch (Exception e) {
            throw new NotValidateException();
        }
    }

    public BlockHusk createBlockHuskByTxList(Wallet wallet, List<TransactionHusk> txList) {
        return new BlockHusk(wallet, txList, createGenesisBlockHusk());
    }

    public static byte[] randomBytes(int length) {
        byte[] result = new byte[length];
        new Random().nextBytes(result);
        return result;
    }

    public JsonObject sampleTxObject(Wallet newWallet) {
        return sampleTxObject(newWallet, new byte[20]);
    }

    public JsonObject sampleTxObject(Wallet newWallet, byte[] chain) {

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

    public JsonObject sampleTxObject(Wallet newWallet, JsonObject body) {
        return sampleTxObject(newWallet, body, new byte[20]);
    }

    public JsonObject sampleTxObject(Wallet newWallet, JsonObject body, byte[] chain) {

        Wallet nodeWallet;
        byte[] txSig;
        Transaction tx;

        if (newWallet == null) {
            nodeWallet = wallet;
        } else {
            nodeWallet = newWallet;
        }

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(body);

        TransactionBody txBody;
        txBody = new TransactionBody(jsonArray);

        byte[] version = new byte[8];
        byte[] type = new byte[8];
        long timestamp = TimeUtils.time();

        TransactionHeader txHeader;
        txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);

        try {
            txSig = nodeWallet.signHashedData(txHeader.getHashForSigning());
            tx = new Transaction(txHeader, txSig, txBody);

            return tx.toJsonObject();

        } catch (Exception e) {
            return null;
        }

    }

    public static JsonObject sampleBalanceOfQueryJson() {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        params.add(param);

        JsonObject query = new JsonObject();
        query.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        query.addProperty("method", "balanceOf");
        query.add("params", params);
        return query;
    }

    public static JsonObject getSampleBranch1() {
        String name = "TEST1";
        String symbol = "TEST1";
        String property = "dex";
        String type = "immunity";
        String description = "TEST1";
        String version = "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76";
        String referenceAddress = "";
        String reserveAddress = "0x2G5f8A319550f80f9D362ab2eE0D1f023EC665a3";
        return createBranch(name, symbol, property, type, description,
                version, referenceAddress, reserveAddress);
    }

    public static JsonObject getSampleBranch2() {
        String name = "TEST2";
        String symbol = "TEST2";
        String property = "exchange";
        String type = "mutable";
        String description = "TEST2";
        String version = "0xe4452ervbo091qw4f5n2s8799232abr213er2c90";
        String referenceAddress = "";
        String reserveAddress = "0x2G5f8A319550f80f9D362ab2eE0D1f023EC665a3";
        return createBranch(name, symbol, property, type, description,
                version, referenceAddress, reserveAddress);
    }

    public static JsonObject getSampleBranch3(String branchId) {
        String name = "Ethereum TO YEED";
        String symbol = "ETH TO YEED";
        String property = "exchange";
        String type = "immunity";
        String description = "ETH TO YEED";
        String version = "0xb5790adeafbb9ac6c9be60955484ab1547ab0b76";
        String referenceAddress = branchId;
        String reserveAddress = "0x1F8f8A219550f89f9D372ab2eE0D1f023EC665a3";
        return createBranch(name, symbol, property, type, description,
                version, referenceAddress, reserveAddress);
    }

    private static JsonObject createBranch(String name,
                                           String symbol,
                                           String property,
                                           String type,
                                           String description,
                                           String version,
                                           String referenceAddress,
                                           String reserveAddress) {
        JsonArray versionHistory = new JsonArray();
        versionHistory.add(version);
        JsonObject branch = new JsonObject();
        branch.addProperty("name", name);
        //branch.addProperty("owner", wallet.getHexAddress());
        branch.addProperty("owner", "9e187f5264037ab77c87fcffcecd943702cd72c3");
        branch.addProperty("symbol", symbol);
        branch.addProperty("property", property);
        branch.addProperty("type", type);
        branch.addProperty("timestamp", "0000016531dfa31c");
        branch.addProperty("description", description);
        branch.addProperty("tag", 0.1);
        branch.addProperty("version", version);
        branch.add("versionHistory", versionHistory);
        branch.addProperty("reference_address", referenceAddress);
        branch.addProperty("reserve_address", reserveAddress);

        return branch;
    }

    public static JsonObject updateBranch(String description,
                                          String version, JsonObject branch, Integer checkSum) {
        JsonObject updatedBranch = new JsonObject();
        updatedBranch.addProperty("name",
                checkSum == 0 ? branch.get("name").getAsString() : "HELLO");
        updatedBranch.addProperty("owner", branch.get("owner").getAsString());
        updatedBranch.addProperty("symbol", branch.get("symbol").getAsString());
        updatedBranch.addProperty("property", branch.get("property").getAsString());
        updatedBranch.addProperty("type", branch.get("type").getAsString());
        updatedBranch.addProperty("timestamp", branch.get("timestamp").getAsString());
        updatedBranch.addProperty("description", description);
        updatedBranch.addProperty("tag", branch.get("tag").getAsFloat());
        updatedBranch.addProperty("version", version);
        updatedBranch.add("versionHistory", branch.get("versionHistory").getAsJsonArray());
        updatedBranch.addProperty("reference_address",
                branch.get("reference_address").getAsString());
        updatedBranch.addProperty("reserve_address",
                branch.get("reserve_address").getAsString());

        return updatedBranch;
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

    public static JsonObject createQuery(String method, JsonArray params) {
        JsonObject query = new JsonObject();
        query.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        query.addProperty("method", method);
        query.add("params", params);
        return query;
    }

    public Transaction sampleTx() {
        return new Transaction(sampleTxObject(wallet));
    }

    public Transaction sampleTx(byte[] chain) {
        return new Transaction(sampleTxObject(wallet, chain));
    }

    public Transaction sampleTx(JsonObject body) {
        return new Transaction(sampleTxObject(wallet));
    }

    public Transaction sampleTx(Wallet wallet) {
        return new Transaction(sampleTxObject(wallet));
    }

    public JsonObject sampleBlockObject(long index, byte[] prevBlockHash) {

        List<Transaction> txs1 = new ArrayList<>();
        txs1.add(sampleTx());

        BlockBody blockBody = new BlockBody(txs1);

        long timestamp = TimeUtils.time();
        BlockHeader blockHeader;
        try {
            blockHeader = new BlockHeader(
                    new byte[20], new byte[8], new byte[8], prevBlockHash, index, timestamp,
                    blockBody.getMerkleRoot(), blockBody.length());

            byte[] blockSig = wallet.signHashedData(blockHeader.getHashForSigning());

            Block block = new Block(blockHeader, blockSig, blockBody);

            return block.toJsonObject();
        } catch (Exception e) {
            throw new NotValidateException();
        }
    }

    public JsonObject sampleBlockObject() {

        List<Transaction> txs1 = new ArrayList<>();
        txs1.add(sampleTx());

        BlockBody blockBody = new BlockBody(txs1);

        long index = 0;
        long timestamp = TimeUtils.time();
        BlockHeader blockHeader;
        try {
            blockHeader = new BlockHeader(
                    new byte[20], new byte[8], new byte[8], new byte[32], index, timestamp,
                    blockBody.getMerkleRoot(), blockBody.length());

            byte[] blockSig = wallet.signHashedData(blockHeader.getHashForSigning());

            Block block = new Block(blockHeader, blockSig, blockBody);

            return block.toJsonObject();
        } catch (Exception e) {
            throw new NotValidateException();
        }
    }

    public Block sampleBlock() {
        return new Block(sampleBlockObject());
    }

    public Block sampleBlock(long index, byte[] prevBlockHash) {
        return new Block(sampleBlockObject(index, prevBlockHash));
    }

    public Proto.Transaction sampleProtoTx() {
        return Transaction.toProtoTransaction(sampleTx());
    }

    public Proto.Block[] getBlockFixtures() {
        return new Proto.Block[] {getBlockFixture(), getBlockFixture(), getBlockFixture()};
    }

    public void clearTestDb() {
        String dbPath = new DefaultConfig().getDatabasePath();
        FileUtil.recursiveDelete(Paths.get(dbPath));
    }
}