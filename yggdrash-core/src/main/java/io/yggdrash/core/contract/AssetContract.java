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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AssetContract extends BaseContract<JsonArray> {

    private static final Logger log = LoggerFactory.getLogger(AssetContract.class);

    protected TransactionReceiptStore txReceiptStore;
    protected StateStore state;

    @Override
    public void init(StateStore stateStore, TransactionReceiptStore txReceiptStore) {
        this.state = stateStore;
        this.txReceiptStore = txReceiptStore;
    }

    @Override
    public boolean invoke(TransactionHusk txHusk) {
        TransactionReceipt txReceipt;
        try {
            this.sender = txHusk.getAddress().toString();
            JsonObject txBody = Utils.parseJsonArray(txHusk.getBody()).get(0).getAsJsonObject();

            dataFormatValidation(txBody);

            String method = txBody.get("method").getAsString();
            JsonArray params = txBody.get("params").getAsJsonArray();

            txReceipt = (TransactionReceipt) this.getClass()
                    .getMethod(method, JsonArray.class)
                    .invoke(this, params);
            txReceipt.putLog("method", method);
            txReceipt.setTransactionHash(txHusk.getHash().toString());
            txReceiptStore.put(txReceipt.getTransactionHash(), txReceipt);
        } catch (Throwable e) {
            txReceipt = new TransactionReceipt();
            txReceipt.setTransactionHash(txHusk.getHash().toString());
            txReceipt.setStatus(0);
            txReceipt.putLog("Error", e);
            txReceiptStore.put(txHusk.getHash().toString(), txReceipt);
        }
        return txReceipt.isSuccess();
    }

    public TransactionReceipt createDatabase(JsonArray params) {
        log.debug("createDatabase :: params => " + params);

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.setStatus(TransactionReceipt.SUCCESS);

        for (JsonElement element : params) {
            String dbName = element.getAsJsonObject().get("db").getAsString();
            if (dbName != null && !dbName.equals("")) {
                if (state.get(dbName) == null) {
                    state.put(dbName, new JsonArray());
                    txReceipt.putLog("createDatabase", dbName);
                } else {
                    txReceipt.setStatus(TransactionReceipt.FALSE);
                    break;
                }
            } else {
                txReceipt.setStatus(TransactionReceipt.FALSE);
                break;
            }
        }

        log.debug(txReceipt.toString());

        return txReceipt;
    }

    public TransactionReceipt createTable(JsonArray params) {
        log.debug("createTable :: params => " + params);

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.setStatus(TransactionReceipt.SUCCESS);

        for (JsonElement element : params) {
            String dbName = element.getAsJsonObject().get("db").getAsString();
            if (dbName != null && !dbName.equals("")) {
                if (state.get(dbName) == null) {
                    state.put(dbName, new JsonArray());
                    txReceipt.putLog("createTable", dbName);
                } else {
                    txReceipt.setStatus(TransactionReceipt.FALSE);
                    break;
                }
            } else {
                txReceipt.setStatus(TransactionReceipt.FALSE);
                break;
            }
        }

        log.debug(txReceipt.toString());

        return txReceipt;
    }

    private void dataFormatValidation(JsonObject data) {
        if (data.get("method").getAsString().length() < 0) {
            throw new FailedOperationException("Empty method");
        }
        if (!data.get("params").isJsonArray()) {
            throw new FailedOperationException("Params must be JsonArray");
        }
    }


    @Override
    public JsonObject query(JsonObject query) {
        return new JsonObject();
    }




}