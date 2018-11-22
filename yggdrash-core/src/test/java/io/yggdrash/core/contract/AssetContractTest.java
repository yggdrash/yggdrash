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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionBody;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.TransactionSignature;
import io.yggdrash.core.account.Wallet;
import io.yggdrash.core.genesis.BranchJson;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;


public class AssetContractTest {

    private static final Logger log = LoggerFactory.getLogger(AssetContractTest.class);

    private AssetContract assetContract;

    private byte[] branchId = Hex.decode("00fbbccfacd572b102351961c048104f4f21a008");

    private DefaultConfig defaultConfig;
    private Wallet wallet;

    private static final String DBNAME = "assetManagement";
    private static final String DBNAME1 = "assetManagement1";
    private static final String DBNAME2 = "assetManagement2";
    
    // user table
    private static final String TABLENAME_USER = "user";
    private static final String TABLENAME_USER1 = "user1";
    private static final String TABLENAME_USER2 = "user2";
    private JsonObject keyObjectUserSchema;
    private JsonObject recordObjectUserSchema;
    private JsonObject keyObjectUser1;
    private JsonObject recordObjectUser1;
    private JsonObject keyObjectUser2;
    private JsonObject recordObjectUser2;

    // asset table
    private static final String TABLENAME_ASSET = "asset";
    private static final String TABLENAME_ASSET1 = "asset1";
    private static final String TABLENAME_ASSET2 = "asset2";
    private JsonObject keyObjectAssetSchema;
    private JsonObject recordObjectAssetSchema;
    private JsonObject keyObjectAsset1;
    private JsonObject recordObjectAsset1;
    private JsonObject keyObjectAsset2;
    private JsonObject recordObjectAsset2;
    
    public AssetContractTest() throws IOException, InvalidCipherTextException {
        defaultConfig = new DefaultConfig();
        wallet = new Wallet(defaultConfig);

        // user table
        keyObjectUserSchema = new JsonObject();
        keyObjectUserSchema.addProperty("number", "");
        recordObjectUserSchema = new JsonObject();
        recordObjectUserSchema.addProperty("name", "");
        recordObjectUserSchema.addProperty("gender", "");
        recordObjectUserSchema.addProperty("age", "");
        recordObjectUserSchema.addProperty("department", "");

        keyObjectUser1 = new JsonObject();
        keyObjectUser1.addProperty("number", "1");
        recordObjectUser1 = new JsonObject();
        recordObjectUser1.addProperty("name", "Jaes Park");
        recordObjectUser1.addProperty("gender", "mail");
        recordObjectUser1.addProperty("age", "20");
        recordObjectUser1.addProperty("department", "development");

        keyObjectUser2 = new JsonObject();
        keyObjectUser2.addProperty("number", "2");
        recordObjectUser2 = new JsonObject();
        recordObjectUser2.addProperty("name", "Racheal Hong");
        recordObjectUser2.addProperty("gender", "femail");
        recordObjectUser2.addProperty("age", "21");
        recordObjectUser2.addProperty("department", "development");

        // asset table
        keyObjectAssetSchema = new JsonObject();
        keyObjectAssetSchema.addProperty("number", "");
        keyObjectAssetSchema.addProperty("code", "");
        recordObjectAssetSchema = new JsonObject();
        recordObjectAssetSchema.addProperty("type", "");
        recordObjectAssetSchema.addProperty("information", "");
        recordObjectAssetSchema.addProperty("price", "");
        recordObjectAssetSchema.addProperty("registrationDate", "");
        recordObjectAssetSchema.addProperty("disuseDate", "");
        {
            JsonObject userObject = new JsonObject();
            userObject.addProperty("table", TABLENAME_USER1);
            JsonObject userkeyObject = new JsonObject();
            userkeyObject.addProperty("number", "1");
            userObject.add("key", userkeyObject);
            recordObjectAssetSchema.add("user", userObject);
        }

        keyObjectAsset1 = new JsonObject();
        keyObjectAsset1.addProperty("number", "1");
        keyObjectAsset1.addProperty("code", "2018-00001");
        recordObjectAsset1 = new JsonObject();
        recordObjectAsset1.addProperty("type", "Notebook");
        recordObjectAsset1.addProperty("information", "MacBookPro15(A1707)");
        recordObjectAsset1.addProperty("price", "3000000");
        recordObjectAsset1.addProperty("registrationDate", "2018-11-14");
        recordObjectAsset1.addProperty("disuseDate", "0000-00-00");
        {
            JsonObject userObject = new JsonObject();
            userObject.addProperty("table", TABLENAME_USER1);
            JsonObject userkeyObject = new JsonObject();
            userkeyObject.addProperty("number", "1");
            userObject.add("key", userkeyObject);
            recordObjectAsset1.add("user", userObject);
        }

        keyObjectAsset2 = new JsonObject();
        keyObjectAsset2.addProperty("number", "2");
        keyObjectAsset2.addProperty("code", "2018-00002");
        recordObjectAsset2 = new JsonObject();
        recordObjectAsset2.addProperty("type", "Monitor");
        recordObjectAsset2.addProperty("information", "BENQ(PD2700Q)");
        recordObjectAsset2.addProperty("price", "400000");
        recordObjectAsset2.addProperty("registrationDate", "2018-11-15");
        recordObjectAsset2.addProperty("disuseDate", "0000-00-00");
        {
            JsonObject userObject = new JsonObject();
            userObject.addProperty("table", TABLENAME_USER1);
            JsonObject userkeyObject = new JsonObject();
            userkeyObject.addProperty("number", "2");
            userObject.add("key", userkeyObject);
            recordObjectAsset1.add("user", userObject);
        }
    }

