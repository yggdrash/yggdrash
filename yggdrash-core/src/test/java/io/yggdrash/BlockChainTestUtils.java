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
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockChainBuilder;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBody;
import io.yggdrash.core.blockchain.TransactionHeader;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.blockchain.TransactionSignature;
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
        return createBranchTxHusk(BranchId.of(json), "create", json);
    }

    public static TransactionHusk createBranchTxHusk(BranchId branchId, String method,
                                                     JsonObject branch) {

        JsonObject params = new JsonObject();
        params.add(branchId.toString(), branch);

        JsonArray txBody = ContractTestUtils.txBodyJson(method, params);
        return createTxHusk(TestConstants.STEM, txBody);
    }

    public static TransactionHusk createTxHusk(BranchId branchId, JsonArray txBody) {
        Transaction tx = createTx(TestConstants.wallet(), branchId, txBody);
        return new TransactionHusk(tx);
    }

    public static Transaction createTx(Wallet wallet, BranchId txBranchId, JsonArray body) {

        TransactionSignature txSig;
        Transaction tx;

        TransactionBody txBody;
        txBody = new TransactionBody(body);

        byte[] chain = txBranchId.getBytes();
        byte[] version = new byte[8];
        byte[] type = new byte[8];
        long timestamp = TimeUtils.time();

        TransactionHeader txHeader;
        txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);

        try {
            txSig = new TransactionSignature(wallet, txHeader.getHashForSigning());
            tx = new Transaction(txHeader, txSig.getSignature(), txBody);

            return tx;

        } catch (Exception e) {
            return null;
        }
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

    private static Transaction createTransferTx() {
        return createTransferTx(TestConstants.TRANSFER_TO, 100);
    }

    private static Transaction createTransferTx(String to, int amount) {
        JsonArray txBody = ContractTestUtils.transferTxBodyJson(to, amount);
        return createTx(wallet, TestConstants.YEED, txBody);
    }

}
