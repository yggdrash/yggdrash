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
import io.yggdrash.TestUtils;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBody;
import io.yggdrash.core.blockchain.TransactionHeader;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.blockchain.TransactionSignature;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import io.yggdrash.core.wallet.Wallet;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class AssetContractTest {

    private static final Logger log = LoggerFactory.getLogger(AssetContractTest.class);

    private AssetContract assetContract;

    private byte[] branchId = Hex.decode("9f2ef14cb553b07866a9cd1293c3331dbace10f9");

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
        StateStore<JsonArray> stateStore = new StateStore<>(new HashMapDbSource());
        TransactionReceiptStore txReceiptStore = TestUtils.testReceipt();

        assetContract = new AssetContract();
        assetContract.init(stateStore, txReceiptStore);
    }

    @Test
    public void createAssetBranchTest() {
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
        branch.addProperty("contractId", "dadb74381eec75ec6d3f91c12dfc8e286d6e736f");
        branch.add("genesis", genesis);
        branch.addProperty("timestamp", "00000166c837f0c9");
        TestUtils.signBranch(wallet, branch);

        String branchJsonString = new GsonBuilder().setPrettyPrinting().create().toJson(branch);
        assert branchJsonString.contains("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        assert branchJsonString.contains("signature");
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
        assert createDatabaseModuleTest(DBNAME)
                && createDatabaseModuleTest(DBNAME1)
                && createDatabaseModuleTest(DBNAME2);
        log.info(assetContract.state.getAllKey().toString());
    }

    @Test
    public void createDatabaseTxTests() {
        assert createDatabaseTxTest(DBNAME)
                && createDatabaseTxTest(DBNAME1)
                && createDatabaseTxTest(DBNAME2);
        log.info(assetContract.state.getAllKey().toString());
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

        assert createTableModuleTest(
                DBNAME, TABLENAME_USER, keyObjectUserSchema, recordObjectUserSchema)
                && createTableModuleTest(
                DBNAME, TABLENAME_ASSET, keyObjectAssetSchema, recordObjectAssetSchema)
                && createTableModuleTest(
                DBNAME, TABLENAME_USER1, keyObjectUserSchema, recordObjectUserSchema)
                && createTableModuleTest(
                DBNAME, TABLENAME_USER2, keyObjectUserSchema, recordObjectUserSchema);
        log.info(assetContract.state.get(DBNAME).toString());
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

        assert createTableTxTest(
                DBNAME, TABLENAME_USER, keyObjectUserSchema, recordObjectUserSchema)
                && createTableTxTest(
                        DBNAME, TABLENAME_ASSET, keyObjectAssetSchema, recordObjectAssetSchema)
                && createTableTxTest(
                        DBNAME, TABLENAME_USER1, keyObjectUserSchema, recordObjectUserSchema)
                && createTableTxTest(
                DBNAME, TABLENAME_USER2, keyObjectUserSchema, recordObjectUserSchema);
        log.info(assetContract.state.get(DBNAME).toString());
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
        createTableModuleTest(
                DBNAME, TABLENAME_USER, keyObjectUserSchema, recordObjectUserSchema);
        createTableModuleTest(
                DBNAME, TABLENAME_ASSET, keyObjectAssetSchema, recordObjectAssetSchema);

        assert insertModuleTest(
                DBNAME, TABLENAME_USER, keyObjectUser1, recordObjectUser1)
                && insertModuleTest(
                DBNAME, TABLENAME_USER, keyObjectUser2, recordObjectUser2)
                && insertModuleTest(
                DBNAME, TABLENAME_ASSET, keyObjectAsset1, recordObjectAsset1)
                && insertModuleTest(
                DBNAME, TABLENAME_ASSET, keyObjectAsset2, recordObjectAsset2);

        assertEquals(assetContract.state.getAssetState(DBNAME, TABLENAME_USER, keyObjectUser1),
                recordObjectUser1);
        assertEquals(assetContract.state.getAssetState(DBNAME, TABLENAME_USER, keyObjectUser2),
                recordObjectUser2);
        assertEquals(
                assetContract.state.getAssetState(DBNAME, TABLENAME_ASSET, keyObjectAsset1),
                recordObjectAsset1);
        assertEquals(
                assetContract.state.getAssetState(DBNAME, TABLENAME_ASSET, keyObjectAsset2),
                recordObjectAsset2);
        log.info(assetContract.state.get(DBNAME).toString());
        log.info(assetContract.state.getAssetState(DBNAME, TABLENAME_USER).toString());
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

        assert insertTxTest(
                DBNAME, TABLENAME_USER, keyObjectUser1, recordObjectUser1)
                && insertTxTest(
                DBNAME, TABLENAME_USER, keyObjectUser2, recordObjectUser2)
                && insertTxTest(
                DBNAME, TABLENAME_ASSET, keyObjectAsset1, recordObjectAsset1)
                && insertTxTest(
                DBNAME, TABLENAME_ASSET, keyObjectAsset2, recordObjectAsset2);
        assertEquals(assetContract.state.getAssetState(DBNAME, TABLENAME_USER, keyObjectUser1),
                recordObjectUser1);
        assertEquals(assetContract.state.getAssetState(DBNAME, TABLENAME_USER, keyObjectUser2),
                recordObjectUser2);
        assertEquals(
                assetContract.state.getAssetState(DBNAME, TABLENAME_ASSET, keyObjectAsset1),
                recordObjectAsset1);
        assertEquals(
                assetContract.state.getAssetState(DBNAME, TABLENAME_ASSET, keyObjectAsset2),
                recordObjectAsset2);
        log.info(assetContract.state.get(DBNAME).toString());
        log.info(assetContract.state.getAssetState(DBNAME, TABLENAME_USER).toString());
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
        createTableModuleTest(
                DBNAME, TABLENAME_USER, keyObjectUserSchema, recordObjectUserSchema);
        createTableModuleTest(
                DBNAME, TABLENAME_ASSET, keyObjectAssetSchema, recordObjectAssetSchema);

        insertModuleTest(DBNAME, TABLENAME_USER, keyObjectUser1, recordObjectUser1);
        insertModuleTest(DBNAME, TABLENAME_USER, keyObjectUser2, recordObjectUser2);
        insertModuleTest(DBNAME, TABLENAME_ASSET, keyObjectAsset1, recordObjectAsset1);
        insertModuleTest(DBNAME, TABLENAME_ASSET, keyObjectAsset2, recordObjectAsset2);

        assert updateModuleTest(
                DBNAME, TABLENAME_USER, keyObjectUser1, recordObjectUser2)
                && updateModuleTest(
                DBNAME, TABLENAME_USER, keyObjectUser2, recordObjectUser1)
                && updateModuleTest(
                DBNAME, TABLENAME_ASSET, keyObjectAsset1, recordObjectAsset2)
                && updateModuleTest(
                DBNAME, TABLENAME_ASSET, keyObjectAsset2, recordObjectAsset1);
        assertEquals(assetContract.state.getAssetState(DBNAME, TABLENAME_USER, keyObjectUser1),
                recordObjectUser2);
        assertEquals(assetContract.state.getAssetState(DBNAME, TABLENAME_USER, keyObjectUser2),
                recordObjectUser1);
        assertEquals(
                assetContract.state.getAssetState(DBNAME, TABLENAME_ASSET, keyObjectAsset1),
                recordObjectAsset2);
        assertEquals(
                assetContract.state.getAssetState(DBNAME, TABLENAME_ASSET, keyObjectAsset2),
                recordObjectAsset1);
        log.info(assetContract.state.get(DBNAME).toString());
        log.info(assetContract.state.getAssetState(DBNAME, TABLENAME_USER).toString());
    }

    private boolean updateTxTest(
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

        bodyObject.add("params", paramsArray);

        JsonArray txBodyArray = new JsonArray();
        txBodyArray.add(bodyObject);

        return assetContract.invoke(makeTransaction(txBodyArray));
    }

    @Test
    public void updateTxTests() {
        createDatabaseTxTest(DBNAME);
        createTableTxTest(DBNAME, TABLENAME_USER, keyObjectUserSchema, recordObjectUserSchema);
        createTableTxTest(DBNAME, TABLENAME_ASSET, keyObjectAssetSchema, recordObjectAssetSchema);

        insertTxTest(DBNAME, TABLENAME_USER, keyObjectUser1, recordObjectUser1);
        insertTxTest(DBNAME, TABLENAME_USER, keyObjectUser2, recordObjectUser2);
        insertTxTest(DBNAME, TABLENAME_ASSET, keyObjectAsset1, recordObjectAsset1);
        insertTxTest(DBNAME, TABLENAME_ASSET, keyObjectAsset2, recordObjectAsset2);

        assert updateTxTest(
                DBNAME, TABLENAME_USER, keyObjectUser1, recordObjectUser2)
                && updateTxTest(
                DBNAME, TABLENAME_USER, keyObjectUser2, recordObjectUser1)
                && updateTxTest(
                DBNAME, TABLENAME_ASSET, keyObjectAsset1, recordObjectAsset2)
                && updateTxTest(
                DBNAME, TABLENAME_ASSET, keyObjectAsset2, recordObjectAsset1);
        assertEquals(assetContract.state.getAssetState(DBNAME, TABLENAME_USER, keyObjectUser1),
                recordObjectUser2);
        assertEquals(assetContract.state.getAssetState(DBNAME, TABLENAME_USER, keyObjectUser2),
                recordObjectUser1);
        assertEquals(
                assetContract.state.getAssetState(DBNAME, TABLENAME_ASSET, keyObjectAsset1),
                recordObjectAsset2);
        assertEquals(
                assetContract.state.getAssetState(DBNAME, TABLENAME_ASSET, keyObjectAsset2),
                recordObjectAsset1);
        log.info(assetContract.state.get(DBNAME).toString());
        log.info(assetContract.state.getAssetState(DBNAME, TABLENAME_USER).toString());
    }

    private JsonObject queryAllDatabasesModuleTest() {
        JsonArray paramsArray = new JsonArray();
        return assetContract.queryAllDatabases(paramsArray);
    }

    @Test
    public void queryAllDatabasesModuleTests() {
        createDatabaseModuleTest(DBNAME1);
        createDatabaseModuleTest(DBNAME2);

        JsonObject result = queryAllDatabasesModuleTest();
        log.info(result.toString());
        assertEquals(2, result.get("db").getAsJsonArray().size());
    }

    private JsonObject queryAllDatabasesTxTest() {
        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "queryAllDatabases");

        JsonArray paramsArray = new JsonArray();
        bodyObject.add("params", paramsArray);

        return assetContract.query(bodyObject);
    }

    @Test
    public void queryAllDatabasesTxTests() {
        createDatabaseTxTest(DBNAME1);
        createDatabaseTxTest(DBNAME2);

        JsonObject result = queryAllDatabasesTxTest();
        log.info(result.toString());
        assertEquals(2, result.get("db").getAsJsonArray().size());
    }

    @Test
    public void queryAllTablesModuleTest() {
        createDatabaseModuleTest(DBNAME);
        createTableModuleTest(
                DBNAME, TABLENAME_USER1, keyObjectUserSchema, recordObjectUserSchema);
        createTableModuleTest(
                DBNAME, TABLENAME_USER2, keyObjectUserSchema, recordObjectUserSchema);
        createTableModuleTest(
                DBNAME, TABLENAME_ASSET1, keyObjectAssetSchema, recordObjectAssetSchema);
        createTableModuleTest(
                DBNAME, TABLENAME_ASSET2, keyObjectAssetSchema, recordObjectAssetSchema);

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "queryAllTables");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", DBNAME);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);
        bodyObject.add("params", paramsArray);

        JsonObject result = assetContract.queryAllTables(paramsArray);
        log.info(result.toString());
        assertEquals(4, result.get("table").getAsJsonArray().size());
    }

    @Test
    public void queryAllTablesTxTest() {
        createDatabaseTxTest(DBNAME);
        createTableTxTest(
                DBNAME, TABLENAME_USER1, keyObjectUserSchema, recordObjectUserSchema);
        createTableTxTest(
                DBNAME, TABLENAME_USER2, keyObjectUserSchema, recordObjectUserSchema);
        createTableTxTest(
                DBNAME, TABLENAME_ASSET1, keyObjectAssetSchema, recordObjectAssetSchema);
        createTableTxTest(
                DBNAME, TABLENAME_ASSET2, keyObjectAssetSchema, recordObjectAssetSchema);

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "queryAllTables");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", DBNAME);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);
        bodyObject.add("params", paramsArray);

        JsonObject result = assetContract.query(bodyObject);
        log.info(result.toString());
        assertEquals(4, result.get("table").getAsJsonArray().size());
    }

    private JsonObject queryTableModuleTest(String dbName, String tableName) {
        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "queryTable");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        return assetContract.queryTable(paramsArray);
    }

    @Test
    public void queryTableModuleTests() {
        createDatabaseModuleTest(DBNAME);

        createTableModuleTest(
                DBNAME, TABLENAME_USER1, keyObjectUserSchema, recordObjectUserSchema);
        createTableModuleTest(
                DBNAME, TABLENAME_USER2, keyObjectUserSchema, recordObjectUserSchema);
        createTableModuleTest(
                DBNAME, TABLENAME_ASSET1, keyObjectAssetSchema, recordObjectAssetSchema);
        createTableModuleTest(
                DBNAME, TABLENAME_ASSET2, keyObjectAssetSchema, recordObjectAssetSchema);

        JsonObject result = queryTableModuleTest(DBNAME, TABLENAME_USER1);
        log.info(result.toString());
        {
            JsonObject tableObjectSchema = new JsonObject();
            tableObjectSchema.addProperty("table", TABLENAME_USER1);
            tableObjectSchema.add("key", keyObjectUserSchema);
            tableObjectSchema.add("record", recordObjectUserSchema);

            assertEquals(result, tableObjectSchema);
        }

        result = queryTableModuleTest(DBNAME, TABLENAME_USER2);
        log.info(result.toString());
        {
            JsonObject tableObjectSchema = new JsonObject();
            tableObjectSchema.addProperty("table", TABLENAME_USER2);
            tableObjectSchema.add("key", keyObjectUserSchema);
            tableObjectSchema.add("record", recordObjectUserSchema);

            assertEquals(result, tableObjectSchema);
        }

        result = queryTableModuleTest(DBNAME, TABLENAME_ASSET1);
        log.info(result.toString());
        {
            JsonObject tableObjectSchema = new JsonObject();
            tableObjectSchema.addProperty("table", TABLENAME_ASSET1);
            tableObjectSchema.add("key", keyObjectAssetSchema);
            tableObjectSchema.add("record", recordObjectAssetSchema);

            assertEquals(result, tableObjectSchema);
        }

        result = queryTableModuleTest(DBNAME, TABLENAME_ASSET2);
        log.info(result.toString());
        {
            JsonObject tableObjectSchema = new JsonObject();
            tableObjectSchema.addProperty("table", TABLENAME_ASSET2);
            tableObjectSchema.add("key", keyObjectAssetSchema);
            tableObjectSchema.add("record", recordObjectAssetSchema);

            assertEquals(result, tableObjectSchema);
        }
    }

    private JsonObject queryTableTxTest(String dbName, String tableName) {
        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "queryTable");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);
        bodyObject.add("params", paramsArray);

        return assetContract.query(bodyObject);
    }

    @Test
    public void queryTableTxTests() {
        createDatabaseTxTest(DBNAME);

        createTableTxTest(
                DBNAME, TABLENAME_USER1, keyObjectUserSchema, recordObjectUserSchema);
        createTableTxTest(
                DBNAME, TABLENAME_USER2, keyObjectUserSchema, recordObjectUserSchema);
        createTableTxTest(
                DBNAME, TABLENAME_ASSET1, keyObjectAssetSchema, recordObjectAssetSchema);
        createTableTxTest(
                DBNAME, TABLENAME_ASSET2, keyObjectAssetSchema, recordObjectAssetSchema);

        JsonObject result = queryTableTxTest(DBNAME, TABLENAME_USER1);
        log.info(result.toString());
        {
            JsonObject tableObjectSchema = new JsonObject();
            tableObjectSchema.addProperty("table", TABLENAME_USER1);
            tableObjectSchema.add("key", keyObjectUserSchema);
            tableObjectSchema.add("record", recordObjectUserSchema);

            assertEquals(result, tableObjectSchema);
        }

        result = queryTableTxTest(DBNAME, TABLENAME_USER2);
        log.info(result.toString());
        {
            JsonObject tableObjectSchema = new JsonObject();
            tableObjectSchema.addProperty("table", TABLENAME_USER2);
            tableObjectSchema.add("key", keyObjectUserSchema);
            tableObjectSchema.add("record", recordObjectUserSchema);

            assertEquals(result, tableObjectSchema);
        }

        result = queryTableTxTest(DBNAME, TABLENAME_ASSET1);
        log.info(result.toString());
        {
            JsonObject tableObjectSchema = new JsonObject();
            tableObjectSchema.addProperty("table", TABLENAME_ASSET1);
            tableObjectSchema.add("key", keyObjectAssetSchema);
            tableObjectSchema.add("record", recordObjectAssetSchema);

            assertEquals(result, tableObjectSchema);
        }

        result = queryTableTxTest(DBNAME, TABLENAME_ASSET2);
        log.info(result.toString());
        {
            JsonObject tableObjectSchema = new JsonObject();
            tableObjectSchema.addProperty("table", TABLENAME_ASSET2);
            tableObjectSchema.add("key", keyObjectAssetSchema);
            tableObjectSchema.add("record", recordObjectAssetSchema);

            assertEquals(result, tableObjectSchema);
        }
    }

    private JsonObject queryRecordWithKeyModuleTest(
            String dbName, String tableName, JsonObject keyObject) {
        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "queryRecordWithKey");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName);
        paramsObject.add("key", keyObject);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        return assetContract.queryRecordWithKey(paramsArray);
    }

    @Test
    public void queryRecordWithKeyModuleTests() {
        createDatabaseModuleTest(DBNAME);

        createTableModuleTest(
                DBNAME, TABLENAME_USER1, keyObjectUserSchema, recordObjectUserSchema);
        createTableModuleTest(
                DBNAME, TABLENAME_USER2, keyObjectUserSchema, recordObjectUserSchema);
        createTableModuleTest(
                DBNAME, TABLENAME_ASSET1, keyObjectAssetSchema, recordObjectAssetSchema);
        createTableModuleTest(
                DBNAME, TABLENAME_ASSET2, keyObjectAssetSchema, recordObjectAssetSchema);

        insertTxTest(DBNAME, TABLENAME_USER1, keyObjectUser1, recordObjectUser1);
        insertTxTest(DBNAME, TABLENAME_USER2, keyObjectUser2, recordObjectUser2);
        insertTxTest(DBNAME, TABLENAME_ASSET1, keyObjectAsset1, recordObjectAsset1);
        insertTxTest(DBNAME, TABLENAME_ASSET2, keyObjectAsset2, recordObjectAsset2);

        JsonObject result = queryRecordWithKeyModuleTest(DBNAME, TABLENAME_USER1, keyObjectUser1);
        log.info(result.toString());
        assertEquals(result, recordObjectUser1);

        result = queryRecordWithKeyModuleTest(DBNAME, TABLENAME_USER2, keyObjectUser2);
        log.info(result.toString());
        assertEquals(result, recordObjectUser2);

        result = queryRecordWithKeyModuleTest(DBNAME, TABLENAME_ASSET1, keyObjectAsset1);
        log.info(result.toString());
        assertEquals(result, recordObjectAsset1);

        result = queryRecordWithKeyModuleTest(DBNAME, TABLENAME_ASSET2, keyObjectAsset2);
        log.info(result.toString());
        assertEquals(result, recordObjectAsset2);
    }

    private JsonObject queryRecordWithKeyTxTest(
            String dbName, String tableName, JsonObject keyObject) {
        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "queryRecordWithKey");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName);
        paramsObject.add("key", keyObject);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        return assetContract.query(bodyObject);
    }

    @Test
    public void queryRecordWithKeyTxTests() {
        createDatabaseTxTest(DBNAME);

        createTableTxTest(
                DBNAME, TABLENAME_USER1, keyObjectUserSchema, recordObjectUserSchema);
        createTableTxTest(
                DBNAME, TABLENAME_USER2, keyObjectUserSchema, recordObjectUserSchema);
        createTableTxTest(
                DBNAME, TABLENAME_ASSET1, keyObjectAssetSchema, recordObjectAssetSchema);
        createTableTxTest(
                DBNAME, TABLENAME_ASSET2, keyObjectAssetSchema, recordObjectAssetSchema);

        insertTxTest(DBNAME, TABLENAME_USER1, keyObjectUser1, recordObjectUser1);
        insertTxTest(DBNAME, TABLENAME_USER2, keyObjectUser2, recordObjectUser2);
        insertTxTest(DBNAME, TABLENAME_ASSET1, keyObjectAsset1, recordObjectAsset1);
        insertTxTest(DBNAME, TABLENAME_ASSET2, keyObjectAsset2, recordObjectAsset2);

        JsonObject result = queryRecordWithKeyTxTest(DBNAME, TABLENAME_USER1, keyObjectUser1);
        log.info(result.toString());
        assertEquals(result, recordObjectUser1);

        result = queryRecordWithKeyTxTest(DBNAME, TABLENAME_USER2, keyObjectUser2);
        log.info(result.toString());
        assertEquals(result, recordObjectUser2);

        result = queryRecordWithKeyTxTest(DBNAME, TABLENAME_ASSET1, keyObjectAsset1);
        log.info(result.toString());
        assertEquals(result, recordObjectAsset1);

        result = queryRecordWithKeyTxTest(DBNAME, TABLENAME_ASSET2, keyObjectAsset2);
        log.info(result.toString());
        assertEquals(result, recordObjectAsset2);
    }

    private boolean deleteRecordWithKeyModuleTest(
            String dbName, String tableName, JsonObject keyObject) {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "deleteRecordWithKey");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName);
        paramsObject.add("key", keyObject);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        TransactionReceipt receipt = assetContract.deleteRecordWithKey(paramsArray);
        log.info(receipt.toString());
        return (receipt.getStatus() == 1);
    }

    @Test
    public void deleteRecordWithKeyModuleTests() {
        createDatabaseModuleTest(DBNAME);

        createTableModuleTest(
                DBNAME, TABLENAME_USER1, keyObjectUserSchema, recordObjectUserSchema);
        createTableModuleTest(
                DBNAME, TABLENAME_USER2, keyObjectUserSchema, recordObjectUserSchema);
        createTableModuleTest(
                DBNAME, TABLENAME_ASSET1, keyObjectAssetSchema, recordObjectAssetSchema);
        createTableModuleTest(
                DBNAME, TABLENAME_ASSET2, keyObjectAssetSchema, recordObjectAssetSchema);

        insertTxTest(DBNAME, TABLENAME_USER1, keyObjectUser1, recordObjectUser1);
        insertTxTest(DBNAME, TABLENAME_USER2, keyObjectUser2, recordObjectUser2);
        insertTxTest(DBNAME, TABLENAME_ASSET1, keyObjectAsset1, recordObjectAsset1);
        insertTxTest(DBNAME, TABLENAME_ASSET1, keyObjectAsset2, recordObjectAsset2);

        assert deleteRecordWithKeyModuleTest(DBNAME, TABLENAME_USER1, keyObjectUser1)
                && deleteRecordWithKeyModuleTest(DBNAME, TABLENAME_USER2, keyObjectUser2)
                && deleteRecordWithKeyModuleTest(DBNAME, TABLENAME_ASSET1, keyObjectAsset1)
                && deleteRecordWithKeyModuleTest(DBNAME, TABLENAME_ASSET1, keyObjectAsset2);
        assertNull(assetContract.state.getAssetState(DBNAME, TABLENAME_USER1, keyObjectUser1));
        assertNull(assetContract.state.getAssetState(DBNAME, TABLENAME_USER2, keyObjectUser2));
        assertNull(
                assetContract.state.getAssetState(DBNAME, TABLENAME_ASSET1, keyObjectAsset1));
        assertNull(
                assetContract.state.getAssetState(DBNAME, TABLENAME_ASSET1, keyObjectAsset2));
        log.info(assetContract.state.get(DBNAME).toString());
        log.info(assetContract.state.getAssetState(DBNAME, TABLENAME_USER1).toString());
        log.info(assetContract.state.getAssetState(DBNAME, TABLENAME_USER2).toString());
        log.info(assetContract.state.getAssetState(DBNAME, TABLENAME_ASSET1).toString());
    }

    private boolean deleteRecordWithKeyTxTest(
            String dbName, String tableName, JsonObject keyObject) {
        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "deleteRecordWithKey");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName);
        paramsObject.add("key", keyObject);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonArray txBodyArray = new JsonArray();
        txBodyArray.add(bodyObject);

        return assetContract.invoke(makeTransaction(txBodyArray));
    }

    @Test
    public void deleteRecordWithKeyTxTests() {
        createDatabaseTxTest(DBNAME);

        createTableTxTest(
                DBNAME, TABLENAME_USER1, keyObjectUserSchema, recordObjectUserSchema);
        createTableTxTest(
                DBNAME, TABLENAME_USER2, keyObjectUserSchema, recordObjectUserSchema);
        createTableTxTest(
                DBNAME, TABLENAME_ASSET1, keyObjectAssetSchema, recordObjectAssetSchema);
        createTableTxTest(
                DBNAME, TABLENAME_ASSET2, keyObjectAssetSchema, recordObjectAssetSchema);

        insertTxTest(DBNAME, TABLENAME_USER1, keyObjectUser1, recordObjectUser1);
        insertTxTest(DBNAME, TABLENAME_USER2, keyObjectUser2, recordObjectUser2);
        insertTxTest(DBNAME, TABLENAME_ASSET1, keyObjectAsset1, recordObjectAsset1);
        insertTxTest(DBNAME, TABLENAME_ASSET1, keyObjectAsset2, recordObjectAsset2);

        assert deleteRecordWithKeyTxTest(DBNAME, TABLENAME_USER1, keyObjectUser1)
                && deleteRecordWithKeyTxTest(DBNAME, TABLENAME_USER2, keyObjectUser2)
                && deleteRecordWithKeyTxTest(DBNAME, TABLENAME_ASSET1, keyObjectAsset1)
                && deleteRecordWithKeyTxTest(DBNAME, TABLENAME_ASSET1, keyObjectAsset2);
        assertNull(assetContract.state.getAssetState(DBNAME, TABLENAME_USER1, keyObjectUser1));
        assertNull(assetContract.state.getAssetState(DBNAME, TABLENAME_USER2, keyObjectUser2));
        assertNull(
                assetContract.state.getAssetState(DBNAME, TABLENAME_ASSET1, keyObjectAsset1));
        assertNull(
                assetContract.state.getAssetState(DBNAME, TABLENAME_ASSET1, keyObjectAsset2));
        log.info(assetContract.state.get(DBNAME).toString());
        log.info(assetContract.state.getAssetState(DBNAME, TABLENAME_USER1).toString());
        log.info(assetContract.state.getAssetState(DBNAME, TABLENAME_USER2).toString());
        log.info(assetContract.state.getAssetState(DBNAME, TABLENAME_ASSET1).toString());
    }
}