    @Before
    public void setUp() {
        StateStore<JsonArray> stateStore = new StateStore<>();
        TransactionReceiptStore txReceiptStore = new TransactionReceiptStore();

        assetContract = new AssetContract();
        assetContract.init(stateStore, txReceiptStore);
    }

    @Test
    public void createAssetBranchTest() {

        try {
            String frontier = "{\"alloc\":{\n"
                    + "\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\": {\n"
                    + "\"balance\": \"1000000000\"\n"
                    + "},\n"
                    + "\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\": {\n"
                    + "\"balance\": \"1000000000\"\n"
                    + "}\n"
                    + "}}";

            JsonObject genesis = new Gson().fromJson(frontier, JsonObject.class);

            JsonObject branch = new JsonObject();
            branch.addProperty("name", "ASSET");
            branch.addProperty("symbol", "AST");
            branch.addProperty("property", "currency");
            branch.addProperty("type", "");
            branch.addProperty("description", "ASSET is the contract for managing assets.");
            branch.addProperty("contractId", "00fbbccfacd572b102351961c048104f4f21a008");
            branch.add("genesis", genesis);
            branch.addProperty("timestamp", "00000166c837f0c9");
            BranchJson.signBranch(wallet, branch);

            log.info(new GsonBuilder().setPrettyPrinting().create().toJson(branch));

        } catch (Exception e) {
            assert false;
        }

    }

    private  TransactionHusk makeTransaction(JsonArray txBodyArray) {
        TransactionBody txBody = new TransactionBody(txBodyArray);
        TransactionHeader txHeader = new TransactionHeader(
                branchId,
                new byte[8],
                new byte[8],
                TimeUtils.time(),
                txBody);
        TransactionSignature txSig = new TransactionSignature(wallet, txHeader.getHashForSigning());
        Transaction tx = new Transaction(txHeader, txSig.getSignature(), txBody);

        return new TransactionHusk(tx);
    }

    private boolean createDatabaseTxTest(String dbName) {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "createDatabase");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonArray txBodyArray = new JsonArray();
        txBodyArray.add(bodyObject);

