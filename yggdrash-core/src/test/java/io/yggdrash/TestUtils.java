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
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockChainBuilder;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BlockSignature;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.contract.ContractTx;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.Proto;
import org.spongycastle.util.encoders.Hex;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestUtils {
    public static final BranchId STEM = BranchId.of("91b29a1453258d72ca6fbbcabb8dca10cca944fb");
    public static final BranchId YEED = BranchId.of("d872b5a338b824dc56abc6015543496670d81c1b");

    public static final String YGG_HOME = "testOutput";
    public static final String TRANSFER_TO = "e1980adeafbb9ac6c9be60955484ab1547ab0b76";
    private static final Wallet wallet;
    private static final GenesisBlock genesis;

    private TestUtils() {}

    static {
        ClassLoader loader = TestUtils.class.getClassLoader();
        try (InputStream is = loader.getResourceAsStream("branch-sample.json")) {
            wallet = new Wallet();
            genesis = GenesisBlock.of(is);
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
        return sampleTransferTx(100);
    }

    public static Transaction sampleTransferTx(int amount) {
        JsonObject createYeedTxJson =
                ContractTx.createTx(YEED, wallet, TRANSFER_TO, amount).toJsonObject();
        return new Transaction(createYeedTxJson);
    }

    public static Proto.Transaction[] sampleTxs() {
        return new Proto.Transaction[] {Transaction.toProtoTransaction(sampleTransferTx()),
                Transaction.toProtoTransaction(sampleTransferTx()),
                Transaction.toProtoTransaction(sampleTransferTx())};
    }

    public static JsonObject createSampleBranchJson() {
        String description =
                "The Basis of the YGGDRASH Ecosystem. "
                        + "It is also an aggregate and a blockchain containing information "
                        + "of all Branch Chains.";
        return createSampleBranchJson(description);
    }

    public static JsonObject createSampleBranchJson(String description) {
        String name = "STEM";
        String symbol = "STEM";
        String property = "ecosystem";
        String contractId = "d399cd6d34288d04ba9e68ddfda9f5fe99dd778e";
        return createBranchJson(name, symbol, property, description, contractId, null);
    }

    private static Transaction sampleCreateBranchTx(Wallet wallet) {
        TransactionHusk tx = ContractTx.createStemTx(wallet, createSampleBranchJson(), "create");
        return new Transaction(tx.toJsonObject());
    }

    public static JsonObject createBranchJson(String name,
                                              String symbol,
                                              String property,
                                              String description,
                                              String contractId,
                                              String timestamp) {
        JsonObject branch = new JsonObject();
        branch.addProperty("name", name);
        branch.addProperty("symbol", symbol);
        branch.addProperty("property", property);
        branch.addProperty("description", description);
        branch.addProperty("contractId", contractId);
        branch.add("genesis", new JsonObject());
        if (timestamp == null) {
            branch.addProperty("timestamp", "00000166c837f0c9");
        } else {
            branch.addProperty("timestamp", timestamp);
        }
        signBranch(wallet, branch);
        return branch;
    }

    public static JsonObject createQuery(String method, JsonArray params) {
        JsonObject query = new JsonObject();
        query.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        query.addProperty("method", method);
        query.add("params", params);
        return query;
    }

    public static JsonObject signBranch(Wallet wallet, JsonObject raw) {
        if (!raw.has("signature")) {
            raw.addProperty("owner", wallet.getHexAddress());
            Sha3Hash hashForSign = new Sha3Hash(raw.toString().getBytes(StandardCharsets.UTF_8));
            byte[] signature = wallet.signHashedData(hashForSign.getBytes());
            raw.addProperty("signature", Hex.toHexString(signature));
        }
        return raw;
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
