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

package io.yggdrash;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.FileUtil;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockChain;
import io.yggdrash.core.BlockChainBuilder;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BlockSignature;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.account.Wallet;
import io.yggdrash.core.contract.ContractTx;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.genesis.BranchJson;
import io.yggdrash.core.genesis.GenesisBlock;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.proto.Proto;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestUtils {
    public static final BranchId STEM = BranchId.of("b4e639d48bea1c26b7c72a9db94371c376779694");
    public static final BranchId YEED = BranchId.of("ba93ca9f4e0e71dd20bc3fc9b79e53df716a3f95");

    public static final String YGG_HOME = "testOutput";
    public static final String TRANSFER_TO = "e1980adeafbb9ac6c9be60955484ab1547ab0b76";
    private static final Wallet wallet;
    private static final GenesisBlock genesis;

    private TestUtils() {}

    static {
        ClassLoader loader = TestUtils.class.getClassLoader();
        try (InputStream is = loader.getResourceAsStream("branch-sample.json")) {
            wallet = new Wallet();

            BranchJson branchJson = BranchJson.toBranchJson(is);
            genesis = new GenesisBlock(branchJson);
        } catch (Exception e) {
            throw new InvalidSignatureException(e);
        }
    }

    public static Wallet wallet() {
        return wallet;
    }

    public static GenesisBlock genesis() {
        return genesis;
    }

    public static void clearTestDb() {
        String dbPath = new DefaultConfig().getDatabasePath();
        FileUtil.recursiveDelete(Paths.get(dbPath));
    }

    public static BlockChain createBlockChain(boolean isProductionMode) {
        StoreBuilder storeBuilder;
        if (isProductionMode) {
            storeBuilder = getProdMockBuilder();
        } else {
            storeBuilder = new StoreBuilder(new DefaultConfig());
        }
        return BlockChainBuilder.Builder()
                .addGenesis(genesis)
                .setStoreBuilder(storeBuilder)
                .build();
    }

    public static Proto.Block getBlockFixture() {
        return getBlockFixture(999L);
    }

    public static Proto.Block getBlockFixture(Long index) {
        return getBlockFixture(index,
                new Sha3Hash("9358888ca1ccd444ad11fb0ea1b5d03483f87664183c6e91ddab1b577cce2c06"));
    }

    public static Proto.Block getBlockFixture(Long index, Sha3Hash prevHash) {

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
            throw new NotValidateException(e);
        }
    }

    public static TransactionHusk createTransferTxHusk() {
        return new TransactionHusk(sampleTransferTx());
    }

    public static TransactionHusk createBranchTxHusk(Wallet wallet) {
        return new TransactionHusk(sampleCreateBranchTx(wallet));
    }

    public static BlockHusk createGenesisBlockHusk() {
        return new BlockHusk(sampleBlock().toProtoBlock());
    }

    public static BlockHusk createBlockHuskByTxList(Wallet wallet, List<TransactionHusk> txList) {
        return new BlockHusk(wallet, txList, createGenesisBlockHusk());
    }

    public static ObjectMapper getMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    public static byte[] randomBytes(int length) {
        byte[] result = new byte[length];
        new Random().nextBytes(result);
        return result;
    }

    public static Block sampleBlock() {
        return new Block(sampleBlockObject());
    }

    public static Proto.Block[] sampleBlocks() {
        return new Proto.Block[] {sampleBlock().toProtoBlock(),
                sampleBlock().toProtoBlock(),
                sampleBlock().toProtoBlock()};
    }

    public static Transaction sampleTransferTx() {
        JsonObject createYeedTxJson =
                ContractTx.createTx(YEED, wallet, TRANSFER_TO, 100).toJsonObject();
        return new Transaction(createYeedTxJson);
    }

    public static Proto.Transaction[] sampleTxs() {
        return new Proto.Transaction[] {Transaction.toProtoTransaction(sampleTransferTx()),
                Transaction.toProtoTransaction(sampleTransferTx()),
                Transaction.toProtoTransaction(sampleTransferTx())};
    }

    public static JsonObject getSampleBranch() {
        String name = "STEM";
        String symbol = "STEM";
        String property = "ecosystem";
        String type = "immunity";
        String description =
                "The Basis of the YGGDRASH Ecosystem. "
                        + "It is also an aggregate and a blockchain containing information "
                        + "of all Branch Chains.";
        String contractId = "d399cd6d34288d04ba9e68ddfda9f5fe99dd778e";
        return createBranch(name, symbol, property, type, description, contractId);
    }

    private static Transaction sampleCreateBranchTx(Wallet wallet) {
        TransactionHusk tx = ContractTx.createStemTx(wallet, getSampleBranch(), "create");
        return new Transaction(tx.toJsonObject());
    }

    public static JsonObject createBranch(String name,
                                          String symbol,
                                          String property,
                                          String type,
                                          String description,
                                          String contractId) {
        JsonObject branch = new JsonObject();
        branch.addProperty("name", name);
        branch.addProperty("symbol", symbol);
        branch.addProperty("property", property);
        branch.addProperty("type", type);
        branch.addProperty("description", description);
        branch.addProperty("contractId", contractId);
        branch.add("genesis", new JsonObject());
        branch.addProperty("timestamp", "00000166c837f0c9");
        BranchJson.signBranch(wallet, branch);
        return branch;
    }

    public static JsonObject updateBranch(String description, JsonObject branch,
                                          Integer checkSum) {
        JsonObject updatedBranch = new JsonObject();
        updatedBranch.addProperty(
                "name", checkSum == 0 ? branch.get("name").getAsString() : "HELLO");
        updatedBranch.addProperty("symbol", branch.get("symbol").getAsString());
        updatedBranch.addProperty("property", branch.get("property").getAsString());
        updatedBranch.addProperty("type", branch.get("type").getAsString());
        updatedBranch.addProperty("description", description);
        updatedBranch.addProperty("contractId", branch.get("contractId").getAsString());
        updatedBranch.add("genesis", branch.get("genesis"));
        updatedBranch.addProperty("timestamp", branch.get("timestamp").getAsString());
        updatedBranch.addProperty("owner", branch.get("owner").getAsString());
        updatedBranch.addProperty("signature", branch.get("signature").getAsString());
        return updatedBranch;
    }

    public static JsonObject createQuery(String method, JsonArray params) {
        JsonObject query = new JsonObject();
        query.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        query.addProperty("method", method);
        query.add("params", params);
        return query;
    }

    public static StoreBuilder getProdMockBuilder() {
        return new StoreBuilder(new ProdDefaultConfig());
    }

    private static JsonObject sampleBlockObject() {

        List<Transaction> txs1 = new ArrayList<>();
        txs1.add(sampleTransferTx());

        BlockBody blockBody = new BlockBody(txs1);

        long index = 0;
        long timestamp = TimeUtils.time();
        BlockHeader blockHeader;
        try {
            blockHeader = new BlockHeader(
                    STEM.getBytes(),
                    new byte[8], new byte[8], new byte[32], index, timestamp,
                    blockBody.getMerkleRoot(), blockBody.length());

            BlockSignature blockSig = new BlockSignature(wallet, blockHeader.getHashForSigning());

            Block block = new Block(blockHeader, blockSig.getSignature(), blockBody);

            return block.toJsonObject();
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    private static class ProdDefaultConfig extends DefaultConfig {
        ProdDefaultConfig() {
            super();
            this.productionMode = true;
        }
    }
}
