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

package io.yggdrash.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.vo.PrefixKeyEnum;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.BranchUtil;
import io.yggdrash.common.utils.ContractUtils;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptAdapter;
import io.yggdrash.contract.core.TransactionReceiptImpl;
import io.yggdrash.core.blockchain.osgi.ContractCache;
import io.yggdrash.core.blockchain.osgi.ContractCacheImpl;
import io.yggdrash.core.blockchain.osgi.ContractChannelCoupler;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StemContractTest {
    private static final Logger log = LoggerFactory.getLogger(StemContractTest.class);
    private static final StemContract.StemService stemContract = new StemContract.StemService();

    private static final File branchFile
            = new File("../../yggdrash-core/src/main/resources/branch-yggdrash-sample.json");

    private TransactionReceiptAdapter adapter;
    TestYeed testYeed;

    JsonObject branchSample;
    String branchId;
    StateStore stateStore;

    ContractCache cache;
    Map<String, Object> contractMap = new HashMap<>();
    ContractChannelCoupler coupler;

    @Before
    public void setUp() throws IllegalAccessException, IOException {
        // Setup StemContract
        stateStore = new StateStore(new HashMapDbSource());
        adapter = new TransactionReceiptAdapter();
        testYeed = new TestYeed();

        stemContract.txReceipt = adapter;
        testYeed.setTxReceipt(adapter);

        stemContract.state = stateStore;

        // 1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e
        JsonObject obj = new JsonObject();
        obj.addProperty("address", "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e");
        assertTrue("setup", BigInteger.ZERO.compareTo(testYeed.balanceOf(obj)) < 0);

        TestBranchStateStore branchStateStore = new TestBranchStateStore();
        branchStateStore.getValidators().getValidatorMap().put("81b7e08f65bdf5648606c89998a9cc8164397647",
                new Validator("81b7e08f65bdf5648606c89998a9cc8164397647"));
        stemContract.branchStateStore = branchStateStore;

        // Get Branch sample in resources
        try (InputStream is = new FileInputStream(branchFile)) {
            String branchString = IOUtils.toString(is, FileUtil.DEFAULT_CHARSET);
            branchSample = JsonUtil.parseJsonObject(branchString);
        }
        // branch Id generator to util
        byte[] rawBranchId = BranchUtil.branchIdGenerator(branchSample);
        branchId = HexUtil.toHexString(rawBranchId);
        log.debug("Branch Id : {}", branchId);

        // ADD contract coupler
        coupler = new ContractChannelCoupler();
        cache = new ContractCacheImpl();
        coupler.setContract(contractMap, cache);

        //for (Field f : ContractUtils.contractFields(stemContract, ContractChannelField.class)) {
        for (Field f : ContractUtils.contractChannelField(stemContract)) {
            f.setAccessible(true);
            f.set(stemContract, coupler);
        }

        cache.cacheContract("STEM", stemContract);
        cache.cacheContract("YEED", testYeed);

        contractMap.put("STEM", stemContract);
        contractMap.put("YEED", testYeed);
    }

    @Test
    public void verifyBalanceFailed() {
        String noBalanceAddress = "b0aee21c81bf6057efa9a321916f0f1a12f5c547";
        TransactionReceipt receipt = createReceipt(noBalanceAddress);
        setUpReceipt(receipt);

        assertEquals("0", queryBalanceOf(receipt.getIssuer()).toString());

        stemContract.create(getParamForCreateBranch());

        assertFalse(receipt.isSuccess());
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());
        assertEquals(1, receipt.getTxLog().size());
        assertTrue(receipt.getTxLog().contains("Insufficient funds"));
    }

    @Test
    public void createStemBranch() {
        // Set Receipt
        TransactionReceipt receipt = createReceipt();
        setUpReceipt(receipt);

        // Check balance of issuer and stemContract
        assertEquals("100000000000000", queryBalanceOf(receipt.getIssuer()).toString());
        assertEquals("0", queryBalanceOf(createContractAccount("STEM")).toString());

        stemContract.create(getParamForCreateBranch());

        assertTrue("Branch Create Success", receipt.isSuccess());
        receipt.getTxLog().stream().forEach(l -> log.debug(l));

        String branchKey = String.format("%s%s", PrefixKeyEnum.STEM_BRANCH, branchId);
        assertTrue("Branch Stored", stateStore.contains(branchKey));

        String branchMetaKey = String.format("%s%s", PrefixKeyEnum.STEM_META, branchId);
        assertTrue("Branch Meta Stored", stateStore.contains(branchMetaKey));

        // Check fee transferred
        assertEquals("99999999000000", queryBalanceOf(receipt.getIssuer()).toString());
        assertEquals("1000000", queryBalanceOf(createContractAccount("STEM")).toString());

        // Check fee state
        BigInteger feeState = getFeeState();
        log.debug("Current fee state : {}", feeState);
        assertEquals("1000000", feeState.toString());
    }

    @Test
    public void otherBranchCreate() {
        createStemBranch();

        TransactionReceipt receipt = createReceipt();
        setUpReceipt(receipt);

        // Check balance of issuer and stemContract
        assertEquals("99999999000000", queryBalanceOf(receipt.getIssuer()).toString());
        assertEquals("1000000", queryBalanceOf(createContractAccount("STEM")).toString());

        JsonObject otherBranch = branchSample.deepCopy();
        otherBranch.addProperty("name", "ETH TO YEED Branch");
        otherBranch.addProperty("symbol", "ETY");
        otherBranch.addProperty("property", "exchange");
        otherBranch.addProperty("timeStamp", "00000166c837f0c9");

        JsonObject param = new JsonObject();
        param.add("branch", otherBranch);
        param.addProperty("serviceFee", BigInteger.valueOf(10000));

        byte[] rawBranchId = BranchUtil.branchIdGenerator(otherBranch);
        String otherBranchId = HexUtil.toHexString(rawBranchId);

        JsonObject queryParam = new JsonObject();
        queryParam.addProperty("branchId", otherBranchId);

        stemContract.create(param);

        assertEquals("otherBranch Create", ExecuteStatus.SUCCESS, receipt.getStatus());

        JsonObject queryMeta = stemContract.getBranchMeta(queryParam);

        assertEquals("otehr branch symbol", "ETY", queryMeta.get("symbol").getAsString());

        // Check fee transferred
        assertEquals("99999998990000", queryBalanceOf(receipt.getIssuer()).toString());
        assertEquals("1010000", queryBalanceOf(createContractAccount("STEM")).toString());

        // Check fee state
        BigInteger feeState = getFeeState();
        log.debug("Current serviceFee state : {}", feeState);
        assertEquals("1000000", feeState.toString());
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
    public void getContractQuery() {
        createStemBranch();

        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId);

        Set<JsonObject> contracts = stemContract.getContract(param);
        contracts.forEach(c -> log.debug(c.getAsJsonObject().get("contractVersion").getAsString()));

        assertEquals("Contract Size", 3, contracts.size());
    }

    private BigInteger getFeeState() {
        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId);
        return stemContract.feeState(param);
    }

    @Test
    public void updateBranchMetaInformation() {
        // All meta information should not be deleted by a transaction
        createStemBranch();
        // Set new transaction receipt
        TransactionReceipt receipt = createReceipt();
        receipt.setIssuer("101167aaf090581b91c08480f6e559acdd9a3ddd");
        setUpReceipt(receipt);

        JsonObject branchUpdate = new JsonObject();
        branchUpdate.addProperty("name", "NOT UPDATE");
        branchUpdate.addProperty("description", "UPDATE DESCRIPTION");

        JsonObject param = new JsonObject();
        param.addProperty("branchId", branchId);
        param.add("branch", branchUpdate);
        param.addProperty("serviceFee", BigInteger.valueOf(1000000));

        stemContract.update(param);

        assertEquals("update result", ExecuteStatus.SUCCESS, receipt.getStatus());
        JsonObject metaInfo = stemContract.getBranchMeta(param);

        assertEquals("name did not update meta information", "YGGDRASH", metaInfo.get("name").getAsString());
        assertEquals("description is updated", "UPDATE DESCRIPTION", metaInfo.get("description").getAsString());

        // Check fee state
        BigInteger feeState = getFeeState();
        log.debug("Current fee state : {}", feeState);
        assertEquals("2000000", feeState.toString());
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
        param.addProperty("serviceFee", BigInteger.valueOf(1000000));

        stemContract.update(param);

        assertEquals("transaction update is False", ExecuteStatus.ERROR, receipt.getStatus());
        //assertEquals("transaction update is False", ExecuteStatus.FALSE, receipt.getStatus());
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

        assertEquals("Name is not update", "YGGDRASH", metaUpdated.get("name").getAsString());
        assertEquals("Symbol is not update", "YGGDRASH", metaUpdated.get("symbol").getAsString());
        assertEquals("Property is not update", "platform", metaUpdated.get("property").getAsString());
        assertEquals("description is Update", "UPDATE DESCRIPTION", metaUpdated.get("description").getAsString());
    }

    /**
     * Issuer deposits yeed to stemContract
     * Deposit from the account who approved to 'issuer' is also available
     */
    @Test
    public void issuerDepositYeedToStem() {
        createStemBranch();

        assertEquals("1000000", getFeeState().toString());

        TransactionReceipt receipt = createReceipt();
        setUpReceipt(receipt);

        JsonObject param = new JsonObject();
        param.addProperty("amount", BigInteger.valueOf(1000000));

        stemContract.deposit(param);

        assertEquals("2000000", getFeeState().toString());
        assertEquals("2000000", queryContractBalanceOf().toString());
    }

    @Test
    public void depositFromTheAccountWhoApproveToTheIssuer() {
        createStemBranch();

        TransactionReceipt receipt = createReceipt();
        setUpReceipt(receipt);

        String owner = receipt.getIssuer();

        assertEquals("1000000", queryContractBalanceOf().toString());
        assertEquals("99999999000000", queryBalanceOf(owner).toString());

        String spender = "b0aee21c81bf6057efa9a321916f0f1a12f5c547";

        // Transfer the amount of fee the spender needs to use
        JsonObject transferParam = new JsonObject();
        transferParam.addProperty("to", spender);
        transferParam.addProperty("amount", BigInteger.valueOf(50));

        assertTrue(testYeed.transfer(transferParam));
        assertEquals(BigInteger.valueOf(50), queryBalanceOf(spender));
        assertEquals(BigInteger.ZERO, queryAllowance(owner, spender));
        assertEquals("99999998999950", queryBalanceOf(owner).toString());

        // Approve 1000 amount to spender
        BigInteger approveAmount = BigInteger.valueOf(1000);
        BigInteger fee = BigInteger.TEN;
        JsonObject obj2 = new JsonObject();
        obj2.addProperty("amount", approveAmount);
        obj2.addProperty("fee", fee);
        obj2.addProperty("spender", spender);

        assertTrue(testYeed.approve(obj2));
        assertEquals(BigInteger.valueOf(1000), queryAllowance(owner, spender));
        assertEquals("99999998999950", queryBalanceOf(owner).toString());

        // Now the spender is the issuer
        TransactionReceipt spenderReceipt = createReceipt(spender);
        setUpReceipt(spenderReceipt);

        // Deposit the amount of yeed within approved by the issuer
        JsonObject param = new JsonObject();
        param.addProperty("from", owner);
        param.addProperty("amount", BigInteger.valueOf(500));

        stemContract.deposit(param);

        assertEquals(ExecuteStatus.SUCCESS, spenderReceipt.getStatus());
        assertEquals(BigInteger.valueOf(50), queryBalanceOf(spender));
        assertEquals(BigInteger.valueOf(500), queryAllowance(owner, spender));
        assertEquals("1000500", queryContractBalanceOf().toString());
        assertEquals("99999998999450", queryBalanceOf(receipt.getIssuer()).toString());
    }

    @Test
    public void issuerWithdrawYeedFromStem() {
        createStemBranch();

        assertEquals("1000000", getFeeState().toString());

        TransactionReceipt receipt = createReceipt();
        setUpReceipt(receipt);

        JsonObject param = new JsonObject();
        param.addProperty("amount", BigInteger.valueOf(1000));

        stemContract.withdraw(param);

        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());
        assertEquals("999000", getFeeState().toString());
        assertEquals("999000", queryContractBalanceOf().toString());
    }

    private JsonObject getParamForCreateBranch() {
        // Param contains branch and fee
        JsonObject param = new JsonObject();
        param.add("branch", branchSample);
        param.addProperty("serviceFee", BigInteger.valueOf(1000000));

        return param;
    }

    private TransactionReceipt createReceipt() {
        String defaultIssuer = "101167aaf090581b91c08480f6e559acdd9a3ddd";
        return createReceipt(defaultIssuer);
    }

    private TransactionReceipt createReceipt(String issuer) {
        TransactionReceipt receipt = new TransactionReceiptImpl();
        receipt.setIssuer(issuer);
        receipt.setBranchId(branchId);
        receipt.setBlockHeight(100L);
        receipt.setContractVersion("ba6909cb36c786ef876b9c5d69301399346a138f");
        return receipt;
    }

    private void setUpReceipt(TransactionReceipt receipt) {
        adapter.setTransactionReceipt(receipt);
    }

    private String createContractAccount(String contractName) {
        return String.format("%s%s", PrefixKeyEnum.CONTRACT_ACCOUNT, contractName);
    }

    private BigInteger queryContractBalanceOf() {
        JsonObject obj = new JsonObject();
        obj.addProperty("contractName", "STEM");
        return testYeed.getContractBalanceOf(obj);
    }

    private JsonObject createBalanceOfParam(String address) {
        JsonObject obj = new JsonObject();
        obj.addProperty("address", address);
        return obj;
    }

    private BigInteger queryBalanceOf(String address) {
        return testYeed.balanceOf(createBalanceOfParam(address));
    }

    private BigInteger queryAllowance(String owner, String spender) {
        JsonObject obj = new JsonObject();
        obj.addProperty("owner", owner);
        obj.addProperty("spender", spender);
        return testYeed.allowance(obj);
    }
}