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

import com.google.gson.JsonObject;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.wallet.Wallet;

public class TransactionBuilder {
    private BranchId branchId;
    private Wallet wallet;
    private JsonObject txBody = new JsonObject(); //TODO Change modifier to final
    private byte[] version = Constants.EMPTY_BYTE8;
    private byte[] type = Constants.EMPTY_BYTE8;

    private long timestamp = -1L;

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

    public TransactionBuilder setVersion(byte[] version) {
        this.version = version;
        return this;
    }

    public TransactionBuilder setType(byte[] type) {
        this.type = type;
        return this;
    }


    public TransactionBuilder setTxBody(JsonObject txBody) {
        this.txBody = txBody;
        return this;
    }

    public TransactionBuilder setTxBody(ContractVersion contractVersion, String method,
                                        JsonObject params, boolean isSystem) {
        return setTxBody(commonTxBody(contractVersion.toString(), method, params, isSystem));
    }

    @Deprecated
    public TransactionBuilder setTxBody(ContractVersion contractVersion, String method,
                                        JsonObject params, boolean isSystem, JsonObject consensus) {
        // TODO Consensus information get State Store v0.4.0
        JsonObject txObj = commonTxBody(contractVersion.toString(), method, params, isSystem);
        txObj.add("consensus", consensus);
        return setTxBody(txObj);
    }

    private JsonObject commonTxBody(String contractVersion, String method, JsonObject params, boolean isSystem) {
        JsonObject txObj = new JsonObject();
        txObj.addProperty("contractVersion", contractVersion);
        txObj.addProperty("method", method);
        txObj.add("params", params);
        txObj.addProperty("isSystem", isSystem);

        return txObj;
    }

    private Transaction createTx() {
        //Wallet wallet, byte[] version, byte[] type, BranchId txBranchId, JsonArray body

        TransactionBody transactionBody = new TransactionBody(txBody);

        byte[] chain = branchId.getBytes();
        // Check timeStamp
        if (timestamp == -1L) {
            timestamp = TimeUtils.time();
        }

        TransactionHeader txHeader = new TransactionHeader(chain, version, type, timestamp, transactionBody);

        try {
            byte[] sign = Constants.EMPTY_SIGNATURE;
            if (wallet != null) {
                sign = wallet.sign(txHeader.getHashForSigning(), true);
            }

            return new TransactionImpl(txHeader, sign, transactionBody);
        } catch (Exception e) {
            return null;
        }
    }

    public Transaction build() {
        if (branchId == null || txBody.size() == 0) {
            return  null;
        }
        return createTx();
    }

}
