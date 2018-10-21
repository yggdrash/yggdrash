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

package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.contract.CoinContract;
import io.yggdrash.contract.Contract;
import io.yggdrash.contract.ContractClassLoader;
import io.yggdrash.contract.ContractMeta;
import io.yggdrash.contract.NoneContract;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.store.datasource.DbSource;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import io.yggdrash.util.TimeUtils;
import java.util.ArrayList;
import java.util.List;

public class BlockChainBuilder {

    public static BlockChain buildBlockChain(Wallet wallet, Branch branch, boolean isProduction)
            throws IllegalAccessException, InstantiationException {
        // TODO fix change builder patten ref : https://jdm.kr/blog/217
        BlockStore blockStore =
                new BlockStore(getDbSource(isProduction,branch.getBranchId() + "/blocks"));

        BlockHusk genesis;
        if (blockStore.size() > 0) {
            genesis = blockStore.get(0);
        } else {
            genesis = getGenesis(wallet, branch);
        }
        blockStore.close();
        return buildBlockChain(genesis, branch.getName(), isProduction);
    }

    public static BlockChain buildBlockChain(BlockHusk genesis, String branchName,
                                             boolean isProduction)
            throws InstantiationException, IllegalAccessException {
        // TODO fix blockchain by branch information (contract and other information)
        BlockStore blockStore =
                new BlockStore(getDbSource(isProduction,genesis.getBranchId() + "/blocks"));
        TransactionStore txStore =
                new TransactionStore(getDbSource(isProduction, genesis.getBranchId() + "/txs"));
        Contract contract = getContract(branchName);
        Runtime<?> runtime = getRunTime(branchName);

        BlockChain blockChain = new BlockChain(genesis, blockStore, txStore, contract, runtime);
        blockChain.setBranchName(branchName);
        return blockChain;
    }

    private static BlockHusk getGenesis(Wallet wallet, Branch branch) {

        if (!branch.isYeed()) {
            throw new FailedOperationException("Not supported name=" + branch.getName());
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("branchId", branch.getBranchId().toString());
        jsonObject.addProperty("method", "genesis");

        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("frontier", branch.getOwner());
        param.addProperty("balance", "1000000000");
        params.add(param);
        jsonObject.add("params", params);

        JsonArray jsonArrayTxBody = new JsonArray();
        jsonArrayTxBody.add(jsonObject);

        return genesis(wallet, branch.getBranchId(), jsonArrayTxBody);
    }

    private static BlockHusk genesis(Wallet wallet, BranchId branchId, JsonArray jsonArrayTxBody) {
        try {
            TransactionBody txBody = new TransactionBody(jsonArrayTxBody);

            TransactionHeader txHeader = new TransactionHeader(
                    branchId.getBytes(),
                    new byte[8],
                    new byte[8],
                    TimeUtils.time(),
                    txBody);

            Transaction tx = new Transaction(txHeader, wallet, txBody);
            List<Transaction> txList = new ArrayList<>();
            txList.add(tx);

            BlockBody blockBody = new BlockBody(txList);
            BlockHeader blockHeader = new BlockHeader(
                    branchId.getBytes(),
                    new byte[8],
                    new byte[8],
                    new byte[32],
                    0L,
                    0L,
                    blockBody.getMerkleRoot(),
                    blockBody.length());

            Block coreBlock = new Block(blockHeader, wallet, blockBody);

            return new BlockHusk(coreBlock.toProtoBlock());
        } catch (Exception e) {
            throw new NotValidateException();
        }
    }

    private static DbSource<byte[], byte[]> getDbSource(boolean isProduction, String path) {
        if (isProduction) {
            return new LevelDbDataSource(path);
        } else {
            return new HashMapDbSource();
        }
    }

    private static Contract getContract(String branchName)
            throws IllegalAccessException, InstantiationException {
        if (Branch.STEM.equalsIgnoreCase(branchName)) {
            // replace StemContract
            ContractMeta stem
                    = ContractClassLoader.loadContractById("5e793e345791e26c22498d6978ada9a2392b0692");
            return stem.getContract().newInstance();
        } else if (Branch.YEED.equalsIgnoreCase(branchName)) {
            return new CoinContract();
        } else {
            return new NoneContract();
        }
    }

    private static Runtime<?> getRunTime(String branchName) {
        if (Branch.STEM.equalsIgnoreCase(branchName)) {
            return getRunTime(JsonObject.class);
        } else if (Branch.YEED.equalsIgnoreCase(branchName)) {
            return getRunTime(Long.class);
        } else {
            return getRunTime(String.class);
        }
    }

    private static <T> Runtime<T> getRunTime(Class<T> clazz) {
        return new Runtime<>(new StateStore<>(), new TransactionReceiptStore());
    }
}
