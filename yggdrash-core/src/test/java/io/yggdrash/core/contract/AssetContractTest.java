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
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionBody;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.TransactionSignature;
import io.yggdrash.core.account.Wallet;
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

    private void createTableUserTestModule() {

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

    private void createTableAssetTestModule() {

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
        createTableUserTestModule();
        createTableAssetTestModule();
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

    @Test
    public void insertUserRecordTest() {
        createDatabaseTest();
        createTableUserTestModule();
        insertRecordUserTest1();
        insertRecordUserTest2();

        log.info(assetContract.state.getAssetState("assetManagement", "user").toString());
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
        createTableUserTestModule();
        insertRecordUserTest1();
        updateRecordUserTest1();

        log.info(assetContract.state.getAssetState("assetManagement", "user").toString());
    }
}

