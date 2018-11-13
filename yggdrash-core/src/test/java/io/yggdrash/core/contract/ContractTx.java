/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionBody;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.TransactionSignature;
import io.yggdrash.core.account.Address;
import io.yggdrash.core.account.Wallet;
import io.yggdrash.core.genesis.BranchJson;

public class ContractTx {

    public static TransactionHusk createStemTxBySeed(BranchId stemBranchId, Wallet wallet,
                                                     JsonObject seed, String method) {
        if (!seed.has("genesis")) {
            seed.add("genesis", new JsonObject());
        }
        if (!seed.has("timestamp")) {
            seed.addProperty("timestamp", TimeUtils.hexTime());
        }
        BranchJson.signBranch(wallet, seed);
        BranchId branchId = BranchId.of(seed);

        return createTx(wallet, stemBranchId, createStemTxBody(branchId, seed, method));
    }

    public static TransactionHusk createStemTxByBranch(
            Wallet wallet, JsonObject branch, String method) {
        BranchId branchId = BranchId.of(branch);

        return createTx(wallet, branchId, createStemTxBody(branchId, branch, method));
    }

    public static TransactionHusk createYeedTx(BranchId branchId, Wallet wallet, Address to,
                                               long amount) {
        return createTx(wallet, branchId, createYeedTxBody(to, amount));
    }

    private static TransactionHusk createTx(Wallet wallet, BranchId txBranchId, JsonArray body) {
        return new TransactionHusk(txBodyJson(wallet, txBranchId, body));
    }

    private static JsonArray createStemParams(BranchId branchId, JsonObject branch) {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId.toString());
        param.add("branch", branch);
        params.add(param);

        return params;
    }

    private static JsonArray createStemTxBody(BranchId branchId, JsonObject branch, String method) {
        return txBodyJson(createStemParams(branchId, branch), method);
    }

    private static JsonArray createYeedTxBody(Address to, long amount) {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("address", to.toString());
        param.addProperty("amount", amount);
        params.add(param);

        return txBodyJson(params, "transfer");
    }

    private static JsonArray txBodyJson(JsonArray params, String method) {
        JsonObject txObj = new JsonObject();
        txObj.addProperty("method", method);
        txObj.add("params", params);

        JsonArray txBody = new JsonArray();
        txBody.add(txObj);

        return txBody;
    }

    private static JsonObject txBodyJson(Wallet wallet, BranchId txBranchId, JsonArray body) {

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

            return tx.toJsonObject();

        } catch (Exception e) {
            return null;
        }

    }
}
