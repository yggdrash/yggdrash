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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.contract.ContractVersion;
import io.yggdrash.core.wallet.Wallet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TransactionBuilder {
    BranchId branchId;
    Wallet wallet;
    List<JsonObject> txBody = new LinkedList<>();

    long timestamp = -1L;

    public TransactionBuilder setBranchId(BranchId branchId) {
        this.branchId = branchId;
        return this;
    }

    public TransactionBuilder setWallet(Wallet wallet) {
        this.wallet = wallet;
        return this;
    }

    public TransactionBuilder setTimeStamp(long timeStamp) {
        this.timestamp = timeStamp;
        return this;
    }

    public TransactionBuilder addTransactionBody(JsonObject txBody) {
        this.txBody.add(txBody);
        return this;
    }

    public TransactionBuilder addTransactionBody(JsonArray txBody) {
        Iterator<JsonElement> el = txBody.iterator();
        while (el.hasNext()) {
            JsonObject tx = el.next().getAsJsonObject();
            this.txBody.add(tx);
        }
        return this;
    }



    public TransactionBuilder addTxBody(ContractVersion contractVersion, String method, JsonObject params) {
        JsonObject txObj = new JsonObject();
        txObj.addProperty("contractVersion", contractVersion.toString());
        txObj.addProperty("method", method);
        txObj.add("params", params);
        return addTransactionBody(txObj);
    }

    private Transaction createTx(Wallet wallet, BranchId txBranchId, JsonArray body) {

        Transaction tx;

        TransactionBody txBody;
        txBody = new TransactionBody(body);

        byte[] chain = txBranchId.getBytes();
        byte[] version = new byte[8];
        byte[] type = new byte[8];
        // Check timeStamp
        if (timestamp == -1L) {
            timestamp = TimeUtils.time();
        }

        TransactionHeader txHeader;
        txHeader = new TransactionHeader(chain, version, type, timestamp, txBody);

        try {
            byte[] sign = new byte[]{};
            if (wallet != null) {
                TransactionSignature txSig;
                txSig = new TransactionSignature(wallet, txHeader.getHashForSigning());
                sign = txSig.getSignature();
            }

            tx = new Transaction(txHeader, sign, txBody);

            return tx;

        } catch (Exception e) {
            return null;
        }
    }

    public Transaction buildTransaction() {
        JsonArray txArray;
        if (branchId == null || txBody.size() == 0) {
            return  null;
        } else {

            txArray = new JsonArray();
            txBody.stream().forEach(tb -> txArray.add(tb));
        }
        return createTx(wallet, branchId, txArray);
    }

    public TransactionHusk build() {
        Transaction tx = buildTransaction();
        return new TransactionHusk(tx);
    }

}
