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

import java.util.Collection;
import java.util.Map;

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
            if (dbName == null || dbName.equals("")) {
                txReceipt.setStatus(TransactionReceipt.FALSE);
                txReceipt.putLog("createDatabase", "This dbName is not valid.");
                break;
            } else {
                if (state.get(dbName) == null) {
                    state.put(dbName, new JsonArray());
                    txReceipt.putLog("createDatabase", dbName);
                } else {
                    txReceipt.setStatus(TransactionReceipt.FALSE);
                    txReceipt.putLog("createDatabase", "This dbName is already exist.");
                    break;
                }
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
            String tableName = element.getAsJsonObject().get("table").getAsString();
            JsonObject keyObject = element.getAsJsonObject().get("key").getAsJsonObject();
            JsonObject recordObject = element.getAsJsonObject().get("record").getAsJsonObject();

            if (dbName == null || dbName.equals("")
                    || tableName == null || tableName.equals("")
                    || keyObject == null || keyObject.size() == 0
                    || recordObject == null) {
                txReceipt.setStatus(TransactionReceipt.FALSE);
                txReceipt.putLog("createTable",
                        params.toString() + " This table is not valid.");
                break;
            } else {
                JsonObject tableObject = element.getAsJsonObject();
                tableObject.remove("db");

                for (JsonElement dbElement : (JsonArray)state.get(dbName)) {
                    if (dbElement.getAsJsonObject().get("table").getAsString().equals(tableName)) {
                        txReceipt.setStatus(TransactionReceipt.FALSE);
                        txReceipt.putLog("createTable",
                                params.toString() + " This table is already exist.");
                        break;
                    }
                }

                JsonArray stateArray = ((JsonArray) state.get(dbName)).getAsJsonArray();
                if (stateArray != null) {
                    stateArray.add(tableObject);
                    state.replace(dbName, stateArray);
                    txReceipt.putLog("createTable", tableObject);
                } else {
                    txReceipt.setStatus(TransactionReceipt.FALSE);
                    txReceipt.putLog("createTable",
                            params.toString() + " This table is already exist.");
                    break;
                }
            }
        }

        log.debug(txReceipt.toString());

        return txReceipt;
    }

    public TransactionReceipt insert(JsonArray params) {
        log.debug("insert :: params => " + params);

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.setStatus(TransactionReceipt.SUCCESS);

        for (JsonElement element : params) {
            String dbName = element.getAsJsonObject().get("db").getAsString();
            String tableName = element.getAsJsonObject().get("table").getAsString();
            JsonObject keyObject = element.getAsJsonObject().get("key").getAsJsonObject();
            JsonObject recordObject = element.getAsJsonObject().get("record").getAsJsonObject();

            if (dbName == null || dbName.equals("")
                    || tableName == null || tableName.equals("")
                    || keyObject == null || keyObject.size() == 0
                    || recordObject == null || recordObject.size() == 0) {
                txReceipt.setStatus(TransactionReceipt.FALSE);
                txReceipt.putLog("insert",
                        params.toString() + " This data is not valid.");
                break;
            }

            // check db & table
            JsonObject tableObject = null;
            for (JsonElement dbElement : (JsonArray) state.get(dbName)) {
                if (dbElement.getAsJsonObject().get("table").getAsString().equals(tableName)) {
                    tableObject = dbElement.getAsJsonObject();
                }
            }

            if (tableObject == null || !checkParams(tableObject, (JsonObject) element)) {
                txReceipt.setStatus(TransactionReceipt.FALSE);
                txReceipt.putLog("insert",
                        params.toString() + " This table is not valid.");
                break;
            }

            // insert record
            if (!state.putAssetState(dbName, tableName, keyObject, recordObject)) {
                txReceipt.setStatus(TransactionReceipt.FALSE);
                txReceipt.putLog("insert",
                        params.toString() + " This record is not valid.");
                break;
            }
        }

        log.debug(txReceipt.toString());

        return txReceipt;
    }

    public TransactionReceipt update(JsonArray params) {
        log.debug("update :: params => " + params);

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.setStatus(TransactionReceipt.SUCCESS);

        for (JsonElement element : params) {
            String dbName = element.getAsJsonObject().get("db").getAsString();
            String tableName = element.getAsJsonObject().get("table").getAsString();
            JsonObject keyObject = element.getAsJsonObject().get("key").getAsJsonObject();
            JsonObject recordObject = element.getAsJsonObject().get("record").getAsJsonObject();

            if (dbName == null || dbName.equals("")
                    || tableName == null || tableName.equals("")
                    || keyObject == null || keyObject.size() == 0
                    || recordObject == null || recordObject.size() == 0) {
                txReceipt.setStatus(TransactionReceipt.FALSE);
                txReceipt.putLog("update",
                        params.toString() + " This data is not valid.");
                break;
            }

            // check db & table
            JsonObject tableObject = null;
            for (JsonElement dbElement : (JsonArray) state.get(dbName)) {
                if (dbElement.getAsJsonObject().get("table").getAsString().equals(tableName)) {
                    tableObject = dbElement.getAsJsonObject();
                }
            }

            if (tableObject == null || !checkParams(tableObject, (JsonObject) element)) {
                txReceipt.setStatus(TransactionReceipt.FALSE);
                txReceipt.putLog("update",
                        params.toString() + " This table is not valid.");
                break;
            }

            // update record
            if (!state.updateAssetState(dbName, tableName, keyObject, recordObject)) {
                txReceipt.setStatus(TransactionReceipt.FALSE);
                txReceipt.putLog("update",
                        params.toString() + " This record is not valid.");
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

    private boolean checkParams(JsonObject table, JsonObject record) {

        String tableObjectName = table.get("table").getAsString();
        String recordObjectName = record.get("table").getAsString();

        if (!tableObjectName.equals(recordObjectName)) {
            return false;
        }

        JsonObject tableKeyObject = table.get("key").getAsJsonObject();
        JsonObject recordKeyObject = record.get("key").getAsJsonObject();

        // check key size
        if (tableKeyObject.size() != recordKeyObject.size()) {
            return false;
        }

        // check key
        for (Map.Entry<String, JsonElement> entry : tableKeyObject.entrySet()) {
            if (!recordKeyObject.has(entry.getKey())) {
                return false;
            }
        }

        JsonObject tableRecordObject = table.get("record").getAsJsonObject();
        JsonObject recordRecordObject = record.get("record").getAsJsonObject();

        // check record
        for (Map.Entry<String, JsonElement> entry : recordRecordObject.entrySet()) {
            if (!tableRecordObject.has(entry.getKey())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public JsonObject query(JsonObject query) {
        dataFormatValidation(query);

        String method = query.get("method").getAsString();
        JsonArray params = query.get("params").getAsJsonArray();

        JsonObject result = null;
        try {
            result = (JsonObject)getClass().getMethod(method, JsonArray.class).invoke(this, params);

        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
        return result;
    }

    public JsonObject queryAllDatabases(JsonArray params) {

        JsonObject result = new JsonObject();
        JsonArray dbArray = new JsonArray();

        for (Object dbName : state.getAllKey()) {
            dbArray.add(dbName.toString());
        }

        result.add("db", dbArray);

        return result;
    }

    public JsonObject queryAllTables(JsonArray params) {

        String db = params.get(0).getAsJsonObject().get("db").getAsString();

        JsonObject result = new JsonObject();
        JsonArray tableNames = new JsonArray();

        JsonArray tableStates = (JsonArray)state.get(db);

        for (JsonElement table : tableStates) {
            tableNames.add(table.getAsJsonObject().get("table").getAsString());
        }

        result.add("table", tableNames);

        return result;
    }

    public JsonObject queryTable(JsonArray params) {

        String dbName = params.get(0).getAsJsonObject().get("db").getAsString();
        String tableName = params.get(0).getAsJsonObject().get("table").getAsString();

        JsonObject result = null;

        JsonArray tableStates = (JsonArray)state.get(dbName);

        for (JsonElement table : tableStates) {
            if (table.getAsJsonObject().get("table").getAsString().equals(tableName)) {
                result = (JsonObject)table;
                break;
            }
        }

        return result;
    }

    public JsonObject queryRecordWithKey(JsonArray params) {
        String dbName = params.get(0).getAsJsonObject().get("db").getAsString();
        String tableName = params.get(0).getAsJsonObject().get("table").getAsString();
        JsonObject keyObject = params.get(0).getAsJsonObject().get("key").getAsJsonObject();

        return state.getAssetState(dbName, tableName, keyObject);
    }





}