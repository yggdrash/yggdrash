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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AssetContract extends BaseContract<JsonObject> {

    private static final Logger log = LoggerFactory.getLogger(AssetContract.class);

    public TransactionReceipt createdatabase(JsonObject param) {
        log.debug("createDatabase :: param => " + param);

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.setStatus(TransactionReceipt.SUCCESS);

        String dbName = param.get("db").getAsString();
        if (dbName == null || dbName.equals("")) {
            txReceipt.setStatus(TransactionReceipt.FALSE);
            txReceipt.putLog("createDatabase", "This dbName is not valid.");
        } else {
            if (state.get(dbName) == null) {
                try {
                    state.put(dbName, new JsonObject());
                } catch (Exception e) {
                    txReceipt.setStatus(TransactionReceipt.FALSE);
                    e.printStackTrace();
                }
                txReceipt.putLog("createDatabase", dbName);
            } else {
                txReceipt.setStatus(TransactionReceipt.FALSE);
                txReceipt.putLog("createDatabase", "This dbName is already exist.");
            }
        }

        log.debug(txReceipt.toString());
        return txReceipt;
    }

    public TransactionReceipt createtable(JsonObject param) {
        log.debug("createTable :: param => " + param);

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.setStatus(TransactionReceipt.SUCCESS);

        String dbName = param.get("db").getAsString();
        String tableName = param.get("table").getAsString();
        JsonObject keyObject = param.getAsJsonObject("key");
        JsonObject recordObject = param.getAsJsonObject("record");

        if (dbName == null || dbName.equals("")
                || tableName == null || tableName.equals("")
                || keyObject == null || keyObject.size() == 0
                || recordObject == null) {
            txReceipt.setStatus(TransactionReceipt.FALSE);
            txReceipt.putLog("createTable", param.toString() + " This table is not valid.");
        } else {
            param.remove("db");

            for (JsonElement dbElement : state.get(dbName).getAsJsonArray()) {
                if (dbElement.getAsJsonObject().get("table").getAsString().equals(tableName)) {
                    txReceipt.setStatus(TransactionReceipt.FALSE);
                    txReceipt.putLog("createTable",
                            param.toString() + " This table is already exist.");
                    break;
                }
            }

            JsonArray stateArray = state.get(dbName).getAsJsonArray();
            if (stateArray != null) {
                stateArray.add(param);
                try {
                    state.put(dbName, stateArray.getAsJsonObject());
                    txReceipt.putLog("createTable", param);
                } catch (Exception e) {
                    txReceipt.setStatus(TransactionReceipt.FALSE);
                    e.printStackTrace();
                }
            } else {
                txReceipt.setStatus(TransactionReceipt.FALSE);
                txReceipt.putLog("createTable", param.toString() + " This table is already exist.");
            }
        }

        log.debug(txReceipt.toString());
        return txReceipt;
    }

    public TransactionReceipt insert(JsonObject param) {
        log.debug("insert :: param => " + param);

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.setStatus(TransactionReceipt.SUCCESS);

        String dbName = param.get("db").getAsString();
        String tableName = param.get("table").getAsString();
        JsonObject keyObject = param.getAsJsonObject("key");
        JsonObject recordObject = param.getAsJsonObject("record");

        if (dbName == null || dbName.equals("")
                || tableName == null || tableName.equals("")
                || keyObject == null || keyObject.size() == 0
                || recordObject == null || recordObject.size() == 0) {
            txReceipt.setStatus(TransactionReceipt.FALSE);
            txReceipt.putLog("insert", param.toString() + " This data is not valid.");
        }

        // check db & table
        JsonObject tableObject = null;
        for (JsonElement dbElement : state.get(dbName).getAsJsonArray()) {
            if (dbElement.getAsJsonObject().get("table").getAsString().equals(tableName)) {
                tableObject = dbElement.getAsJsonObject();
            }
        }

        if (tableObject == null || !checkParam(tableObject, param)) {
            txReceipt.setStatus(TransactionReceipt.FALSE);
            txReceipt.putLog("insert",
                    param.toString() + " This table is not valid.");
        }

        // insert record
        if (!state.putAssetState(dbName, tableName, keyObject, recordObject)) {
            txReceipt.setStatus(TransactionReceipt.FALSE);
            txReceipt.putLog("insert", param.toString() + " This record is not valid.");
        }

        txReceipt.putLog("insert", param.toString());
        log.debug(txReceipt.toString());

        return txReceipt;
    }

    public TransactionReceipt update(JsonObject param) {
        log.debug("update :: param => " + param);

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.setStatus(TransactionReceipt.SUCCESS);

        String dbName = param.get("db").getAsString();
        String tableName = param.get("table").getAsString();
        JsonObject keyObject = param.getAsJsonObject("key");
        JsonObject recordObject = param.getAsJsonObject("record");

        if (dbName == null || dbName.equals("")
                || tableName == null || tableName.equals("")
                || keyObject == null || keyObject.size() == 0
                || recordObject == null || recordObject.size() == 0) {
            txReceipt.setStatus(TransactionReceipt.FALSE);
            txReceipt.putLog("update", param.toString() + " This data is not valid.");
        }

        // check db & table
        JsonObject tableObject = null;
        for (JsonElement dbElement : state.get(dbName).getAsJsonArray()) {
            if (dbElement.getAsJsonObject().get("table").getAsString().equals(tableName)) {
                tableObject = dbElement.getAsJsonObject();
            }
        }

        if (tableObject == null || !checkParam(tableObject, param)) {
            txReceipt.setStatus(TransactionReceipt.FALSE);
            txReceipt.putLog("update", param.toString() + " This table is not valid.");
        }

        // update record
        if (!state.updateAssetState(dbName, tableName, keyObject, recordObject)) {
            txReceipt.setStatus(TransactionReceipt.FALSE);
            txReceipt.putLog("update", param.toString() + " This record is not valid.");
        }

        txReceipt.putLog("update", param.toString());
        log.debug(txReceipt.toString());

        return txReceipt;
    }

    private boolean checkParam(JsonObject table, JsonObject record) {

        String tableObjectName = table.get("table").getAsString();
        String recordObjectName = record.get("table").getAsString();

        if (!tableObjectName.equals(recordObjectName)) {
            return false;
        }

        JsonObject tableKeyObject = table.getAsJsonObject("key");
        JsonObject recordKeyObject = record.getAsJsonObject("key");

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

        JsonObject tableRecordObject = table.getAsJsonObject("record");
        JsonObject recordRecordObject = record.getAsJsonObject("record");

        // check record
        for (Map.Entry<String, JsonElement> entry : recordRecordObject.entrySet()) {
            if (!tableRecordObject.has(entry.getKey())) {
                return false;
            }
        }

        return true;
    }

    public JsonObject queryalldatabases(JsonObject param) {

        JsonObject result = new JsonObject();
        JsonArray dbArray = new JsonArray();

        for (Object dbName : state.getAllKey()) {
            dbArray.add(dbName.toString());
        }

        result.add("db", dbArray);

        return result;
    }

    public JsonObject queryalltables(JsonObject param) {

        String db = param.get("db").getAsString();

        JsonObject result = new JsonObject();
        JsonArray tableNames = new JsonArray();

        JsonArray tableStates = state.get(db).getAsJsonArray();

        for (JsonElement table : tableStates) {
            tableNames.add(table.getAsJsonObject().get("table").getAsString());
        }

        result.add("table", tableNames);

        return result;
    }

    public JsonObject querytable(JsonObject param) {

        String dbName = param.get("db").getAsString();
        String tableName = param.get("table").getAsString();

        JsonObject result = null;

        JsonArray tableStates = state.get(dbName).getAsJsonArray();

        for (JsonElement table : tableStates) {
            if (table.getAsJsonObject().get("table").getAsString().equals(tableName)) {
                result = (JsonObject)table;
                break;
            }
        }

        return result;
    }

    public JsonObject queryrecordwithkey(JsonObject param) {
        String dbName = param.get("db").getAsString();
        String tableName = param.get("table").getAsString();
        JsonObject keyObject = param.getAsJsonObject("key");

        return state.getAssetState(dbName, tableName, keyObject);
    }

    public TransactionReceipt deleterecordwithkey(JsonObject param) {
        log.debug("delete :: param => " + param);

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.setStatus(TransactionReceipt.SUCCESS);

        String dbName = param.get("db").getAsString();
        String tableName = param.get("table").getAsString();
        JsonObject keyObject = param.getAsJsonObject("key");

        if (dbName == null || dbName.equals("")
                || tableName == null || tableName.equals("")
                || keyObject == null || keyObject.size() == 0) {
            txReceipt.setStatus(TransactionReceipt.FALSE);
            txReceipt.putLog("delete", param.toString() + " This data is not valid.");
        }

        // check db & table
        JsonObject tableObject = null;
        for (JsonElement dbElement : state.get(dbName).getAsJsonArray()) {
            if (dbElement.getAsJsonObject().get("table").getAsString().equals(tableName)) {
                tableObject = dbElement.getAsJsonObject();
            }
        }

        if (tableObject == null) {
            txReceipt.setStatus(TransactionReceipt.FALSE);
            txReceipt.putLog("delete", param.toString() + " This table is not valid.");
        }

        // delete record
        try {
            state.getAssetState(dbName, tableName).remove(keyObject);
        } catch (Exception e) {
            txReceipt.setStatus(TransactionReceipt.FALSE);
            txReceipt.putLog("delete", param.toString() + " This record is not valid.");
        }

        txReceipt.putLog("deleteRecordWithKey", param.toString());
        log.debug(txReceipt.toString());

        return txReceipt;
    }

}