        return assetContract.invoke(makeTransaction(txBodyArray));
    }

    private boolean createDatabaseModuleTest(String dbName) {
        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "createDatabase");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        TransactionReceipt receipt = assetContract.createDatabase(paramsArray);
        log.info(receipt.toString());
        return (receipt.getStatus() == 1);
    }

    @Test
    public void createDatabaseModuleTests() {
        if (createDatabaseModuleTest(DBNAME)
                && createDatabaseModuleTest(DBNAME1)
                && createDatabaseModuleTest(DBNAME2)) {
            log.info(assetContract.state.getAllKey().toString());
        } else {
            assert false;
        }
    }

    @Test
    public void createDatabaseTxTests() {
        if (createDatabaseTxTest(DBNAME)
                && createDatabaseTxTest(DBNAME1)
                && createDatabaseTxTest(DBNAME2)) {
            log.info(assetContract.state.getAllKey().toString());
        } else {
            assert false;
        }
    }

    private boolean createTableModuleTest(
            String dbName, String tableName, JsonObject keyObject, JsonObject recordObject) {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "createTable");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName);

        paramsObject.add("key", keyObject);
        paramsObject.add("record", recordObject);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonArray txBodyArray = new JsonArray();
        txBodyArray.add(bodyObject);

        TransactionReceipt receipt = assetContract.createTable(paramsArray);
        log.info(receipt.toString());
        return (receipt.getStatus() == 1);
    }

    @Test
    public void createTableModuleTests() {
        createDatabaseTxTest(DBNAME);

        if (createTableModuleTest(
                DBNAME, TABLENAME_USER, keyObjectUserSchema, recordObjectUserSchema)
                && createTableModuleTest(
                DBNAME, TABLENAME_ASSET, keyObjectAssetSchema, recordObjectAssetSchema)
                && createTableModuleTest(
                DBNAME, TABLENAME_USER1, keyObjectUserSchema, recordObjectUserSchema)
                && createTableModuleTest(
                DBNAME, TABLENAME_USER2, keyObjectUserSchema, recordObjectUserSchema)) {
            log.info(assetContract.state.get(DBNAME).toString());
        } else {
            assert false;
        }
    }

    private boolean createTableTxTest(
            String dbName, String tableName, JsonObject keyObject, JsonObject recordObject) {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "createTable");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName);

        paramsObject.add("key", keyObject);
        paramsObject.add("record", recordObject);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonArray txBodyArray = new JsonArray();
        txBodyArray.add(bodyObject);

        log.info(txBodyArray.toString());

        return assetContract.invoke(makeTransaction(txBodyArray));
    }

    @Test
    public void createTableTxTests() {
        createDatabaseTxTest(DBNAME);

        if (createTableTxTest(
                DBNAME, TABLENAME_USER, keyObjectUserSchema, recordObjectUserSchema)
                && createTableTxTest(
                        DBNAME, TABLENAME_ASSET, keyObjectAssetSchema, recordObjectAssetSchema)
                && createTableTxTest(
                        DBNAME, TABLENAME_USER1, keyObjectUserSchema, recordObjectUserSchema)
                && createTableTxTest(
                        DBNAME, TABLENAME_USER2, keyObjectUserSchema, recordObjectUserSchema)) {
            log.info(assetContract.state.get(DBNAME).toString());
        } else {
            assert false;
        }
    }

    private boolean insertModuleTest(
            String dbName, String tableName, JsonObject keyObject, JsonObject recordObject) {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "insert");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName);

        paramsObject.add("key", keyObject);
        paramsObject.add("record", recordObject);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        TransactionReceipt receipt = assetContract.insert(paramsArray);
        log.info(receipt.toString());
        return (receipt.getStatus() == 1);
    }

    @Test
    public void insertModuleTests() {
        createDatabaseModuleTest(DBNAME);
        createTableModuleTest(DBNAME, TABLENAME_USER, keyObjectUserSchema, recordObjectUserSchema);
        createTableModuleTest(DBNAME, TABLENAME_ASSET, keyObjectAssetSchema, recordObjectAssetSchema);

        if (insertModuleTest(
                DBNAME, TABLENAME_USER, keyObjectUser1, recordObjectUser1)
                && insertModuleTest(
                DBNAME, TABLENAME_USER, keyObjectUser2, recordObjectUser2)
                && insertModuleTest(
                DBNAME, TABLENAME_ASSET, keyObjectAsset1, recordObjectAsset1)
                && insertModuleTest(
                DBNAME, TABLENAME_ASSET, keyObjectAsset2, recordObjectAsset2)) {
            log.info(assetContract.state.get(DBNAME).toString());
            log.info(assetContract.state.getAssetState(DBNAME, TABLENAME_USER).toString());
        } else {
            assert false;
        }
    }

    private boolean insertTxTest(
            String dbName, String tableName, JsonObject keyObject, JsonObject recordObject) {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "insert");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName);

        paramsObject.add("key", keyObject);
        paramsObject.add("record", recordObject);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonArray txBodyArray = new JsonArray();
        txBodyArray.add(bodyObject);

        return assetContract.invoke(makeTransaction(txBodyArray));
    }

    @Test
    public void insertTxTests() {
        createDatabaseTxTest(DBNAME);
        createTableTxTest(DBNAME, TABLENAME_USER, keyObjectUserSchema, recordObjectUserSchema);
        createTableTxTest(DBNAME, TABLENAME_ASSET, keyObjectAssetSchema, recordObjectAssetSchema);

        if (insertTxTest(
                DBNAME, TABLENAME_USER, keyObjectUser1, recordObjectUser1)
                && insertTxTest(
                DBNAME, TABLENAME_USER, keyObjectUser2, recordObjectUser2)
                && insertTxTest(
                DBNAME, TABLENAME_ASSET, keyObjectAsset1, recordObjectAsset1)
                && insertTxTest(
                DBNAME, TABLENAME_ASSET, keyObjectAsset2, recordObjectAsset2)) {
            log.info(assetContract.state.get(DBNAME).toString());
            log.info(assetContract.state.getAssetState(DBNAME, TABLENAME_USER).toString());
        } else {
            assert false;
        }

    }

    private boolean updateModuleTest(
            String dbName, String tableName, JsonObject keyObject, JsonObject recordObject) {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "update");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName);

        paramsObject.add("key", keyObject);
        paramsObject.add("record", recordObject);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        TransactionReceipt receipt = assetContract.update(paramsArray);
        log.info(receipt.toString());
        return (receipt.getStatus() == 1);
    }


    @Test
    public void updateModuleTests() {
        createDatabaseModuleTest(DBNAME);
        createTableModuleTest(DBNAME, TABLENAME_USER, keyObjectUserSchema, recordObjectUserSchema);
        createTableModuleTest(DBNAME, TABLENAME_ASSET, keyObjectAssetSchema, recordObjectAssetSchema);

        insertModuleTest(DBNAME, TABLENAME_USER, keyObjectUser1, recordObjectUser1);
        insertModuleTest(DBNAME, TABLENAME_USER, keyObjectUser2, recordObjectUser2);
        insertModuleTest(DBNAME, TABLENAME_ASSET, keyObjectAsset1, recordObjectAsset1);
        insertModuleTest(DBNAME, TABLENAME_ASSET, keyObjectAsset2, recordObjectAsset2);

        if (updateModuleTest(
                DBNAME, TABLENAME_USER, keyObjectUser1, recordObjectUser2)
                && updateModuleTest(
                DBNAME, TABLENAME_USER, keyObjectUser2, recordObjectUser1)
                && updateModuleTest(
                DBNAME, TABLENAME_ASSET, keyObjectAsset1, recordObjectAsset2)
                && updateModuleTest(
                DBNAME, TABLENAME_ASSET, keyObjectAsset2, recordObjectAsset1)) {
            assertEquals(assetContract.state.getAssetState(DBNAME, TABLENAME_USER, keyObjectUser1),
                    recordObjectUser2);
            assertEquals(assetContract.state.getAssetState(DBNAME, TABLENAME_USER, keyObjectUser2),
                    recordObjectUser1);
            assertEquals(assetContract.state.getAssetState(DBNAME, TABLENAME_ASSET, keyObjectAsset1),
                    recordObjectAsset2);
            assertEquals(assetContract.state.getAssetState(DBNAME, TABLENAME_ASSET, keyObjectAsset2),
                    recordObjectAsset1);
            log.info(assetContract.state.get(DBNAME).toString());
            log.info(assetContract.state.getAssetState(DBNAME, TABLENAME_USER).toString());
        } else {
            assert false;
        }
    }

    @Test
    public void queryAllDatabasesModuleTest() {
        createDatabaseModuleTest(DBNAME);
        createDatabaseModuleTest(DBNAME1);

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "queryAllDatabases");

        JsonArray paramsArray = new JsonArray();
        bodyObject.add("params", paramsArray);

        JsonObject result = assetContract.queryAllDatabases(paramsArray);

        log.info(result.toString());

        if (result.get("db").getAsJsonArray().size() != 2) {
            throw new AssertionError();
        }
    }

    @Test
    public void queryAllDatabasesTest() {
        createDatabaseTxTest(DBNAME1);
        createDatabaseTxTest(DBNAME2);

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "queryAllDatabases");

        JsonArray paramsArray = new JsonArray();
        bodyObject.add("params", paramsArray);

        JsonObject result = assetContract.query(bodyObject);

        log.info(result.toString());

        if (result.get("db").getAsJsonArray().size() != 2) {
            throw new AssertionError();
        }
    }

    @Test
    public void queryAllTablesModuleTest() {
        createDatabaseModuleTest(DBNAME);
        //createTableUserTest("assetManagement", "user1");
        //createTableUserTest("assetManagement", "user2");

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "queryAllTables");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", "assetManagement");

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonObject result = assetContract.queryAllTables(paramsArray);

        log.info(result.toString());

        if (result.get("table").getAsJsonArray().size() != 2) {
            throw new AssertionError();
        }
    }

    @Test
    public void queryAllTablesTest() {
        createDatabaseTxTest(DBNAME);
        //createTableUserTest("assetManagement", "user1");
        //createTableUserTest("assetManagement", "user2");

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "queryAllTables");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", "assetManagement");

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonObject result = assetContract.query(bodyObject);

        log.info(result.toString());

        if (result.get("table").getAsJsonArray().size() != 2) {
            throw new AssertionError();
        }
    }

    @Test
    public void queryTableModuleTest() {
        createDatabaseTxTest(DBNAME);
        //createTableUserTest("assetManagement", "user1");
        //createTableUserTest("assetManagement", "user2");

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "queryTable");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", "assetManagement");
        paramsObject.addProperty("table", "user2");

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonObject result = assetContract.queryTable(paramsArray);

        if (result == null) {
            throw new AssertionError();
        }

        log.info(result.toString());
    }

    @Test
    public void queryTableTest() {
        // keyObject1
        JsonObject keyObject1 = new JsonObject();
        keyObject1.addProperty("number", "");

        // recordObjectUser1
        JsonObject recordObject1 = new JsonObject();
        recordObject1.addProperty("name", "");
        recordObject1.addProperty("gender", "");
        recordObject1.addProperty("age", "");
        recordObject1.addProperty("department", "");

        // keyObject2
        JsonObject keyObject2 = new JsonObject();
        keyObject2.addProperty("number", "");

        // recordObjectUser2
        JsonObject recordObject2 = new JsonObject();
        recordObject2.addProperty("name", "");
        recordObject2.addProperty("gender", "");
        recordObject2.addProperty("age", "");
        recordObject2.addProperty("department", "");
        recordObject2.addProperty("address", "");

        String dbName = "assetManagement";
        String tableName1 = "user1";
        String tableName2 = "user2";

        createDatabaseTxTest(DBNAME);
        //createTableUserTest(dbName, tableName1, keyObject1, recordObject1);
        //createTableUserTest(dbName, tableName2, keyObject2, recordObject2);

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "queryTable");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName2);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonObject tableSchema = new JsonObject();
        tableSchema.addProperty("table", tableName2);
        tableSchema.add("key", keyObject2);
        tableSchema.add("record", recordObject2);

        JsonObject result = assetContract.query(bodyObject);
        if (!result.equals(tableSchema)) {
            throw new AssertionError();
        }

        log.info(result.toString());
    }

    @Test
    public void queryRecordWithKeyModuleTest() {

        // keyObject1
        JsonObject keyObject1 = new JsonObject();
        keyObject1.addProperty("number", "1");

        // recordObjectUser1
        JsonObject recordObject1 = new JsonObject();
        recordObject1.addProperty("name", "Jaes Park");
        recordObject1.addProperty("gender", "mail");
        recordObject1.addProperty("age", "22");
        recordObject1.addProperty("department", "development");

        // keyObject2
        JsonObject keyObject2 = new JsonObject();
        keyObject2.addProperty("number", "2");

        // recordObjectUser2
        JsonObject recordObject2 = new JsonObject();
        recordObject2.addProperty("name", "Rachael Hong");
        recordObject2.addProperty("gender", "femail");
        recordObject2.addProperty("age", "21");
        recordObject2.addProperty("department", "development");

        String dbName = "assetManagement";
        String tableName = "user";

        createDatabaseModuleTest(DBNAME);
        //createTableUserTest(dbName, tableName);
//        insertRecordUserTest(dbName, tableName, keyObject1, recordObject1);
//        insertRecordUserTest(dbName, tableName, keyObject2, recordObject2);

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "queryRecordWithKey");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName);
        paramsObject.add("key", keyObject2);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonObject result = assetContract.queryRecordWithKey(paramsArray);
        if (!result.equals(recordObject2)) {
            throw new AssertionError();
        }

        log.info(result.toString());
    }

    @Test
    public void queryRecordWithKeyTest() {

        // keyObject1
        JsonObject keyObject1 = new JsonObject();
        keyObject1.addProperty("number", "1");

        // recordObjectUser1
        JsonObject recordObject1 = new JsonObject();
        recordObject1.addProperty("name", "Jaes Park");
        recordObject1.addProperty("gender", "mail");
        recordObject1.addProperty("age", "22");
        recordObject1.addProperty("department", "development");

        // keyObject2
        JsonObject keyObject2 = new JsonObject();
        keyObject2.addProperty("number", "2");

        // recordObjectUser2
        JsonObject recordObject2 = new JsonObject();
        recordObject2.addProperty("name", "Rachael Hong");
        recordObject2.addProperty("gender", "femail");
        recordObject2.addProperty("age", "21");
        recordObject2.addProperty("department", "development");

        String dbName = "assetManagement";
        String tableName = "user";

        createDatabaseModuleTest(DBNAME);
        //createTableUserTest(dbName, tableName);
//        insertRecordUserTest(dbName, tableName, keyObject1, recordObject1);
//        insertRecordUserTest(dbName, tableName, keyObject2, recordObject2);

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "queryRecordWithKey");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName);
        paramsObject.add("key", keyObject2);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonObject result = assetContract.query(bodyObject);
        if (!result.equals(recordObject2)) {
            throw new AssertionError();
        }

        log.info(result.toString());
    }

    @Test
    public void deleteRecordWithKeyModuleTest() {

        // keyObject1
        JsonObject keyObject1 = new JsonObject();
        keyObject1.addProperty("number", "");

        // recordObjectUser1
        JsonObject recordObject1 = new JsonObject();
        recordObject1.addProperty("name", "1");
        recordObject1.addProperty("gender", "");
        recordObject1.addProperty("age", "");
        recordObject1.addProperty("department", "");

        // keyObject2
        JsonObject keyObject2 = new JsonObject();
        keyObject2.addProperty("number", "2");

        // recordObjectUser2
        JsonObject recordObject2 = new JsonObject();
        recordObject2.addProperty("name", "");
        recordObject2.addProperty("gender", "");
        recordObject2.addProperty("age", "");
        recordObject2.addProperty("department", "");

        String dbName = "assetManagement";
        String tableName1 = "user1";

        createDatabaseModuleTest(DBNAME);
        //createTableUserTest(dbName, tableName1, keyObject1, recordObject1);
//        insertRecordUserTest(dbName, tableName1, keyObject1, recordObject1);
//        insertRecordUserTest(dbName, tableName1, keyObject2, recordObject2);

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "deleteRecordWithKey");

        // paramsObject
        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName1);

        // keyObject
        paramsObject.add("key", keyObject1);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonArray txBodyArray = new JsonArray();
        txBodyArray.add(bodyObject);

        log.info(txBodyArray.toString());

        // Module Test
        TransactionReceipt receipt = assetContract.deleteRecordWithKey(paramsArray);
        log.info(receipt.toString());
        if ((receipt.getStatus() != 1)) {
            throw new AssertionError();
        }

        log.info(assetContract.state.getAssetState(dbName, tableName1).toString());
    }

    @Test
    public void deleteRecordWithKeyTest() {
        // keyObject1
        JsonObject keyObject1 = new JsonObject();
        keyObject1.addProperty("number", "");

        // recordObjectUser1
        JsonObject recordObject1 = new JsonObject();
        recordObject1.addProperty("name", "1");
        recordObject1.addProperty("gender", "");
        recordObject1.addProperty("age", "");
        recordObject1.addProperty("department", "");

        // keyObject2
        JsonObject keyObject2 = new JsonObject();
        keyObject2.addProperty("number", "2");

        // recordObjectUser2
        JsonObject recordObject2 = new JsonObject();
        recordObject2.addProperty("name", "");
        recordObject2.addProperty("gender", "");
        recordObject2.addProperty("age", "");
        recordObject2.addProperty("department", "");

        String dbName = "assetManagement";
        String tableName1 = "user1";

        createDatabaseTxTest(DBNAME);
        //createTableUserTest(dbName, tableName1, keyObject1, recordObject1);
//        insertRecordUserTest(dbName, tableName1, keyObject1, recordObject1);
//        insertRecordUserTest(dbName, tableName1, keyObject2, recordObject2);

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "deleteRecordWithKey");

        // paramsObject
        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName1);

        // keyObject
        paramsObject.add("key", keyObject1);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonArray txBodyArray = new JsonArray();
        txBodyArray.add(bodyObject);

        log.info(txBodyArray.toString());

        TransactionBody txBody = new TransactionBody(txBodyArray);
        TransactionHeader txHeader = new TransactionHeader(
                branchId,
                new byte[8],
                new byte[8],
                TimeUtils.time(),
                txBody);
        TransactionSignature txSig = new TransactionSignature(wallet, txHeader.getHashForSigning());
        Transaction tx = new Transaction(txHeader, txSig.getSignature(), txBody);

        boolean result = assetContract.invoke(new TransactionHusk(tx));
        assertThat(result).isTrue();
    }

}

