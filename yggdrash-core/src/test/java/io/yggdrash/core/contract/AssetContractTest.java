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


public class AssetContractTest {

    private static final Logger log = LoggerFactory.getLogger(AssetContractTest.class);

    private AssetContract assetContract;

    private byte[] branchId = Hex.decode("00fbbccfacd572b102351961c048104f4f21a008");

    private DefaultConfig defaultConfig;
    private Wallet wallet;

    public AssetContractTest() throws IOException, InvalidCipherTextException {
        defaultConfig = new DefaultConfig();
        wallet = new Wallet(defaultConfig);
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

    @Test
    public void createDatabaseModuleTest() {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "createDatabase");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", "assetManagement");

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonArray txBodyArray = new JsonArray();
        txBodyArray.add(bodyObject);

        log.info(txBodyArray.toString());

        // Module Test
        TransactionReceipt receipt = assetContract.createDatabase(paramsArray);
        log.info(receipt.toString());
        if ((receipt.getStatus() != 1)) {
            throw new AssertionError();
        }

    }

    @Test
    public void createDatabaseTest() {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "createDatabase");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", "assetManagement");

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonArray txBodyArray = new JsonArray();
        txBodyArray.add(bodyObject);

        log.info(txBodyArray.toString());

        // Transaction Test
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

    private boolean createDatabaseTest(String dbName) {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "createDatabase");

        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonArray txBodyArray = new JsonArray();
        txBodyArray.add(bodyObject);

        log.info(txBodyArray.toString());

        // Transaction Test
        TransactionBody txBody = new TransactionBody(txBodyArray);
        TransactionHeader txHeader = new TransactionHeader(
                branchId,
                new byte[8],
                new byte[8],
                TimeUtils.time(),
                txBody);
        TransactionSignature txSig = new TransactionSignature(wallet, txHeader.getHashForSigning());
        Transaction tx = new Transaction(txHeader, txSig.getSignature(), txBody);

