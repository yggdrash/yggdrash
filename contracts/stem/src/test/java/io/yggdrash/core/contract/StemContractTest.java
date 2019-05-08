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

import com.google.gson.JsonObject;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.ContractUtils;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.common.utils.SerializationUtil;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptImpl;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class StemContractTest {
    private static final Logger log = LoggerFactory.getLogger(StemContractTest.class);
    private static final StemContract.StemService stemContract = new StemContract.StemService();

    private static final File branchFile = new File("../../yggdrash-core/src/main/resources/branch-yggdrash.json");

    private Field txReceiptField;
    TestYeed testYeed = new TestYeed();

    JsonObject branchSample;

    @Before
    public void setUp() throws IllegalAccessException, IOException {
        // Steup StemContract
        StateStore stateStore = new StateStore(new HashMapDbSource());

        List<Field> txReceipt = ContractUtils.txReceiptFields(stemContract);
        if (txReceipt.size() == 1) {
            txReceiptField = txReceipt.get(0);
        }
        for (Field f : ContractUtils.contractFields(stemContract, ContractStateStore.class)) {
            f.setAccessible(true);
            f.set(stemContract, stateStore);
        }
        // 1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e
        JsonObject obj = new JsonObject();
        obj.addProperty("address", "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e");
        assertTrue("setup", BigInteger.ZERO.compareTo(testYeed.balanceOf(obj)) < 0);

        try (InputStream is = new FileInputStream(branchFile)) {
            String branchString = IOUtils.toString(is, FileUtil.DEFAULT_CHARSET);
            branchSample = JsonUtil.parseJsonObject(branchString);
        }
        log.debug(branchSample.toString());
        byte[] serializeByte = SerializationUtil.serializeString(branchSample.toString());
        byte[] sha3omit12 = HashUtil.sha3omit12(serializeByte);
        log.debug(HexUtil.toHexString(sha3omit12));
    }

    @Test
    public void createStemBranch() {
        // Set Receipt
        TransactionReceipt receipt = createReceipt();
        setUpReceipt(receipt);

        // add params
        JsonObject param = new JsonObject();
        // TODO param add branch and fee
        // Get Branch sample in resources

        param.add("branch", branchSample);
        param.addProperty("fee", BigInteger.valueOf(1000000));

        log.debug(JsonUtil.prettyFormat(param));

        stemContract.create(param);

        log.debug(receipt.getStatus().toString());

    }


//    private StemContractStateValue stateValue;
//
//    @Before
//    public void setUp() throws IllegalAccessException {
//        stateStore = new StateStore(new HashMapDbSource());
//
//        JsonObject params = ContractTestUtils.createSampleBranchJson();
//        stateValue = StemContractStateValue.of(params);
//        TransactionReceipt receipt = createReceipt();
//        List<Field> txReceipt = ContractUtils.txReceiptFields(stemContract);
//        if (txReceipt.size() == 1) {
//            txReceiptField = txReceipt.get(0);
//        }
//        for (Field f : ContractUtils.contractFields(stemContract, ContractStateStore.class)) {
//            f.setAccessible(true);
//            f.set(stemContract, stateStore);
//        }
//
//        try {
//            txReceiptField.set(stemContract, receipt);
//            stemContract.init(params);
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void getBranchListTest() {
//        Set<String> branchIdList = stemContract.getBranchIdList();
//        if (!branchIdList.isEmpty()) {
//            Assertions.assertThat(branchIdList).containsOnly(stateValue.getBranchId().toString());
//        }
//    }
//
//    @Test
//    public void createTest() {
//        String description = "ETH TO YEED";
//        JsonObject params = getEthToYeedBranch(description);
//        TransactionReceipt receipt = createReceipt();
//
//        try {
//            txReceiptField.set(stemContract, receipt);
//            stemContract.create(params);
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
//
//        Assertions.assertThat(receipt.isSuccess()).isTrue();
//
//        BranchId branchId = Branch.of(params).getBranchId();
//        JsonObject saved = stateStore.get(branchId.toString());
//        Assertions.assertThat(saved).isNotNull();
//        Assertions.assertThat(saved.get("description").getAsString()).isEqualTo(description);
//    }
//
//    @Test
//    public void updateTest() {
//        JsonObject params = createUpdateParams();
//        TransactionReceipt receipt = createReceipt();
//
//        try {
//            txReceiptField.set(stemContract, receipt);
//            receipt = stemContract.update(params);
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
//
//        assertTrue(receipt.isSuccess());
//        /* ========================================================= */
//
//        JsonObject params2 = createUpdateParams2();
//        TransactionReceipt receipt2 = createReceipt();
//
//        try {
//            txReceiptField.set(stemContract, receipt);
//            receipt2 = stemContract.update(params2);
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
//
//        assertTrue(receipt2.isSuccess());
//    }
//
//    private JsonObject createUpdateParams() {
//        JsonObject params = new JsonObject();
//        params.addProperty(BRANCH_ID, stateValue.getBranchId().toString());
//        params.addProperty("fee", BigDecimal.valueOf(1000));
//        return params;
//    }
//
//    private JsonObject createUpdateParams2() {
//        JsonObject params = new JsonObject();
//        params.addProperty(BRANCH_ID, stateValue.getBranchId().toString());
//        params.addProperty("fee", BigDecimal.valueOf(2000));
//        return params;
//    }
//
//    private static JsonObject getEthToYeedBranch(String description) {
//        // TODO get branch from resource
//
//        String name = "Ethereum TO YEED";
//        String symbol = "ETH TO YEED";
//        String property = "exchange";
//        String timeStamp = "00000166c837f0c9";
//
//        String consensusString = new StringBuilder()
//                .append("{\"consensus\": {\n")
//                .append("    \"algorithm\": \"pbft\",\n")
//                .append("    \"period\": \"* * * * * *\"\n")
//                .append("   \n}")
//                .append("  }").toString();
//        JsonObject consensus = new Gson().fromJson(consensusString, JsonObject.class);
//
//        JsonObject branchJson = BranchBuilder.builder()
//                                .setName(name)
//                                .setDescription(description)
//                                .setSymbol(symbol)
//                                .setProperty(property)
//                                .setTimeStamp(timeStamp)
//                                .setConsensus(consensus)
//                                .buildJson();
//
//        branchJson.addProperty("fee", BigInteger.valueOf(100));
//        return branchJson;
//    }
//
    private TransactionReceipt createReceipt() {
        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setIssuer("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        return receipt;
    }

    private void setUpReceipt(TransactionReceipt receipt) {
        try {
            txReceiptField.set(stemContract, receipt);
            testYeed.setTxReceipt(receipt);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}