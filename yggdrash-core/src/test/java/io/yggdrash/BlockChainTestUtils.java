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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockChainBuilder;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchEventListener;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBuilder;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.wallet.Wallet;
import java.io.InputStream;

public class BlockChainTestUtils {
    private static final GenesisBlock genesis;
    private static final Wallet wallet = TestConstants.wallet();

    private BlockChainTestUtils() {}

    static {
        ClassLoader loader = BlockChainTestUtils.class.getClassLoader();
        try (InputStream is = loader.getResourceAsStream("branch-sample.json")) {
            genesis = GenesisBlock.of(is);
        } catch (Exception e) {
            throw new InvalidSignatureException(e);
        }
    }

    public static BlockHusk genesisBlock() {
        return genesis.getBlock();
    }

    public static TransactionHusk createTransferTxHusk() {
        return new TransactionHusk(createTransferTx());
    }

    public static TransactionHusk createBranchTxHusk() {
        JsonObject json = ContractTestUtils.createSampleBranchJson();

        TransactionBuilder builder = new TransactionBuilder();
        return builder.addTxBody(Constants.STEM_CONTRACT_ID, "create", json)
                .setWallet(TestConstants.wallet())
                .setBranchId(TestConstants.STEM)
                .build();
    }

    public static TransactionHusk createBranchTxHusk(BranchId branchId, String method,
                                                     JsonObject branch) {
        JsonObject params = new JsonObject();
        params.add(branchId.toString(), branch);
        TransactionBuilder builder = new TransactionBuilder();
        return builder.addTxBody(Constants.STEM_CONTRACT_ID, method, params)
                .setWallet(TestConstants.wallet())
                .setBranchId(TestConstants.STEM)
                .build();
    }

    public static TransactionHusk createTxHusk(BranchId branchId, JsonArray txBody) {
        TransactionBuilder builder = new TransactionBuilder();
        return builder.addTransaction(txBody)
                .setWallet(TestConstants.wallet())
                .setBranchId(branchId)
                .build();
    }

    public static BlockChain createBlockChain(boolean isProductionMode) {
        StoreBuilder storeBuilder;
        if (isProductionMode) {
            storeBuilder = StoreTestUtils.getProdMockBuilder();
        } else {
            storeBuilder = new StoreBuilder(new DefaultConfig());
        }
        return BlockChainBuilder.Builder()
                .addGenesis(genesis)
                .setStoreBuilder(storeBuilder)
                .build();
    }

    public static BranchGroup createBranchGroup() {
        BranchGroup branchGroup = new BranchGroup();
        BlockChain blockChain = createBlockChain(false);
        branchGroup.addBranch(blockChain,
                new BranchEventListener() {
                    @Override
                    public void chainedBlock(BlockHusk block) {
                    }

                    @Override
                    public void receivedTransaction(TransactionHusk tx) {
                    }
                });
        return branchGroup;
    }

    private static Transaction createTransferTx() {
        return createTransferTx(TestConstants.TRANSFER_TO, 100);
    }

    private static Transaction createTransferTx(String to, int amount) {
        JsonArray txBody = ContractTestUtils.transferTxBodyJson(to, amount);
        TransactionBuilder builder = new TransactionBuilder();
        return builder.addTransaction(txBody)
                .setWallet(TestConstants.wallet())
                .setBranchId(TestConstants.STEM)
                .build()
                .getCoreTransaction()
                ;
    }

}