        return assetContract.invoke(new TransactionHusk(tx));
    }

    @Test
    public void createTableUserModuleTest() {

        createDatabaseTest();

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "createTable");

        // paramsObject
        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", "assetManagement");
        paramsObject.addProperty("table", "user");

        // keyObject
        JsonObject keyObject = new JsonObject();
        keyObject.addProperty("number", "1");
        paramsObject.add("key", keyObject);

        // recordObject
        JsonObject recordObject = new JsonObject();
        recordObject.addProperty("name", "");
        recordObject.addProperty("gender", "");
        recordObject.addProperty("age", "");
        recordObject.addProperty("department", "");

        paramsObject.add("record", recordObject);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonArray txBodyArray = new JsonArray();
        txBodyArray.add(bodyObject);

        log.info(txBodyArray.toString());

        // Module Test
        TransactionReceipt receipt = assetContract.createTable(paramsArray);
        log.info(receipt.toString());
        if ((receipt.getStatus() != 1)) {
            throw new AssertionError();
        }
    }

    private void createTableUserTest() {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "createTable");

        // paramsObject
        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", "assetManagement");
        paramsObject.addProperty("table", "user");

        // keyObject
        JsonObject keyObject = new JsonObject();
        keyObject.addProperty("number", "1");
        paramsObject.add("key", keyObject);

        // recordObject
        JsonObject recordObject = new JsonObject();
        recordObject.addProperty("name", "");
        recordObject.addProperty("gender", "");
        recordObject.addProperty("age", "");
        recordObject.addProperty("department", "");

        paramsObject.add("record", recordObject);

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

    private void createTableUserTest(String dbName, String tableName) {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "createTable");

        // paramsObject
        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName);

        // keyObject
        JsonObject keyObject = new JsonObject();
        keyObject.addProperty("number", "1");
        paramsObject.add("key", keyObject);

        // recordObject
        JsonObject recordObject = new JsonObject();
        recordObject.addProperty("name", "");
        recordObject.addProperty("gender", "");
        recordObject.addProperty("age", "");
        recordObject.addProperty("department", "");

        paramsObject.add("record", recordObject);

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

    private void createTableUserTest(
            String dbName, String tableName, JsonObject keyObject, JsonObject recordObject) {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "createTable");

        // paramsObject
        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName);

        // keyObject
        paramsObject.add("key", keyObject);

        // recordObject
        paramsObject.add("record", recordObject);

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

    private void createTableAssetTest() {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "createTable");

        // paramsObject
        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", "assetManagement");
        paramsObject.addProperty("table", "asset");

        // keyObject
        JsonObject keyObject = new JsonObject();
        keyObject.addProperty("number", "1");
        keyObject.addProperty("code", "0000-0000");
        paramsObject.add("key", keyObject);

        // recordObject
        JsonObject recordObject = new JsonObject();
        recordObject.addProperty("type", "");
        recordObject.addProperty("information", "");
        recordObject.addProperty("price", "0");
        recordObject.addProperty("registrationDate", "0000-00-0000");
        recordObject.addProperty("disuseDate", "0000-00-0000");

        JsonObject userObject = new JsonObject();
        userObject.addProperty("table", "user");

        JsonObject userkeyObject = new JsonObject();
        userkeyObject.addProperty("number", "1");

        userObject.add("key", userkeyObject);

        recordObject.add("user", userObject);

        paramsObject.add("record", recordObject);

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


    @Test
    public void createTableTest() {
        createDatabaseTest();
        createTableUserTest();
        createTableAssetTest();
    }

    @Test
    public void insertRecordUserModuleTest() {

        createDatabaseTest();
        createTableUserTest();

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "insert");

        // paramsObject
        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", "assetManagement");
        paramsObject.addProperty("table", "user");

        // keyObject
        JsonObject keyObject = new JsonObject();
        keyObject.addProperty("number", "1");
        paramsObject.add("key", keyObject);

        // recordObject
        JsonObject recordObject = new JsonObject();
        recordObject.addProperty("name", "Jaes Park");
        recordObject.addProperty("gender", "mail");
        recordObject.addProperty("age", "20");
        recordObject.addProperty("department", "development");

        paramsObject.add("record", recordObject);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonArray txBodyArray = new JsonArray();
        txBodyArray.add(bodyObject);

        log.info(txBodyArray.toString());

        // Module Test
        TransactionReceipt receipt = assetContract.insert(paramsArray);
        log.info(receipt.toString());
        if ((receipt.getStatus() != 1)) {
            throw new AssertionError();
        }
    }

    private void insertRecordUserTest1() {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "insert");

        // paramsObject
        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", "assetManagement");
        paramsObject.addProperty("table", "user");

        // keyObject
        JsonObject keyObject = new JsonObject();
        keyObject.addProperty("number", "1");
        paramsObject.add("key", keyObject);

        // recordObject
        JsonObject recordObject = new JsonObject();
        recordObject.addProperty("name", "Jaes Park");
        recordObject.addProperty("gender", "mail");
        recordObject.addProperty("age", "20");
        recordObject.addProperty("department", "development");

        paramsObject.add("record", recordObject);

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

    private void insertRecordUserTest2() {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "insert");

        // paramsObject
        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", "assetManagement");
        paramsObject.addProperty("table", "user");

        // keyObject
        JsonObject keyObject = new JsonObject();
        keyObject.addProperty("number", "2");
        paramsObject.add("key", keyObject);

        // recordObject
        JsonObject recordObject = new JsonObject();
        recordObject.addProperty("name", "Rachael Hong");
        recordObject.addProperty("gender", "femail");
        recordObject.addProperty("age", "21");
        recordObject.addProperty("department", "development");

        paramsObject.add("record", recordObject);

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

    private void insertRecordUserTest(
            String dbName, String tableName, JsonObject keyObject, JsonObject recordObject) {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "insert");

        // paramsObject
        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", dbName);
        paramsObject.addProperty("table", tableName);

        // keyObject
        paramsObject.add("key", keyObject);

        // recordObject
        paramsObject.add("record", recordObject);

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

    @Test
    public void insertUserRecordTest() {
        createDatabaseTest();
        createTableUserTest();
        insertRecordUserTest1();
        insertRecordUserTest2();

        log.info(assetContract.state.getAssetState("assetManagement", "user").toString());
    }

    @Test
    public void updateRecordUserModuleTest() {

        createDatabaseTest();
        createTableUserTest();
        insertRecordUserTest1();

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "update");

        // paramsObject
        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", "assetManagement");
        paramsObject.addProperty("table", "user");

        // keyObject
        JsonObject keyObject = new JsonObject();
        keyObject.addProperty("number", "1");
        paramsObject.add("key", keyObject);

        // recordObject
        JsonObject recordObject = new JsonObject();
        recordObject.addProperty("name", "Jaes Park");
        recordObject.addProperty("gender", "mail");
        recordObject.addProperty("age", "22");
        recordObject.addProperty("department", "development");

        paramsObject.add("record", recordObject);

        JsonArray paramsArray = new JsonArray();
        paramsArray.add(paramsObject);

        bodyObject.add("params", paramsArray);

        JsonArray txBodyArray = new JsonArray();
        txBodyArray.add(bodyObject);

        log.info(txBodyArray.toString());

        // Module Test
        TransactionReceipt receipt = assetContract.update(paramsArray);
        log.info(receipt.toString());
        if ((receipt.getStatus() != 1)) {
            throw new AssertionError();
        }
    }

    private void updateRecordUserTest1() {

        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "update");

        // paramsObject
        JsonObject paramsObject = new JsonObject();
        paramsObject.addProperty("db", "assetManagement");
        paramsObject.addProperty("table", "user");

        // keyObject
        JsonObject keyObject = new JsonObject();
        keyObject.addProperty("number", "1");
        paramsObject.add("key", keyObject);

        // recordObject
        JsonObject recordObject = new JsonObject();
        recordObject.addProperty("name", "Jaes Park");
        recordObject.addProperty("gender", "mail");
        recordObject.addProperty("age", "22");
        recordObject.addProperty("department", "development");

        paramsObject.add("record", recordObject);

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

    @Test
    public void updateUserRecordTest() {
        createDatabaseTest();
        createTableUserTest();
        insertRecordUserTest1();
        updateRecordUserTest1();

        log.info(assetContract.state.getAssetState("assetManagement", "user").toString());
    }

    @Test
    public void queryAllDatabasesModuleTest() {
        createDatabaseTest();
        createDatabaseTest("testDb");

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
        createDatabaseTest("testdb1");
        createDatabaseTest("testDb2");

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
        createDatabaseTest("assetManagement");
        createTableUserTest("assetManagement", "user1");
        createTableUserTest("assetManagement", "user2");

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
        createDatabaseTest("assetManagement");
        createTableUserTest("assetManagement", "user1");
        createTableUserTest("assetManagement", "user2");

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
        createDatabaseTest("assetManagement");
        createTableUserTest("assetManagement", "user1");
        createTableUserTest("assetManagement", "user2");

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

        createDatabaseTest(dbName);
        createTableUserTest(dbName, tableName1, keyObject1, recordObject1);
        createTableUserTest(dbName, tableName2, keyObject2, recordObject2);

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

        createDatabaseTest(dbName);
        createTableUserTest(dbName, tableName);
        insertRecordUserTest(dbName, tableName, keyObject1, recordObject1);
        insertRecordUserTest(dbName, tableName, keyObject2, recordObject2);

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

        createDatabaseTest(dbName);
        createTableUserTest(dbName, tableName);
        insertRecordUserTest(dbName, tableName, keyObject1, recordObject1);
        insertRecordUserTest(dbName, tableName, keyObject2, recordObject2);

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

        createDatabaseTest(dbName);
        createTableUserTest(dbName, tableName1, keyObject1, recordObject1);
        insertRecordUserTest(dbName, tableName1, keyObject1, recordObject1);
        insertRecordUserTest(dbName, tableName1, keyObject2, recordObject2);

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


}

