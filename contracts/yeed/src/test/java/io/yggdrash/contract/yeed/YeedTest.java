/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.contract.yeed;

import com.google.common.collect.Iterables;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.store.BranchStateStore;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.ReceiptAdapter;
import io.yggdrash.contract.core.ReceiptImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class YeedTest {
    private static final YeedContract.YeedService yeedContract = new YeedContract.YeedService();
    private static final Logger log = LoggerFactory.getLogger(YeedTest.class);

    private static final String ADDRESS_1 = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94"; // validator
    private static final String ADDRESS_2 = "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e"; // validator
    private static final String BRANCH_ID = "0x00";
    private ReceiptAdapter adapter;

    private static BigInteger BASE_CURRENCY = BigInteger.TEN.pow(17);
    // 0.01 YEED
    private static BigInteger DEFAULT_FEE = BASE_CURRENCY.divide(BigInteger.valueOf(10L));

    private JsonObject genesisParams = JsonUtil.parseJsonObject("{\"alloc\": "
            + "{\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\":{\"balance\": \"1000000000\"},"
            + "\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\":{\"balance\": \"1000000000\"},"
            + "\"5e032243d507c743b061ef021e2ec7fcc6d3ab89\":{\"balance\": \"10\"},"
            + "\"cee3d4755e47055b530deeba062c5bd0c17eb00f\":{\"balance\": \"998000000000\"},"
            + "\"c3cf7a283a4415ce3c41f5374934612389334780\":{\"balance\": \"10000000000000000000000\"},"
            + "\"4d01e237570022440aa126ca0b63065d7f5fd589\":{\"balance\": \"10000000000000000000000\"}"
            + "}}");

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        BranchStateStore branchStateStore = branchStateStoreMock();
        branchStateStore.getValidators().getValidatorMap()
                .put("81b7e08f65bdf5648606c89998a9cc8164397647",
                        new Validator("81b7e08f65bdf5648606c89998a9cc8164397647"));

        adapter = new ReceiptAdapter();
        yeedContract.receipt = adapter;
        yeedContract.store = new StateStore(new HashMapDbSource());
        yeedContract.branchStateStore = branchStateStore;

        Receipt result = new ReceiptImpl();
        setUpReceipt(result);

        yeedContract.init(genesisParams);
        assertTrue(result.isSuccess());
    }

    @After
    public void tearDown() {
        printTxLog();
    }

    @Test
    public void totalSupply() {
        JsonObject alloc = genesisParams.getAsJsonObject("alloc");
        BigInteger totalSupply = BigInteger.ZERO;
        for (Map.Entry<String, JsonElement> entry : alloc.entrySet()) {
            BigInteger balance = entry.getValue().getAsJsonObject()
                    .get("balance").getAsBigInteger()
                    .multiply(BASE_CURRENCY)
                    ;
            // apply BASE_CURRENCY
            totalSupply = totalSupply.add(balance);
        }
        BigInteger res = yeedContract.totalSupply();

        assertEquals(totalSupply, res);
    }

    @Test
    public void balanceOf() {
        BigInteger res = getBalance(ADDRESS_1);
        BigInteger res2 = getBalance(ADDRESS_1.toUpperCase());

        assertEquals(BASE_CURRENCY.multiply(BigInteger.valueOf(1000000000)), res);
        assertEquals(res2, res);

        // Invalid Parameter Check
        String anyStr = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut"
                + "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris"
                + "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit"
                + "esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt"
                + "in culpa qui officia deserunt mollit anim id est laborum.";
        JsonObject obj = new JsonObject(); // No Length Check
        obj.addProperty("address", anyStr);
        assertEquals(BigInteger.ZERO, yeedContract.balanceOf(obj));

        obj.addProperty("address", ""); // Empty String
        yeedContract.balanceOf(obj);

        obj.add("address", null); // Null check
        yeedContract.balanceOf(obj);

        JsonObject obj2 = new JsonObject(); // Parameter non-existence check
        obj2.addProperty("Address", anyStr);
        assertEquals(BigInteger.ZERO, yeedContract.balanceOf(obj2));
    }

    @Test
    public void getContractBalanceOf() {
        JsonObject param = new JsonObject();
        param.addProperty("ContractName", "contractName");
        assertEquals(BigInteger.ZERO, yeedContract.getContractBalanceOf(param));

        param.add("contractName", null);
        assertEquals(BigInteger.ZERO, yeedContract.getContractBalanceOf(param));
    }

    @Test
    public void allowance() {
        BigInteger res = getAllowance(ADDRESS_1, ADDRESS_2);
        BigInteger res2 = getAllowance(ADDRESS_1.toUpperCase(), ADDRESS_2.toUpperCase());

        assertEquals(BigInteger.ZERO, res);
        assertEquals(res2, res);

        JsonObject obj = new JsonObject();
        obj.addProperty("owner", ADDRESS_1);
        assertEquals(BigInteger.ZERO, yeedContract.allowance(obj));
        obj.add("owner", null);
        obj.addProperty("spender", ADDRESS_2);
        assertEquals(BigInteger.ZERO, yeedContract.allowance(obj));
        obj.remove("owner");
        assertEquals(BigInteger.ZERO, yeedContract.allowance(obj));
    }

    @Test
    public void queryPropose() {
        JsonObject obj = new JsonObject();
        obj.addProperty("ProposeId", "proposeId");
        assertEquals(0, yeedContract.queryPropose(obj).size());

        obj.add("proposeId", null);
        assertEquals(0, yeedContract.queryPropose(obj).size());

        obj.addProperty("proposeId", "proposeId");
        assertEquals(0, yeedContract.queryPropose(obj).size());
    }

    @Test
    public void queryTransactionConfirm() {
        JsonObject obj = new JsonObject();
        assertEquals(0, yeedContract.queryTransactionConfirm(obj).size());

        obj.addProperty("txConfirmId", "txConfirmId");
        assertEquals(0, yeedContract.queryTransactionConfirm(obj).size());
    }

    @Test
    public void transfer() {
        BigInteger sendAmount = BASE_CURRENCY; // 1 YEED
        final BigInteger sendAddress = getBalance(ADDRESS_1);
        final BigInteger receiveAmount = getBalance(ADDRESS_2);
        BigInteger feeAmount = DEFAULT_FEE; // 0.1 YEED

        JsonObject paramObj = new JsonObject();
        paramObj.addProperty("to", ADDRESS_2);
        paramObj.addProperty("amount", sendAmount);
        paramObj.addProperty("fee", feeAmount);

        log.debug("{}:{}", ADDRESS_1, sendAddress);
        log.debug("{}:{}", ADDRESS_2, receiveAmount);

        // SETUP
        Receipt receipt = setUpReceipt(ADDRESS_1);

        receipt = yeedContract.transfer(paramObj);

        assertTrue(receipt.isSuccess());

        BigInteger senderRemainAmount = sendAddress.subtract(feeAmount).subtract(sendAmount);
        BigInteger receiverRemainAmount = receiveAmount.add(sendAmount);

        assertEquals(senderRemainAmount, getBalance(ADDRESS_1));
        assertEquals(receiverRemainAmount, getBalance(ADDRESS_2));

        // To much amount
        addAmount(paramObj, BigInteger.valueOf(1).add(senderRemainAmount));
        receipt = yeedContract.transfer(paramObj);

        assertFalse(receipt.isSuccess());

        // Same amount
        addAmount(paramObj, senderRemainAmount.subtract(feeAmount));
        receipt = yeedContract.transfer(paramObj);

        assertTrue(receipt.isSuccess());
        assertEquals(0, getBalance(ADDRESS_1).compareTo(BigInteger.ZERO));
    }

    @Test
    public void paramCheckOfTransfer() { // This test also applied to the other @InvokeTransaction methods.
        Receipt receipt = setUpReceipt(ADDRESS_1);

        // non-existence check
        JsonObject obj = new JsonObject();
        receipt = yeedContract.transfer(obj);
        String invalidParamsLog = Iterables.getLast(receipt.getLog());
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("to", "to");
        receipt = yeedContract.transfer(obj);
        assertEquals(invalidParamsLog, Iterables.getLast(receipt.getLog()));
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        // invalid type check
        obj.addProperty("amount", 0.00001);
        receipt = yeedContract.transfer(obj);
        assertEquals(invalidParamsLog, Iterables.getLast(receipt.getLog()));
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("amount", "amount!");
        receipt = yeedContract.transfer(obj);
        assertEquals(invalidParamsLog, Iterables.getLast(receipt.getLog()));
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        // Invalid amount check
        obj.addProperty("amount", 0);
        receipt = yeedContract.transfer(obj);
        String invalidAmountLog = Iterables.getLast(receipt.getLog());
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("amount", -100);
        receipt = yeedContract.transfer(obj);
        assertEquals(invalidAmountLog, Iterables.getLast(receipt.getLog()));
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("amount", -0);
        receipt = yeedContract.transfer(obj);
        assertEquals(invalidAmountLog, Iterables.getLast(receipt.getLog()));
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("amount", 100L);
        receipt = yeedContract.transfer(obj);
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());
        assertTrue(Iterables.getLast(receipt.getLog()).contains(String.valueOf(DEFAULT_FEE)));

        /*
        //No limited amount
        obj.addProperty("amount", BigInteger.TEN.pow(1000000000));
        receipt = yeedContract.transfer(obj);
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());
        */

        // Low Transaction Fee check
        obj.addProperty("fee", 1);

        receipt = yeedContract.transfer(obj);
        String lowTxFee = Iterables.getLast(receipt.getLog());
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("fee", -100);

        receipt = yeedContract.transfer(obj);
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());
        assertEquals(lowTxFee, Iterables.getLast(receipt.getLog()));

        obj.addProperty("fee", DEFAULT_FEE);

        receipt = yeedContract.transfer(obj);
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());

        // Null check
        obj.add("to", null);
        receipt = yeedContract.transfer(obj);
        assertEquals(invalidParamsLog, Iterables.getLast(receipt.getLog()));
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());
    }

    @Test
    public void transferToSelfAccount() {
        Receipt receipt = setUpReceipt(ADDRESS_1);

        BigInteger balanceBeforeTransfer = getBalance(ADDRESS_1);
        BigInteger balanceAfterTransfer = balanceBeforeTransfer.subtract(DEFAULT_FEE);

        JsonObject paramObj = new JsonObject();
        paramObj.addProperty("to", ADDRESS_1);
        paramObj.addProperty("amount", BASE_CURRENCY);
        paramObj.addProperty("fee", DEFAULT_FEE);

        receipt = yeedContract.transfer(paramObj);

        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());
        assertEquals(balanceAfterTransfer, getBalance(ADDRESS_1));
        assertEquals(DEFAULT_FEE, getBalance(receipt.getBranchId()));
    }

    @Test
    public void approveToSelfAccount() {
        Receipt receipt = setUpReceipt(ADDRESS_1);

        BigInteger balanceBeforeApprove = getBalance(ADDRESS_1);
        BigInteger balanceAfterApprove = balanceBeforeApprove.subtract(DEFAULT_FEE);
        BigInteger approveAmount = getBalance(ADDRESS_1).add(BigInteger.TEN);

        JsonObject paramObj = new JsonObject();
        paramObj.addProperty("spender", ADDRESS_1);
        paramObj.addProperty("amount", approveAmount);

        receipt = yeedContract.approve(paramObj);

        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());
        assertEquals(balanceAfterApprove, getBalance(ADDRESS_1));
        assertEquals(approveAmount, getAllowance(ADDRESS_1, ADDRESS_1));

        BigInteger changedApproveAmount = BigInteger.TEN;
        paramObj.addProperty("amount", changedApproveAmount);

        receipt = yeedContract.approve(paramObj);

        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());
        assertEquals(balanceAfterApprove.subtract(DEFAULT_FEE), getBalance(ADDRESS_1));
        assertEquals(changedApproveAmount, getAllowance(ADDRESS_1, ADDRESS_1));
    }

    @Test
    public void transferFromToSelfAccount() {
        Receipt receipt = setUpReceipt(ADDRESS_1);

        BigInteger approveAmount = DEFAULT_FEE.add(BigInteger.valueOf(10000));

        JsonObject approveParam = new JsonObject();
        approveParam.addProperty("spender", ADDRESS_1);
        approveParam.addProperty("amount", approveAmount);
        yeedContract.approve(approveParam);

        BigInteger balanceBeforeTransferFrom = getBalance(ADDRESS_1);
        BigInteger balanceAfterTransferFrom = balanceBeforeTransferFrom.subtract(DEFAULT_FEE);
        BigInteger transferAmount = BigInteger.valueOf(100);

        JsonObject paramObj = new JsonObject();
        paramObj.addProperty("from", ADDRESS_1);
        paramObj.addProperty("to", ADDRESS_1);
        paramObj.addProperty("amount", transferAmount);

        receipt = yeedContract.transferFrom(paramObj);
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());
        assertEquals(balanceAfterTransferFrom, getBalance(ADDRESS_1));
        assertEquals(approveAmount.subtract(transferAmount).subtract(DEFAULT_FEE), getAllowance(ADDRESS_1, ADDRESS_1));
    }

    @Test
    public void paramCheckOfApprove() {
        Receipt receipt = setUpReceipt(ADDRESS_1);

        // non-existence check
        JsonObject obj = new JsonObject();
        receipt = yeedContract.approve(obj);
        String invalidParamsLog = Iterables.getLast(receipt.getLog());
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("spender", "spender");
        receipt = yeedContract.approve(obj);
        assertEquals(invalidParamsLog, Iterables.getLast(receipt.getLog()));
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        // invalid type check
        obj.addProperty("amount", 0.00001);
        receipt = yeedContract.approve(obj);
        assertEquals(invalidParamsLog, Iterables.getLast(receipt.getLog()));
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("amount", -100);
        receipt = yeedContract.approve(obj);
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("amount", 0);
        receipt = yeedContract.approve(obj);
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("amount", "amount!");
        receipt = yeedContract.approve(obj);
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        /*
        //FIXME No limited amount -> SerializationUtil.serializeJson(value) -> The progress of serialization would be freeze.
        obj.addProperty("amount", BigInteger.TEN.pow(10000000));
        receipt = yeedContract.approve(obj);
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());
        assertTrue(Iterables.getLast(receipt.getLog()).contains(String.valueOf(DEFAULT_FEE)));
        */

        obj.addProperty("amount", 100);
        receipt = yeedContract.approve(obj);
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());

        obj.addProperty("fee", 1);

        receipt = yeedContract.approve(obj);
        String lowTxFee = Iterables.getLast(receipt.getLog());
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("fee", -100);

        receipt = yeedContract.approve(obj);
        assertEquals(lowTxFee, Iterables.getLast(receipt.getLog()));
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("fee", DEFAULT_FEE);

        receipt = yeedContract.approve(obj);
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());
    }

    @Test
    public void paramCheckOfTransferFrom() {
        Receipt receipt = setUpReceipt(ADDRESS_1);

        // non-existence check
        JsonObject obj = new JsonObject();
        receipt = yeedContract.transferFrom(obj);
        String invalidParamsLog = Iterables.getLast(receipt.getLog());
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("from", ADDRESS_1);
        obj.addProperty("to", ADDRESS_2);
        receipt = yeedContract.transferFrom(obj);
        assertEquals(invalidParamsLog, Iterables.getLast(receipt.getLog()));
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        // invalid type check
        obj.addProperty("amount", 0.00001);
        receipt = yeedContract.transferFrom(obj);
        assertEquals(invalidParamsLog, Iterables.getLast(receipt.getLog()));
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("amount", -100);
        receipt = yeedContract.transferFrom(obj);
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("amount", 0);
        receipt = yeedContract.transferFrom(obj);
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("amount", "amount!");
        receipt = yeedContract.transferFrom(obj);
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());


        obj.addProperty("amount", 100);
        receipt = yeedContract.transferFrom(obj);
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus()); // Insufficient funds

        obj.addProperty("fee", 1);

        receipt = yeedContract.transferFrom(obj);
        String lowTxFee = Iterables.getLast(receipt.getLog());
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());

        obj.addProperty("fee", -100);

        receipt = yeedContract.transferFrom(obj);
        assertEquals(lowTxFee, Iterables.getLast(receipt.getLog()));
        assertEquals(ExecuteStatus.ERROR, receipt.getStatus());
    }

    @Test
    public void transferFrom() {
        String owner = ADDRESS_1;
        String spender = ADDRESS_2;
        String to = "cee3d4755e47055b530deeba062c5bd0c17eb00f";

        BigInteger approveBalance = BASE_CURRENCY.multiply(BigInteger.valueOf(1000L));
        final BigInteger transferFromBalance = BASE_CURRENCY.multiply(BigInteger.valueOf(700L));
        final BigInteger toBalance = getBalance(to);
        // APPROVE
        approveByOwner(owner, spender, approveBalance.toString());

        // CHECK allowance
        JsonObject param = new JsonObject();
        param.addProperty("owner", owner);
        param.addProperty("spender", spender);
        BigInteger allowanceBalance = yeedContract.allowance(param);

        assertEquals("allowanceBalance : ", 0, allowanceBalance.compareTo(approveBalance));

        JsonObject transferFromObject = new JsonObject();
        transferFromObject.addProperty("from", owner);
        transferFromObject.addProperty("to", to);
        // 700 YEED
        transferFromObject.addProperty("amount", transferFromBalance);
        // Fee is 0.01 YEED
        transferFromObject.addProperty("fee", DEFAULT_FEE);

        Receipt receipt = setUpReceipt(spender);

        // Transfer From owner to receiver by spender
        yeedContract.transferFrom(transferFromObject);

        assertTrue(receipt.isSuccess());
        assertEquals(approveBalance.subtract(transferFromBalance).subtract(DEFAULT_FEE),
                getAllowance(owner, spender));
        assertEquals(toBalance.add(transferFromBalance), getBalance(to));

        String logFormat = "{} : {}";
        log.debug(logFormat, to, getBalance(to));
        log.debug(logFormat, owner, getBalance(owner));
        log.debug(logFormat, spender, getBalance(spender));
        log.debug("getAllowance : {}", getAllowance(owner, spender));
        printTxLog();

        Receipt receipt2 = setUpReceipt("1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e");

        yeedContract.transferFrom(transferFromObject);

        // Insufficient funds
        assertFalse(receipt2.isSuccess());

        allowanceBalance = getAllowance(owner, spender);
        // subtract feeBalance
        allowanceBalance = allowanceBalance.subtract(DEFAULT_FEE);
        addAmount(transferFromObject, allowanceBalance);
        // Transfer all amount of allowance
        yeedContract.transferFrom(transferFromObject);

        assertTrue(receipt2.isSuccess());
        assertEquals(BigInteger.ZERO, getAllowance(owner, spender));
    }

    private void approveByOwner(String owner, String spender, String amount) {
        JsonObject paramObj = new JsonObject();
        paramObj.addProperty("spender", spender);
        paramObj.addProperty("amount", amount);
        paramObj.addProperty("fee", DEFAULT_FEE);

        final BigInteger spenderBalance = getBalance(spender);
        final BigInteger ownerBalance = getBalance(owner);
        Receipt receipt = setUpReceipt(owner);

        yeedContract.approve(paramObj);

        adapter.getLog().stream().forEach(l -> log.debug(l));

        assertTrue(receipt.isSuccess());
        assertEquals(spenderBalance, getBalance(spender));
        assertTrue(ownerBalance.compareTo(getBalance(owner)) > 0);
    }

    @Test
    public void faucetTest() {
        // TODO remove in MainNet
        String issuer = "691af5cbc92d8f4e5683246d27d199ccfa2548d6";

        setUpReceipt("0x00", issuer, BRANCH_ID, 1);

        final JsonObject emptyParam = new JsonObject();
        final BigInteger totalSupply = yeedContract.totalSupply();

        JsonObject testTransfer = new JsonObject();
        testTransfer.addProperty("to","81b7e08f65bdf5648606c89998a9cc8164397647");
        testTransfer.addProperty("amount", BigInteger.valueOf(100L));
        testTransfer.addProperty("fee", DEFAULT_FEE);

        yeedContract.transfer(testTransfer);
        assertSame(ExecuteStatus.ERROR, yeedContract.receipt.getStatus()); // Insufficient funds

        yeedContract.faucet(emptyParam);
        assertSame(ExecuteStatus.SUCCESS, yeedContract.receipt.getStatus());
        assertTrue(yeedContract.totalSupply().compareTo(totalSupply) != 0);

        // Call faucet one more
        yeedContract.faucet(emptyParam);
        assertSame(ExecuteStatus.ERROR, yeedContract.receipt.getStatus()); // Already received or has balance

        yeedContract.transfer(testTransfer);
        assertSame(ExecuteStatus.SUCCESS, yeedContract.receipt.getStatus());
    }

    @Test
    public void transferChannelTest() {
        String from = ADDRESS_1;
        final BigInteger serviceFee = DEFAULT_FEE;
        final BigInteger amount = BASE_CURRENCY.multiply(BigInteger.valueOf(1000L));
        String testContractName = "TEST";

        setUpReceipt("0x00", from, BRANCH_ID, 1);
        JsonObject transferChannelTx = new JsonObject();
        transferChannelTx.addProperty("from", from);
        transferChannelTx.addProperty("to",testContractName);
        transferChannelTx.addProperty("amount", amount);
        transferChannelTx.addProperty("serviceFee", serviceFee);

        BigInteger originBalance = getBalance(from);

        // Deposit Test
        boolean result = yeedContract.transferChannel(transferChannelTx);
        BigInteger depositBalance = getBalance(from);
        assertTrue("Result is True", result);
        assertEquals("", originBalance.subtract(amount).subtract(serviceFee), depositBalance);

        JsonObject param = new JsonObject();
        param.addProperty("contractName", testContractName);
        BigInteger contractBalance = yeedContract.getContractBalanceOf(param);

        assertEquals("Contract Stake Balance", amount, contractBalance);

        // Withdraw Test
        BigInteger withdrawAmount = amount.subtract(serviceFee);
        transferChannelTx.addProperty("from", testContractName);
        transferChannelTx.addProperty("to", from);
        transferChannelTx.addProperty("amount", withdrawAmount);
        log.debug(transferChannelTx.toString());
        result = yeedContract.transferChannel(transferChannelTx);

        assertTrue("Result is True", result);
        BigInteger withdrawBalance = getBalance(from);
        assertEquals("", depositBalance.add(withdrawAmount), withdrawBalance);

        // Contract Balance is zero
        contractBalance = yeedContract.getContractBalanceOf(param);
        assertEquals("Contract Balance is zero", 0, contractBalance.compareTo(BigInteger.ZERO));
    }

    @Test
    public void endblockBurnTest() {
        BigInteger totalSupply = yeedContract.totalSupply();

        Receipt receipt = setUpReceipt(""); // Endblock execute by node
        // Burn network Fee
        yeedContract.endBlock();
        Assert.assertTrue("Network Fee is Zero", receipt.isSuccess());
        Assert.assertTrue("Total Supply is same",
                yeedContract.totalSupply().compareTo(totalSupply) == 0);

        BigInteger sendAmount = BASE_CURRENCY; // 1 YEED
        final BigInteger sendAddress = getBalance(ADDRESS_1);
        final BigInteger receiveAmount = getBalance(ADDRESS_2);
        BigInteger feeAmount = BASE_CURRENCY; // 1YEED

        JsonObject paramObj = new JsonObject();
        paramObj.addProperty("to", ADDRESS_2);
        paramObj.addProperty("amount", sendAmount);
        paramObj.addProperty("fee", feeAmount);


        totalSupply = yeedContract.totalSupply();

        // Transfer YEED
        receipt = setUpReceipt(ADDRESS_1);

        yeedContract.transfer(paramObj);

        Assert.assertTrue("Transfer success", receipt.isSuccess());

        receipt = setUpReceipt(""); // Endblock execute by node
        // Burn network Fee
        yeedContract.endBlock();

        BigInteger burnAfterSupply = yeedContract.totalSupply();
        BigInteger burnedYeed = totalSupply.subtract(burnAfterSupply);
        Assert.assertTrue("Burn is Success", receipt.isSuccess());
        Assert.assertTrue("Burn Network YEED", burnedYeed.compareTo(feeAmount) == 0);

    }


    private BranchStateStore branchStateStoreMock() {
        return new BranchStateStore() {
            ValidatorSet set = new ValidatorSet();
            List<BranchContract> contracts;

            @Override
            public Long getLastExecuteBlockIndex() {
                return null;
            }

            @Override
            public Sha3Hash getLastExecuteBlockHash() {
                return null;
            }

            @Override
            public Sha3Hash getGenesisBlockHash() {
                return null;
            }

            @Override
            public Sha3Hash getBranchIdHash() {
                return null;
            }

            @Override
            public ValidatorSet getValidators() {
                return set;
            }

            @Override
            public boolean isValidator(String address) {
                return true;
            }

            @Override
            public List<BranchContract> getBranchContacts() {
                return contracts;
            }

            @Override
            public String getContractVersion(String contractName) {
                return "0x00";
            }

            @Override
            public String getContractName(String contractVersion) {
                return "TEST";
            }

            public void setValidators(ValidatorSet validatorSet) {
                this.set = validatorSet;
            }

        };
    }

    private Receipt createReceipt(String issuer) {
        Receipt receipt = new ReceiptImpl("0x00", 200L, issuer);
        receipt.setBranchId(BRANCH_ID);

        return receipt;
    }

    private Receipt createReceipt(String transactionId, String issuer, String branchId, long blockHeight) {
        Receipt receipt = new ReceiptImpl(transactionId, 200L, issuer);
        receipt.setBranchId(branchId);
        receipt.setBlockHeight(blockHeight);
        return receipt;
    }

    private void setUpReceipt(Receipt receipt) {
        adapter.setReceipt(receipt);
    }

    private Receipt setUpReceipt(String issuer) {
        Receipt receipt = createReceipt(issuer);
        setUpReceipt(receipt);
        return receipt;
    }

    private Receipt setUpReceipt(String transactionId, String issuer, String branchId, long blockHeight) {
        Receipt receipt = createReceipt(transactionId, issuer, branchId, blockHeight);
        setUpReceipt(receipt);
        return receipt;
    }

    private void addAmount(JsonObject param, BigInteger amount) {
        param.addProperty("amount", amount);
    }

    private BigInteger getBalance(String address) {
        JsonObject obj = new JsonObject();
        obj.addProperty("address", address);
        return yeedContract.balanceOf(obj);
    }

    private BigInteger getAllowance(String owner, String spender) {
        JsonObject obj = new JsonObject();
        obj.addProperty("owner", owner);
        obj.addProperty("spender", spender);
        return yeedContract.allowance(obj);
    }

    private void printTxLog() {
        log.debug("txLog={}", adapter.getLog());
    }
}
