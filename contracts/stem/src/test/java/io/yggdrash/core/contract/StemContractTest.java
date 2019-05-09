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
import io.yggdrash.common.contract.vo.PrefixKeyEnum;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.BranchUtil;
import io.yggdrash.common.utils.ContractUtils;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.common.utils.SerializationUtil;
import io.yggdrash.contract.core.ExecuteStatus;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StemContractTest {
    private static final Logger log = LoggerFactory.getLogger(StemContractTest.class);
    private static final StemContract.StemService stemContract = new StemContract.StemService();

    private static final File branchFile = new File("../../yggdrash-core/src/main/resources/branch-yggdrash.json");

    private Field txReceiptField;
    TestYeed testYeed = new TestYeed();

    JsonObject branchSample;
    String branchId;
    StateStore stateStore;

    @Before
    public void setUp() throws IllegalAccessException, IOException {
        // Steup StemContract
        stateStore = new StateStore(new HashMapDbSource());

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
        // branch Id generator to util
        byte[] rawBranchId = BranchUtil.branchIdGenerator(branchSample);
        branchId = HexUtil.toHexString(rawBranchId);
        log.debug("Branch Id : {}", branchId);
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


        stemContract.create(param);

        String branchKey = String.format("%s%s", PrefixKeyEnum.STEM_BRANCH, branchId);
        String branchMetaKey = String.format("%s%s", PrefixKeyEnum.STEM_META, branchId);
        assertTrue("Branch Stored", stateStore.contains(branchKey));
        assertTrue("Branch Meta Stored", stateStore.contains(branchMetaKey));
        assertTrue("Branch Create Success", receipt.isSuccess());
    }

    @Test
    public void getBranchQuery() {
        createStemBranch();

        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId);


        JsonObject branch = stemContract.getBranch(param);

        byte[] rawBranchId = BranchUtil.branchIdGenerator(branch);
        String queryBranchId = HexUtil.toHexString(rawBranchId);
        assertEquals("branch Id check", branchId, queryBranchId);
    }

    @Test
    public void updateBranchMetaInformation() {
        // all meta information is not delete by transaction
        createStemBranch();
        // Set new Receipt
        TransactionReceipt receipt = createReceipt();
        setUpReceipt(receipt);

        JsonObject branchUpdate = new JsonObject();
        branchUpdate.addProperty("name", "NOT UPDATE");
        branchUpdate.addProperty("description", "UPDATE DESCRIPTION");


        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId);
        param.add("branch", branchUpdate);
        param.addProperty("fee", BigInteger.valueOf(1000000));

        stemContract.update(param);


        JsonObject metaInfo = stemContract.getBranchMeta(param);

        assertEquals("", metaInfo.get("name").getAsString(), "YGGDRASH");
        assertEquals("", metaInfo.get("description").getAsString(), "UPDATE DESCRIPTION");
    }

    @Test
    public void updateNotExistBranch() {
        TransactionReceipt receipt = createReceipt();
        setUpReceipt(receipt);
        JsonObject branchUpdate = new JsonObject();
        branchUpdate.addProperty("name", "NOT UPDATE");
        branchUpdate.addProperty("description", "UPDATE DESCRIPTION");


        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId);
        param.add("branch", branchUpdate);
        param.addProperty("fee", BigInteger.valueOf(1000000));

        stemContract.update(param);

        assertEquals("transaction update is False", receipt.getStatus(), ExecuteStatus.FALSE);
    }

    @Test
    public void metaDataMerge() {

        JsonObject metaSample = new JsonObject();
        metaSample.addProperty("name", "YGGDRASH");
        metaSample.addProperty("symbol", "YGGDRASH");
        metaSample.addProperty("property", "platform");
        metaSample.addProperty("description", "TRUST-based Multi-dimensional Blockchains");


        JsonObject metaUpdate = new JsonObject();
        metaUpdate.addProperty("name", "NOT UPDATE");
        metaUpdate.addProperty("symbol", "NOT UPDATE");
        metaUpdate.addProperty("description", "UPDATE DESCRIPTION");

        JsonObject metaUpdated = stemContract.metaMerge(metaSample, metaUpdate);


        assertEquals("Name is not update",
                metaUpdated.get("name").getAsString(), "YGGDRASH");
        assertEquals("Symbol is not update",
                metaUpdated.get("symbol").getAsString(), "YGGDRASH");
        assertEquals("Property is not update",
                metaUpdated.get("property").getAsString(), "platform");
        assertEquals("description is Update",
                metaUpdated.get("description").getAsString(), "UPDATE DESCRIPTION");


    }

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