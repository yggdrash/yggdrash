/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.blockchain;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.contract.ContractId;
import io.yggdrash.core.wallet.Wallet;
import java.util.LinkedList;
import java.util.List;

public class TransactionBuilder {
    BranchId branchId;
    Wallet wallet;
    List<JsonObject> txBody = new LinkedList<>();

    public TransactionBuilder setBranchId(BranchId branchId) {
        this.branchId = branchId;
        return this;
    }

    public TransactionBuilder setWalet(Wallet walet) {
        this.wallet = walet;
        return this;
    }

    public TransactionBuilder addTransaction(JsonObject txBody) {
        this.txBody.add(txBody);
        return this;
    }

    public TransactionBuilder addTxBody(ContractId contractId, String method, JsonObject params) {
        JsonObject txObj = new JsonObject();
        txObj.addProperty("contractId", contractId.toString());
        txObj.addProperty("method", method);
        txObj.add("params", params);
        return addTransaction(txObj);
    }

    private Transaction createTx(Wallet wallet, BranchId txBranchId, JsonArray body) {

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

    public TransactionHusk build() {
        JsonArray txArray;
        if (branchId == null || txBody.size() == 0) {
            return  null;
        } else {

            txArray = new JsonArray();
            txBody.stream().forEach(tb -> txArray.add(tb));
        }
        Transaction tx = createTx(wallet, branchId, txArray);
        return new TransactionHusk(tx);
    }

}
