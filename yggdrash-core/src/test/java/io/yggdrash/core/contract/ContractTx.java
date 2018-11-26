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
import io.yggdrash.TestUtils;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBody;
import io.yggdrash.core.blockchain.TransactionHeader;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.blockchain.TransactionSignature;
import io.yggdrash.core.blockchain.genesis.BranchJson;
import io.yggdrash.core.wallet.Wallet;

import java.math.BigDecimal;
import java.util.Map;

public class ContractTx {

    public static TransactionHusk createStemTx(Wallet wallet, JsonObject branch, String method) {
        if (!branch.has("genesis")) {
            branch.add("genesis", new JsonObject());
        }
        if (!branch.has("timestamp")) {
            branch.addProperty("timestamp", TimeUtils.hexTime());
        }
        BranchJson.signBranch(wallet, branch);
        BranchId branchId = BranchId.of(branch);
        JsonArray txBody = txBodyJson(createStemParams(branchId, branch), method);
        return createTx(wallet, TestUtils.STEM, txBody);
    }

    public static TransactionHusk createTx(Wallet wallet, BranchId txBranchId, JsonArray txBody) {
        return new TransactionHusk(txBodyJson(wallet, txBranchId, txBody));
    }

    @Deprecated
    public static TransactionHusk createTx(BranchId branchId, Wallet wallet, String to,
                                           long amount) {
        return createTx(wallet, branchId, createTxBody(to, amount));
    }

    @Deprecated
    private static JsonArray createTxBody(String to, long amount) {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("address", to);
        param.addProperty("amount", amount);
        params.add(param);

        return txBodyJson(params, "transfer");
    }

    public static JsonArray createTxBody(String method, Map<String, String> keyValue) {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        if (!keyValue.isEmpty()) {
            for (String key : keyValue.keySet()) {
                param.addProperty(key, keyValue.get(key));
            }
        }
        params.add(param);
        return txBodyJson(params, method);
    }

    static JsonArray createStemParams(BranchId branchId, JsonObject branch) {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.add(branchId.toString(), branch);
        params.add(param);

        return params;
    }

    public static JsonArray createTransferBody(String to, BigDecimal amount) {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("to", to);
        param.addProperty("amount", amount);
        params.add(param);

        return txBodyJson(params, "transfer");
    }

    public static JsonArray createApproveBody(String spender, BigDecimal amount) {
        JsonArray params = new JsonArray();
        JsonObject param = new JsonObject();
        param.addProperty("spender", spender);
        param.addProperty("amount", amount);
        params.add(param);

        return txBodyJson(params, "approve");
    }

    public static JsonArray createTransferFromBody(String from, String to, BigDecimal amount) {
        JsonObject param = new JsonObject();
        param.addProperty("from", from);
        param.addProperty("to", to);
        param.addProperty("amount", amount);

        JsonArray params = new JsonArray();
        params.add(param);

        return txBodyJson(params, "transferfrom");
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
