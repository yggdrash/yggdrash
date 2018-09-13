/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.config.Constants;
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.contract.ContractTx;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BlockSignature;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.Proto;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.FileUtil;
import io.yggdrash.util.TimeUtils;
import org.spongycastle.util.encoders.Hex;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestUtils {
    public static Wallet wallet;
    public static final byte[] STEM_CHAIN =
            Hex.decode("fe7b7c93dd23f78e12ad42650595bc0f874c88f7");
    public static final byte[] TRANSFER_TO =
            Hex.decode("e1980adeafbb9ac6c9be60955484ab1547ab0b76");
    public static final BranchId STEM_BRANCH_ID = BranchId.of(STEM_CHAIN);

    private TestUtils() {}

    static {
        try {
            wallet = new Wallet();
        } catch (Exception e) {
            throw new InvalidSignatureException(e);
        }
    }

    public static Proto.Transaction createDummyTx() {
        String body = "[\"dummy\"]";
        return Proto.Transaction.newBuilder()
                .setHeader(Proto.Transaction.Header.newBuilder()
                    .setChain(ByteString.copyFrom(randomBytes(20)))
                    .setVersion(ByteString.copyFrom(randomBytes(8)))
                    .setType(ByteString.copyFrom(randomBytes(8)))
                    .setTimestamp(ByteString.copyFrom(
                            ByteUtil.longToBytes(TimeUtils.time())))
                    .setBodyHash(ByteString.copyFrom(HashUtil.sha3(body.getBytes())))
                    .setBodyLength(ByteString.copyFrom(randomBytes(8)))
                )
                .setSignature(ByteString.copyFrom(new byte[65]))
                .setBody(ByteString.copyFrom(body.getBytes()))
                .build();
    }

    public static TransactionHusk createInvalidTxHusk() {
        return new TransactionHusk(createDummyTx());
    }

    public static TransactionHusk createUnsignedTxHusk() {
        return new TransactionHusk(createDummyTx());
    }

    public static TransactionHusk createTxHusk() {
        return createTxHusk(wallet);
    }

    public static TransactionHusk createTxHusk(Wallet wallet) {
        return new TransactionHusk(TestUtils.sampleTx(wallet));
    }

    public static BlockHusk createGenesisBlockHusk() {
        return createGenesisBlockHusk(wallet);
    }

    public static BlockHusk createGenesisBlockHusk(Wallet wallet) {
        return BlockHusk.genesis(wallet, ContractTx.createStemTx(
                wallet, getSampleBranch1(), "create").toJsonObject());
    }

    public static BlockHusk createBlockHuskByTxList(Wallet wallet, List<TransactionHusk> txList) {
        return new BlockHusk(wallet, txList, createGenesisBlockHusk());
    }

    public static ObjectMapper getMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    private static byte[] randomBytes(int length) {
        byte[] result = new byte[length];
        new Random().nextBytes(result);
        return result;
    }

    public static Transaction sampleTx() {
        return new Transaction(ContractTx.createStemTx(
                wallet, getSampleBranch1(), "create").toJsonObject());
    }

    public static Transaction sampleTx(Wallet wallet) {
        return new Transaction(ContractTx.createStemTx(
                wallet, getSampleBranch1(), "create").toJsonObject());
    }

    public static Proto.Transaction[] sampleTxs() {
        return new Proto.Transaction[] { Transaction.toProtoTransaction(sampleTx()),
                Transaction.toProtoTransaction(sampleTx()),
                Transaction.toProtoTransaction(sampleTx())};
    }

    public static JsonObject sampleBlockObject() {

        List<Transaction> txs1 = new ArrayList<>();
        txs1.add(sampleTx());

        BlockBody blockBody = new BlockBody(txs1);

        long index = 0;
        long timestamp = TimeUtils.time();
        BlockHeader blockHeader = null;
        try {
            blockHeader = new BlockHeader(
                    STEM_CHAIN, new byte[8], new byte[8], new byte[32], index, timestamp,
                    blockBody.getMerkleRoot(), blockBody.length());

            BlockSignature blockSig = new BlockSignature(wallet, blockHeader.getHashForSignning());

            Block block = new Block(blockHeader, blockSig.getSignature(), blockBody);

            return block.toJsonObject();
        } catch (Exception e) {
            throw new NotValidateException();
        }
    }

    public static Block sampleBlock() {
        return new Block(sampleBlockObject());
    }

    public static Proto.Block[] sampleBlocks() {
        return new Proto.Block[] {sampleBlock().toProtoBlock(),
                sampleBlock().toProtoBlock(),
                sampleBlock().toProtoBlock()};
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
        String name = "STEM";
        String symbol = "STEM";
        String property = "ecosystem";
        String type = "immunity";
        String description = "The Basis of the YGGDRASH Ecosystem. It is also an aggregate and a blockchain containing information of all Branch Chains.";
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
        branch.addProperty("owner", "cee3d4755e47055b530deeba062c5bd0c17eb00f");
        branch.addProperty("symbol", symbol);
        branch.addProperty("property", property);
        branch.addProperty("type", type);
        branch.addProperty("timestamp", "0000016531dfa31c");
        branch.addProperty("description", description);
        branch.addProperty("tag", 0.1);
        branch.addProperty("version", version);
        branch.add("version_history", versionHistory);
        branch.addProperty("reference_address", referenceAddress);
        branch.addProperty("reserve_address", reserveAddress);

        return branch;
    }

    public static JsonObject updateBranch(
            String description, String version, JsonObject branch, Integer checkSum) {
        JsonObject updatedBranch = new JsonObject();
        updatedBranch.addProperty(
                "name", checkSum == 0 ? branch.get("name").getAsString() : "HELLO");
        updatedBranch.addProperty("owner", branch.get("owner").getAsString());
        updatedBranch.addProperty("symbol", branch.get("symbol").getAsString());
        updatedBranch.addProperty("property", branch.get("property").getAsString());
        updatedBranch.addProperty("type", branch.get("type").getAsString());
        updatedBranch.addProperty("timestamp", branch.get("timestamp").getAsString());
        updatedBranch.addProperty("description", description);
        updatedBranch.addProperty("tag", branch.get("tag").getAsFloat());
        updatedBranch.addProperty("version", version);
        updatedBranch.add("version_history", branch.get("version_history").getAsJsonArray());
        updatedBranch.addProperty(
                "reference_address", branch.get("reference_address").getAsString());
        updatedBranch.addProperty("reserve_address", branch.get("reserve_address").getAsString());

        return updatedBranch;
    }

    public static JsonObject createQuery(String method, JsonArray params) {
        JsonObject query = new JsonObject();
        query.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        query.addProperty("method", method);
        query.add("params", params);
        return query;
    }

    public static void clearTestDb() {
        String dbPath = new DefaultConfig().getConfig().getString(Constants.DATABASE_PATH);
        FileUtil.recursiveDelete(Paths.get(dbPath));
    }
}
