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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.store.BranchStateStore;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.ReceiptAdapter;
import io.yggdrash.contract.core.ReceiptImpl;
import io.yggdrash.contract.yeed.ehtereum.EthTransaction;
import io.yggdrash.contract.yeed.intertransfer.TxConfirmStatus;
import io.yggdrash.contract.yeed.propose.ProposeStatus;
import io.yggdrash.contract.yeed.propose.ProposeType;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static BigInteger BASE_CURRENCY = BigInteger.TEN.pow(18);
    // 0.01 YEED
    private static BigInteger DEFAULT_FEE = BASE_CURRENCY.divide(BigInteger.valueOf(100L));

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
    }

    @Test
    public void allowance() {
        BigInteger res = getAllowance(ADDRESS_1, ADDRESS_2);
        BigInteger res2 = getAllowance(ADDRESS_1.toUpperCase(), ADDRESS_2.toUpperCase());

        assertEquals(BigInteger.ZERO, res);
        assertEquals(res2, res);
    }

    @Test
    public void transfer() {

        BigInteger sendAmount = BASE_CURRENCY; // 1 YEED
        final BigInteger sendAddress = getBalance(ADDRESS_1);
        final BigInteger receiveAmount = getBalance(ADDRESS_2);
        BigInteger feeAmount = DEFAULT_FEE;

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
        BigInteger receverRemainAmount = receiveAmount.add(sendAmount);

        assertEquals(senderRemainAmount, getBalance(ADDRESS_1));
        assertEquals(receverRemainAmount, getBalance(ADDRESS_2));

        // To much amount
        addAmount(paramObj, BigInteger.valueOf(1).add(senderRemainAmount));
        receipt = yeedContract.transfer(paramObj);

        assertFalse(receipt.isSuccess());

        // Same amount
        addAmount(paramObj, senderRemainAmount.subtract(feeAmount));
        receipt = yeedContract.transfer(paramObj);

        assertTrue(receipt.isSuccess());
        assertTrue(getBalance(ADDRESS_1).compareTo(BigInteger.ZERO) == 0);
    }

    @Test
    public void failTransfer() {
        JsonObject paramObj = new JsonObject();
        paramObj.addProperty("to", "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e");
        paramObj.addProperty("amount", -1000000);
        paramObj.addProperty("fee", -1000);

        Receipt receipt = setUpReceipt(ADDRESS_1);

        receipt = yeedContract.transfer(paramObj);

        assertFalse(receipt.isSuccess());
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

        // CHECK allowence
        JsonObject param = new JsonObject();
        param.addProperty("owner", owner);
        param.addProperty("spender", spender);
        BigInteger allowanceBalance = yeedContract.allowance(param);

        Assert.assertTrue("allowanceBalance : ",
                allowanceBalance.compareTo(approveBalance) == 0);


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
    public void sendSameAccount() {
        String issuer = "4d01e237570022440aa126ca0b63065d7f5fd589";
        final BigInteger balance = yeedContract.getBalance(issuer);
        setUpReceipt("0x00", issuer, BRANCH_ID, 1);

        JsonObject testTransfer = new JsonObject();
        testTransfer.addProperty("to","4d01e237570022440aa126ca0b63065d7f5fd589");
        testTransfer.addProperty("amount", BigInteger.valueOf(100L));
        yeedContract.transfer(testTransfer);

        BigInteger after = yeedContract.getBalance(issuer);

        assertTrue(balance.compareTo(after) == 0);
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
