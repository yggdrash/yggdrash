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

import com.fasterxml.jackson.databind.util.JSONPObject;
import com.google.gson.JsonObject;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.ContractUtils;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptImpl;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.yeed.propose.ProposeType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class YeedTest {
    private static final YeedContract.YeedService yeedContract = new YeedContract.YeedService();
    private static final Logger log = LoggerFactory.getLogger(YeedTest.class);

    private static final String ADDRESS_1 = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
    private static final String ADDRESS_2 = "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e";
    private static final String BRANCH_ID = "0x00";
    private static final String ADDRESS_FORMAT = "{\"address\" : \"%s\"}";
    private static final String ADDRESS_JSON_1 = String.format(ADDRESS_FORMAT, ADDRESS_1);
    private static final String ADDRESS_JSON_2 = String.format(ADDRESS_FORMAT, ADDRESS_2);
    private Field txReceiptField;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws IllegalAccessException {
        StateStore coinContractStateStore = new StateStore(new HashMapDbSource());

        List<Field> txReceipt = ContractUtils.txReceiptFields(yeedContract);
        if (txReceipt.size() == 1) {
            txReceiptField = txReceipt.get(0);
        }
        for (Field f : ContractUtils.contractFields(yeedContract, ContractStateStore.class)) {
            f.setAccessible(true);
            f.set(yeedContract, coinContractStateStore);
        }

        genesis();
    }

    private void genesis() {
        String genesisStr = "{\"alloc\": "
                + "{\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\":{\"balance\": \"1000000000\"},"
                + "\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\":{\"balance\": \"1000000000\"},"
                + "\"cee3d4755e47055b530deeba062c5bd0c17eb00f\":{\"balance\": \"998000000000\"},"
                + "\"c3cf7a283a4415ce3c41f5374934612389334780\":{\"balance\": \"10000000000000000000000\"}"
                + "}}";

        TransactionReceipt result = new TransactionReceiptImpl();

        try {
            txReceiptField.set(yeedContract, result);
            yeedContract.init(createParams(genesisStr));
        } catch (IllegalAccessException e) {
            log.warn(e.getMessage());
        }

        assertTrue(result.isSuccess());
        assertEquals(5, result.getTxLog().size());
    }

    @Test
    public void totalSupply() {
        BigInteger res = yeedContract.totalSupply();

        assertEquals(new BigInteger("10000000001000000000000"), res);
    }

    @Test
    public void balanceOf() {
        BigInteger res = yeedContract.balanceOf(createParams(ADDRESS_JSON_1));

        assertEquals(BigInteger.valueOf(1000000000), res);
    }

    @Test
    public void allowance() {
        String paramStr = "{\"owner\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\","
                + "\"spender\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\"}";

        BigInteger res = yeedContract.allowance(createParams(paramStr));

        assertEquals(BigInteger.ZERO, res);
    }

    @Test
    public void transfer() {
        JsonObject paramObj = new JsonObject();
        paramObj.addProperty("to", "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e");
        paramObj.addProperty("amount", 10);
        paramObj.addProperty("fee", 1);

        // tx 가 invoke 되지 않아 baseContract 에 sender 가 세팅되지 않아서 설정해줌
        log.debug("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94:{}", yeedContract.balanceOf(createParams(ADDRESS_JSON_1)));
        log.debug("1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e:{}", yeedContract.balanceOf(createParams(ADDRESS_JSON_2)));



        TransactionReceipt result = new TransactionReceiptImpl();
        result.setIssuer(ADDRESS_1);
        result.setBranchId(BRANCH_ID);
        try {
            txReceiptField.set(yeedContract, result);
            result = yeedContract.transfer(paramObj);
        } catch (IllegalAccessException e) {
            log.warn(e.getMessage());
        }

        assertTrue(result.isSuccess());

        assertEquals(BigInteger.valueOf(999999989), yeedContract.balanceOf(createParams(ADDRESS_JSON_1)));
        assertEquals(BigInteger.valueOf(1000000010), yeedContract.balanceOf(createParams(ADDRESS_JSON_2)));

        // To many amount
        addAmount(paramObj, BigInteger.valueOf(1000000010));
        result = yeedContract.transfer(paramObj);
        assertFalse(result.isSuccess());

        // Same amount
        addAmount(paramObj, BigInteger.valueOf(999999988));
        result = yeedContract.transfer(paramObj);
        assertTrue(result.isSuccess());
    }

    @Test
    public void transferFrom() {
        String owner = ADDRESS_1;
        String spender = ADDRESS_2;
        String to = "cee3d4755e47055b530deeba062c5bd0c17eb00f";

        approveByOwner(to, owner, spender, "1000");

        String transferParams = "{\"from\" : \"" + owner + "\", \"to\" : \"" + to + "\",\"amount\" : \"700\"}";

        JsonObject transferFromObject = createParams(transferParams);

        TransactionReceipt result = new TransactionReceiptImpl();
        result.setBranchId(BRANCH_ID);
        result.setIssuer(spender);
        try {
            txReceiptField.set(yeedContract, result);
            result = yeedContract.transferFrom(transferFromObject);
        } catch (IllegalAccessException e) {
            log.warn(e.getMessage());
        }

        assertTrue(result.isSuccess());
        assertEquals(BigInteger.valueOf(300), getAllowance(owner, spender));
        String logFormat = "{}: {}";
        log.debug(logFormat, to, getBalance(to));
        log.debug(logFormat, owner, getBalance(owner));
        log.debug(logFormat, spender, getBalance(spender));
        log.debug("getAllowance : {}", getAllowance(owner, spender));

        TransactionReceipt result2 = new TransactionReceiptImpl();
        result2.setBranchId(BRANCH_ID);
        try {
            txReceiptField.set(yeedContract, result);
            yeedContract.transferFrom(transferFromObject);
        } catch (IllegalAccessException e) {
            log.warn(e.getMessage());
        }

        // not enough amount allowed
        assertFalse(result2.isSuccess());

        addAmount(transferFromObject, getAllowance(owner, spender));
        result2 = yeedContract.transferFrom(transferFromObject);
        assertTrue(result2.isSuccess());
        // reset
        assertEquals(BigInteger.ZERO, getAllowance(owner, spender));
    }

    private void approveByOwner(String to, String owner, String spender, String amount) {
        JsonObject paramObj = new JsonObject();
        paramObj.addProperty("spender", spender);
        paramObj.addProperty("amount", amount);

        TransactionReceipt result = new TransactionReceiptImpl();
        result.setIssuer(owner);
        try {
            txReceiptField.set(yeedContract, result);
            yeedContract.approve(paramObj);
        } catch (IllegalAccessException e) {
            log.warn(e.getMessage());
        }

        assertTrue(result.isSuccess());

        String spenderParams = String.format(ADDRESS_FORMAT, spender);
        String senderParams = String.format(ADDRESS_FORMAT, owner);

        assertEquals(BigInteger.valueOf(1000000000),
                yeedContract.balanceOf(createParams(spenderParams)));
        assertEquals(BigInteger.valueOf(1000000000),
                yeedContract.balanceOf(createParams(senderParams)));

        assertTransferFrom(to, owner, spender);
    }

    private void assertTransferFrom(String to, String owner, String spender) {

        String allowanceParams = "{\"owner\" : \"" + owner + "\", \"spender\" : \"" + spender + "\"}";
        assertEquals(BigInteger.valueOf(1000), yeedContract.allowance(createParams(allowanceParams)));

        String toParams = String.format(ADDRESS_FORMAT, to);
        assertEquals(BigInteger.valueOf(998000000000L), yeedContract.balanceOf(createParams(toParams)));

        String fromParams = String.format(ADDRESS_FORMAT, owner);
        assertEquals(BigInteger.valueOf(1000000000), yeedContract.balanceOf(createParams(fromParams)));

        String spenderParams = String.format(ADDRESS_FORMAT, spender);
        assertEquals(BigInteger.valueOf(1000000000), yeedContract.balanceOf(createParams(spenderParams)));
    }

    private void addAmount(JsonObject param, BigInteger amount) {
        param.addProperty("amount", amount);
    }

    private JsonObject createParams(String paramStr) {
        return JsonUtil.parseJsonObject(paramStr);
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

    private TransactionReceipt setTxReceipt(String transactionId, String issure, String branchId, long blockHeight) {
        TransactionReceipt result = new TransactionReceiptImpl();
        result.setTxId(transactionId);
        result.setIssuer(issure);
        result.setBranchId(branchId);
        result.setBlockHeight(blockHeight);
        try {
            txReceiptField.set(yeedContract, result);
        } catch (IllegalAccessException e) {
            log.warn(e.getMessage());
        }

        return result;
    }


    @Test
    public void issuePropose() {
        String transactionId = "0x00";
        String receiveAddress = "c3cf7a283a4415ce3c41f5374934612389334780";
        BigInteger receiveEth = new BigInteger("10000000");
        int receiveChainId = 1;
        ProposeType proposeType = ProposeType.YEED_TO_ETHER;

        String senderAddress = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";

        String inputData = null;
        BigInteger stakeYeed = new BigInteger("1000000000000000");
        long targetBlockHeight = 1000000L;
        BigInteger fee = new BigInteger("1000000000");
        String issuer = "c3cf7a283a4415ce3c41f5374934612389334780";

        JsonObject proposal = new JsonObject();
        //proposal.addProperty("transactionId", transactionId);
        proposal.addProperty("receiveAddress", receiveAddress);
        proposal.addProperty("receiveEth", receiveEth);
        proposal.addProperty("receiveChainId", receiveChainId);
        proposal.addProperty("proposeType", proposeType.toValue());
        proposal.addProperty("senderAddress", senderAddress);
        proposal.addProperty("inputData", inputData);
        proposal.addProperty("stakeYeed", stakeYeed);
        proposal.addProperty("blockHeight", targetBlockHeight);
        proposal.addProperty("fee", fee);

        BigInteger issuerOriginBalance = getBalance(issuer);

        TransactionReceipt receipt = setTxReceipt(transactionId, issuer, BRANCH_ID, 1);

        // issue propose
        yeedContract.issuePropose(proposal);

        assert receipt.getStatus().equals(ExecuteStatus.SUCCESS);
        receipt.getTxLog().stream().forEach(l -> log.debug(l));
        String proposeId = "3bc37802368aaf705e8364c4d52194b2a250828aff3c7a41640451db1bafaba2";
        BigInteger balance = getBalance(proposeId);
        BigInteger issuerIssuedBalance = getBalance(issuer);


        log.debug("propose Stake YEED {}", balance.toString());
        log.debug("issuer Origin YEED {}", issuerOriginBalance.toString());
        log.debug("issuer Isssued YEED {}", issuerIssuedBalance.toString());
        assert balance.compareTo(stakeYeed.add(fee)) == 0;
        assert issuerOriginBalance.subtract(stakeYeed.add(fee)).compareTo(issuerIssuedBalance) == 0;


        JsonObject queryProposeParam = new JsonObject();
        queryProposeParam.addProperty("proposeId", proposeId);
        JsonObject queryPropose = yeedContract.queryPropose(queryProposeParam);
        log.debug(queryPropose.toString());

        assert receiveAddress.equals(queryPropose.get("receiveAddress").getAsString());
    }

    @Test
    public void closeIssueFail() {
        // require issuePropose
        issuePropose();
        String transactionId = "0x01";
        String issuer = "d3cf7a283a4415ce3c41f5374934612389334780";
        JsonObject param = new JsonObject();
        String proposeId = "3bc37802368aaf705e8364c4d52194b2a250828aff3c7a41640451db1bafaba2";
        param.addProperty("proposeId", proposeId);
        TransactionReceipt receipt = setTxReceipt(transactionId, issuer, BRANCH_ID, 1000000L);

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("issuer is not propose issuer");
        yeedContract.closePropose(param);
    }

    @Test
    public void closeIssueFail2() {
        // require issuePropose
        issuePropose();
        String transactionId = "0x01";
        String issuer = "c3cf7a283a4415ce3c41f5374934612389334780";
        JsonObject param = new JsonObject();
        String proposeId = "3bc37802368aaf705e8364c4d52194b2a250828aff3c7a41640451db1bafaba2";
        param.addProperty("proposeId", proposeId);
        TransactionReceipt receipt = setTxReceipt(transactionId, issuer, BRANCH_ID, 100L);
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("propose is not expired");

        yeedContract.closePropose(param);
    }

    @Test
    public void closeIssueFail3() {
        closeIssue();

        String transactionId = "0x01";
        String issuer = "c3cf7a283a4415ce3c41f5374934612389334780";
        JsonObject param = new JsonObject();
        String proposeId = "3bc37802368aaf705e8364c4d52194b2a250828aff3c7a41640451db1bafaba2";
        param.addProperty("proposeId", proposeId);

        TransactionReceipt receipt = setTxReceipt(transactionId, issuer, BRANCH_ID, 1000001L);

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("propose is not issued");

        yeedContract.closePropose(param);
    }



    @Test
    public void closeIssue() {
        // require issuePropose
        issuePropose();
        String transactionId = "0x01";
        String issuer = "c3cf7a283a4415ce3c41f5374934612389334780";
        JsonObject param = new JsonObject();
        String proposeId = "3bc37802368aaf705e8364c4d52194b2a250828aff3c7a41640451db1bafaba2";
        param.addProperty("proposeId", proposeId);

        TransactionReceipt receipt = null;

        // Success case
        receipt = setTxReceipt(transactionId, issuer, BRANCH_ID, 1000000L);


        BigInteger proposeYeed = yeedContract.getBalance(proposeId);

        yeedContract.closePropose(param);

        BigInteger proposeCloseYeed = yeedContract.getBalance(proposeId);
        assert receipt.getStatus() == ExecuteStatus.SUCCESS;
        assert proposeYeed.compareTo(BigInteger.ZERO) > 0;
        assert proposeCloseYeed.compareTo(BigInteger.ZERO) == 0;

    }

}
