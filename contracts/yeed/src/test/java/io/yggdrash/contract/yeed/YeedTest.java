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

import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.store.BranchStateStore;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.ContractUtils;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptImpl;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.yeed.ehtereum.EthTransaction;
import io.yggdrash.contract.yeed.intertransfer.TxConfirmStatus;
import io.yggdrash.contract.yeed.propose.ProposeStatus;
import io.yggdrash.contract.yeed.propose.ProposeType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class YeedTest {
    private static final YeedContract.YeedService yeedContract = new YeedContract.YeedService();
    private static final Logger log = LoggerFactory.getLogger(YeedTest.class);

    private static final String ADDRESS_1 = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
    private static final String ADDRESS_2 = "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e";
    private static final String BRANCH_ID = "0x00";
    private Field txReceiptField;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws IllegalAccessException {
        StateStore coinContractStateStore = new StateStore(new HashMapDbSource());

        BranchStateStore branchStateStore = new BranchStateStore() {
            ValidatorSet set = new ValidatorSet();

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
                return null;
            }

            @Override
            public String getContractVersion(String contractName) {
                return null;
            }

            @Override
            public String getContractName(String contractVersion) {
                return null;
            }

            public void setValidators(ValidatorSet validatorSet) {
                this.set = validatorSet;
            }

        };
        branchStateStore.getValidators().getValidatorMap()
                .put("81b7e08f65bdf5648606c89998a9cc8164397647",
                        new Validator("81b7e08f65bdf5648606c89998a9cc8164397647"));
        List<Field> txReceipt = ContractUtils.txReceiptFields(yeedContract);
        if (txReceipt.size() == 1) {
            txReceiptField = txReceipt.get(0);
        }
        for (Field f : ContractUtils.contractFields(yeedContract, ContractStateStore.class)) {
            f.setAccessible(true);
            f.set(yeedContract, coinContractStateStore);
        }
        for (Field f : ContractUtils.contractFields(yeedContract, ContractBranchStateStore.class)) {
            f.setAccessible(true);
            f.set(yeedContract, branchStateStore);
        }

        genesis();
    }

    private void genesis() {
        String genesisStr = "{\"alloc\": "
                + "{\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\":{\"balance\": \"1000000000\"},"
                + "\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\":{\"balance\": \"1000000000\"},"
                + "\"cee3d4755e47055b530deeba062c5bd0c17eb00f\":{\"balance\": \"998000000000\"},"
                + "\"c3cf7a283a4415ce3c41f5374934612389334780\":{\"balance\": \"10000000000000000000000\"},"
                + "\"4d01e237570022440aa126ca0b63065d7f5fd589\":{\"balance\": \"10000000000000000000000\"}"
                + "}}";

        TransactionReceipt result = new TransactionReceiptImpl();

        try {
            txReceiptField.set(yeedContract, result);
            yeedContract.init(createParams(genesisStr));
        } catch (IllegalAccessException e) {
            log.warn(e.getMessage());
        }
        assertTrue(result.isSuccess());
    }

    @Test
    public void totalSupply() {
        BigInteger res = yeedContract.totalSupply();

        assertEquals(new BigInteger("20000000001000000000000"), res);
    }

    @Test
    public void balanceOf() {
        BigInteger res = getBalance(ADDRESS_1);

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
        log.debug("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94:{}", getBalance(ADDRESS_1));
        log.debug("1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e:{}", getBalance(ADDRESS_2));



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

        assertEquals(BigInteger.valueOf(999999989), getBalance(ADDRESS_1));
        assertEquals(BigInteger.valueOf(1000000010), getBalance(ADDRESS_2));

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
    public void failTransfer() {
        JsonObject paramObj = new JsonObject();
        paramObj.addProperty("to", "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e");
        paramObj.addProperty("amount", -1000000);
        paramObj.addProperty("fee", -1000);

        TransactionReceipt result = new TransactionReceiptImpl();
        result.setIssuer(ADDRESS_1);
        result.setBranchId(BRANCH_ID);
        try {
            txReceiptField.set(yeedContract, result);
            result = yeedContract.transfer(paramObj);
        } catch (IllegalAccessException e) {
            log.warn(e.getMessage());
        }

        assertFalse(result.isSuccess());
    }


    @Test
    public void transferFrom() {
        String owner = ADDRESS_1;
        String spender = ADDRESS_2;
        String to = "cee3d4755e47055b530deeba062c5bd0c17eb00f";

        approveByOwner(to, owner, spender, "1000");

        JsonObject transferFromObject = new JsonObject();

        transferFromObject.addProperty("from", owner);
        transferFromObject.addProperty("to", to);
        transferFromObject.addProperty("amount", "700");

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
        assertEquals(BigInteger.valueOf(1000000000), getBalance(spender));
        assertEquals(BigInteger.valueOf(1000000000), getBalance(owner));

        assertTransferFrom(to, owner, spender);
    }

    private void assertTransferFrom(String to, String owner, String spender) {
        JsonObject param = new JsonObject();
        param.addProperty("owner", owner);
        param.addProperty("spender", spender);

        assertEquals(BigInteger.valueOf(1000), yeedContract.allowance(param));
        assertEquals(BigInteger.valueOf(998000000000L), getBalance(to));
        assertEquals(BigInteger.valueOf(1000000000), getBalance(owner));
        assertEquals(BigInteger.valueOf(1000000000), getBalance(spender));
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
        BigInteger receiveAsset = new BigInteger("10000000");
        int receiveChainId = 1;
        long networkBlockHeight = 10;
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
        proposal.addProperty("receiveAsset", receiveAsset);
        proposal.addProperty("receiveChainId", receiveChainId);
        proposal.addProperty("networkBlockHeight", networkBlockHeight);

        proposal.addProperty("proposeType", proposeType.toValue());
        proposal.addProperty("senderAddress", senderAddress);
        proposal.addProperty("inputData", inputData);
        proposal.addProperty("stakeYeed", stakeYeed);
        proposal.addProperty("blockHeight", targetBlockHeight);
        proposal.addProperty("fee", fee);

        final BigInteger issuerOriginBalance = getBalance(issuer);

        TransactionReceipt receipt = setTxReceipt(transactionId, issuer, BRANCH_ID, 1);

        // issue propose
        yeedContract.issuePropose(proposal);

        assert receipt.getStatus().equals(ExecuteStatus.SUCCESS);
        receipt.getTxLog().stream().forEach(l -> log.debug(l));

        String proposeIssueIdPatten = "Propose [a-f0-9]{64} ISSUED";
        Pattern p = Pattern.compile(proposeIssueIdPatten);
        Matcher matcher = p.matcher(receipt.getTxLog().get(1));
        assert matcher.find();

        String proposeIssueId = matcher.group();
        String proposeId = proposeIssueId.replaceAll("Propose ","")
                .replaceAll(" ISSUED", "");

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
        String proposeId = "ea79c0652a5c88db8a0f53d37a2944a56ff2eaf4185370191c98313843b35056";
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
        String proposeId = "ea79c0652a5c88db8a0f53d37a2944a56ff2eaf4185370191c98313843b35056";
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
        String proposeId = "ea79c0652a5c88db8a0f53d37a2944a56ff2eaf4185370191c98313843b35056";
        param.addProperty("proposeId", proposeId);

        TransactionReceipt receipt = setTxReceipt(transactionId, issuer, BRANCH_ID, 1000001L);

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("propose is CLOSED");

        yeedContract.closePropose(param);
    }



    @Test
    public void closeIssue() {
        // require issuePropose
        issuePropose();
        String transactionId = "0x01";
        String issuer = "c3cf7a283a4415ce3c41f5374934612389334780";
        JsonObject param = new JsonObject();
        String proposeId = "ea79c0652a5c88db8a0f53d37a2944a56ff2eaf4185370191c98313843b35056";
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


    @Test
    public void processingPropose() {
        // Type 1 - Issuer validate transaction
        final String transactionId = "0x02";
        String receiveAddress = "c3cf7a283a4415ce3c41f5374934612389334780";
        BigInteger receiveAsset = new BigInteger("1000000000000000000");
        int receiveChainId = 1;
        long networkBlockHeight = 10;
        ProposeType proposeType = ProposeType.YEED_TO_ETHER;

        String senderAddress = "5e032243d507c743b061ef021e2ec7fcc6d3ab89";

        String inputData = null;
        BigInteger stakeYeed = new BigInteger("1000000000000000000");
        long targetBlockHeight = 1000000L;
        BigInteger fee = new BigInteger("10000000000000000");
        String issuer = "c3cf7a283a4415ce3c41f5374934612389334780";

        JsonObject proposal = new JsonObject();
        proposal.addProperty("receiveAddress", receiveAddress);
        proposal.addProperty("receiveAsset", receiveAsset);
        proposal.addProperty("receiveChainId", receiveChainId);
        proposal.addProperty("networkBlockHeight", networkBlockHeight);
        proposal.addProperty("proposeType", proposeType.toValue());
        proposal.addProperty("senderAddress", senderAddress);
        proposal.addProperty("inputData", inputData);
        proposal.addProperty("stakeYeed", stakeYeed);
        proposal.addProperty("blockHeight", targetBlockHeight);
        proposal.addProperty("fee", fee);

        BigInteger issuerOriginBalance = getBalance(issuer);
        log.debug("issuerOriginBalance {} ", issuerOriginBalance);
        TransactionReceipt receipt = setTxReceipt(transactionId, issuer, BRANCH_ID, 1);

        // issue propose
        yeedContract.issuePropose(proposal);

        assert receipt.getStatus() == ExecuteStatus.SUCCESS;

        BigInteger issuerIssuedBalance = getBalance(issuer);
        assert issuerOriginBalance.subtract(stakeYeed.add(fee)).compareTo(issuerIssuedBalance) == 0;
        log.debug("issuerIssuedBalance {} ", issuerIssuedBalance);
        assert issuerOriginBalance.subtract(issuerIssuedBalance).compareTo(stakeYeed.add(fee)) == 0;

        assert receipt.getStatus() == ExecuteStatus.SUCCESS;

        String proposeIssue = receipt.getTxLog().get(1);
        log.debug("Log 1 : {} ",proposeIssue);
        String proposeIssueIdPatten = "Propose [a-f0-9]{64} ISSUED";
        Pattern p = Pattern.compile(proposeIssueIdPatten);
        Matcher matcher = p.matcher(proposeIssue);
        assert matcher.find();

        String proposeIssueId = matcher.group();
        proposeIssueId = proposeIssueId.replaceAll("Propose ","")
                .replaceAll(" ISSUED", "");

        log.debug("propose Issue ID : {}", proposeIssueId);

        String ethRawTransaction = "0xf86f830414ac850df847580082afc894c3cf7a283a4415ce3c41f5374934612389"
                + "334780880de0b6b3a76400008026a0c9938e35c6281a2003531ef19c0368fb0ec680d1bc073ee2881"
                + "3602616ce172ca03885e6218dbd7a09fc250ce4eb982114cc25c0974f4adfbd08c4e834f9c74dc3";


        byte[] etheSendEncode = HexUtil.hexStringToBytes(ethRawTransaction);

        EthTransaction ethTransaction = new EthTransaction(etheSendEncode);
        log.debug("sender : {} ", HexUtil.toHexString(ethTransaction.getSendAddress()));
        log.debug("Receive : {}", HexUtil.toHexString(ethTransaction.getReceiveAddress()));
        log.debug("{} WEI", ethTransaction.getValue());

        JsonObject processJson = new JsonObject();
        processJson.addProperty("proposeId", proposeIssueId);
        processJson.addProperty("rawTransaction", ethRawTransaction);
        processJson.addProperty("fee", BigInteger.ZERO);
        receipt = setTxReceipt(transactionId, issuer, BRANCH_ID, 10);
        yeedContract.processPropose(processJson);

        BigInteger issuerDoneBalance = getBalance(issuer);
        log.debug("issuerDoneBalance {} ", issuerDoneBalance);
        log.debug("fee {} ", fee);
        // issuer Done process , issuer return fee 1/2
        receipt.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals("Transaction is Success", ExecuteStatus.SUCCESS, receipt.getStatus());
        assert issuerDoneBalance.subtract(issuerIssuedBalance)
                .compareTo(fee.divide(BigInteger.valueOf(2L))) == 0;
        assert issuerDoneBalance.compareTo(issuerIssuedBalance) > 0;

        assert receipt.getStatus() == ExecuteStatus.SUCCESS;

        receipt = setTxReceipt("0x03", issuer, BRANCH_ID, 50);
        yeedContract.processPropose(processJson);
        assert receipt.getStatus() == ExecuteStatus.FALSE;
        receipt.getTxLog().stream().forEach(l -> log.debug(l));

    }

    @Test
    public void processingInvalid() {
        final String transactionId = "0x02";
        String receiveAddress = "ad8992d6f78d9cc597438efbccd8940d7c02bc6d";
        BigInteger receiveAsset = new BigInteger("11000000000000000000");
        int receiveChainId = 1;
        long networkBlockHeight = 10;
        ProposeType proposeType = ProposeType.YEED_TO_ETHER;

        String senderAddress = "dcf94a3153398b9e78a3202ffb7d0c606348f616";

        String inputData = null;
        BigInteger stakeYeed = new BigInteger("1000000000000000000");
        long targetBlockHeight = 1000000L;
        BigInteger fee = new BigInteger("10000000000000000");
        String issuer = "c3cf7a283a4415ce3c41f5374934612389334780";

        JsonObject proposal = new JsonObject();
        proposal.addProperty("receiveAddress", receiveAddress);
        proposal.addProperty("receiveAsset", receiveAsset);
        proposal.addProperty("receiveChainId", receiveChainId);
        proposal.addProperty("networkBlockHeight", networkBlockHeight);
        proposal.addProperty("proposeType", proposeType.toValue());
        proposal.addProperty("senderAddress", senderAddress);
        proposal.addProperty("inputData", inputData);
        proposal.addProperty("stakeYeed", stakeYeed);
        proposal.addProperty("blockHeight", targetBlockHeight);
        proposal.addProperty("fee", fee);

        BigInteger issuerOriginBalance = getBalance(issuer);
        log.debug("issuerOriginBalance {} ", issuerOriginBalance);
        TransactionReceipt receipt = setTxReceipt(transactionId, issuer, BRANCH_ID, 1);

        // issue propose
        yeedContract.issuePropose(proposal);

        assert receipt.getStatus() == ExecuteStatus.SUCCESS;

        String proposeIssue = receipt.getTxLog().get(1);
        log.debug("Log 1 : {} ",proposeIssue);
        String proposeIssueIdPatten = "Propose [a-f0-9]{64} ISSUED";
        Pattern p = Pattern.compile(proposeIssueIdPatten);
        Matcher matcher = p.matcher(proposeIssue);
        assert matcher.find();

        String proposeIssueId = matcher.group();
        proposeIssueId = proposeIssueId.replaceAll("Propose ","")
                .replaceAll(" ISSUED", "");

        log.debug("propose Issue ID : {}", proposeIssueId);

        log.debug(proposal.toString());

        // Make Error


        String ethHexString = "0xf86e81b68502540be400830493e094735c4b587ae018c4733df6a8ef59711d15f551b48"
                + "80de0b6b3a76400008025a09a5ebf9b742c5a1fb3a6fd931cc419afefdcf5cca371c411a1d5e2b"
                + "55de8dee1a04cbfe5ec19ec5c8fa4a762fa49f17749b0a13a380567da1b75cf80d2faa1a8c9";

        byte[] etheSendEncode = HexUtil.hexStringToBytes(ethHexString);

        EthTransaction ethTransaction = new EthTransaction(etheSendEncode);
        log.debug("sender : {} ", HexUtil.toHexString(ethTransaction.getSendAddress()));
        log.debug("Receive : {}", HexUtil.toHexString(ethTransaction.getReceiveAddress()));
        log.debug("{} WEI", ethTransaction.getValue());

        JsonObject processJson = new JsonObject();
        processJson.addProperty("proposeId", proposeIssueId);
        processJson.addProperty("rawTransaction", ethHexString);
        processJson.addProperty("fee", BigInteger.ZERO);
        receipt = setTxReceipt(transactionId, issuer, BRANCH_ID, 10);
        yeedContract.processPropose(processJson);

        Assert.assertEquals("processing is fail", ExecuteStatus.FALSE, receipt.getStatus());

        receipt.getTxLog().stream().forEach(l -> log.debug(l));

    }

    @Test
    public void proposeIssueFail() {
        String receiveAddress = "ad8992d6f78d9cc597438efbccd8940d7c02bc6d";
        BigInteger receiveAsset = new BigInteger("11000000000000000000");
        int receiveChainId = 1;
        long networkBlockHeight = 10;
        ProposeType proposeType = ProposeType.YEED_TO_ETHER;

        String senderAddress = "dcf94a3153398b9e78a3202ffb7d0c606348f616";

        String inputData = null;
        BigInteger stakeYeed = new BigInteger("-1000000000000000000");
        long targetBlockHeight = 1000000L;
        BigInteger fee = new BigInteger("10000000000000000");
        String issuer = "c3cf7a283a4415ce3c41f5374934612389334780";

        JsonObject proposal = new JsonObject();
        proposal.addProperty("receiveAddress", receiveAddress);
        proposal.addProperty("receiveAsset", receiveAsset);
        proposal.addProperty("receiveChainId", receiveChainId);
        proposal.addProperty("networkBlockHeight", networkBlockHeight);
        proposal.addProperty("proposeType", proposeType.toValue());
        proposal.addProperty("senderAddress", senderAddress);
        proposal.addProperty("inputData", inputData);
        proposal.addProperty("stakeYeed", stakeYeed);
        proposal.addProperty("blockHeight", targetBlockHeight);
        proposal.addProperty("fee", fee);

        BigInteger issuerOriginBalance = getBalance(issuer);
        log.debug("issuerOriginBalance {} ", issuerOriginBalance);
        String transactionId = "0x02";
        TransactionReceipt receipt = setTxReceipt(transactionId, issuer, BRANCH_ID, 1);

        // issue propose
        yeedContract.issuePropose(proposal);
        assert receipt.getStatus() == ExecuteStatus.FALSE;

        stakeYeed = new BigInteger("1000000000000000000");
        fee = new BigInteger("-10000000000000000");
        proposal.addProperty("stakeYeed", stakeYeed);
        proposal.addProperty("fee", fee);
        // issue propose
        yeedContract.issuePropose(proposal);
        assert receipt.getStatus() == ExecuteStatus.FALSE;

        stakeYeed = new BigInteger("1000000000000000000");
        fee = new BigInteger("10000000000000000");
        proposal.addProperty("stakeYeed", stakeYeed);
        proposal.addProperty("fee", fee);
        // issue propose
        yeedContract.issuePropose(proposal);

        assert receipt.getStatus() == ExecuteStatus.SUCCESS;
        receipt.getTxLog().stream().forEach(l -> log.debug(l));
    }


    @Test
    public void processingProposeConfirm() {
        // Type 1 - Issuer validate transaction
        final String transactionId = "0x02";
        String receiveAddress = "c3cf7a283a4415ce3c41f5374934612389334780";
        BigInteger receiveAsset = new BigInteger("1000000000000000000");
        int receiveChainId = 1;
        long networkBlockHeight = 10;
        ProposeType proposeType = ProposeType.YEED_TO_ETHER;

        String senderAddress = "5e032243d507c743b061ef021e2ec7fcc6d3ab89";

        String inputData = null;
        BigInteger stakeYeed = new BigInteger("1000000000000000000");
        long targetBlockHeight = 1000000L;
        BigInteger fee = new BigInteger("10000000000000000");
        String issuer = "c3cf7a283a4415ce3c41f5374934612389334780";

        JsonObject proposal = new JsonObject();
        proposal.addProperty("receiveAddress", receiveAddress);
        proposal.addProperty("receiveAsset", receiveAsset);
        proposal.addProperty("receiveChainId", receiveChainId);
        proposal.addProperty("networkBlockHeight", networkBlockHeight);
        proposal.addProperty("proposeType", proposeType.toValue());
        proposal.addProperty("senderAddress", senderAddress);
        proposal.addProperty("inputData", inputData);
        proposal.addProperty("stakeYeed", stakeYeed);
        proposal.addProperty("blockHeight", targetBlockHeight);
        proposal.addProperty("fee", fee);

        BigInteger issuerOriginBalance = getBalance(issuer);
        log.debug("issuerOriginBalance {} ", issuerOriginBalance);
        TransactionReceipt receipt = setTxReceipt(transactionId, issuer, BRANCH_ID, 1);

        // issue propose
        yeedContract.issuePropose(proposal);
        assert receipt.getStatus() == ExecuteStatus.SUCCESS;
        BigInteger issuerIssuedBalance = getBalance(issuer);
        assert issuerOriginBalance.subtract(stakeYeed.add(fee)).compareTo(issuerIssuedBalance) == 0;
        log.debug("issuerIssuedBalance {} ", issuerIssuedBalance);
        assert issuerOriginBalance.subtract(issuerIssuedBalance).compareTo(stakeYeed.add(fee)) == 0;

        // Get propose ID
        String proposeIssue = receipt.getTxLog().get(1);
        log.debug("Log 1 : {} ",proposeIssue);
        String proposeIssueIdPatten = "Propose [a-f0-9]{64} ISSUED";
        Pattern p = Pattern.compile(proposeIssueIdPatten);
        Matcher matcher = p.matcher(proposeIssue);
        assert matcher.find();

        String proposeIssueId = matcher.group();
        proposeIssueId = proposeIssueId.replaceAll("Propose ","")
                .replaceAll(" ISSUED", "");

        log.debug("propose Issue ID : {}", proposeIssueId);

        // Step-2 Send transaction in ethereum, and get raw transaction

        String ethRawTransaction = "0xf86f830414ac850df847580082afc894c3cf7a283a4415ce3c41f5374934612389"
                + "334780880de0b6b3a76400008026a0c9938e35c6281a2003531ef19c0368fb0ec680d1bc073ee2881"
                + "3602616ce172ca03885e6218dbd7a09fc250ce4eb982114cc25c0974f4adfbd08c4e834f9c74dc3";


        byte[] etheSendEncode = HexUtil.hexStringToBytes(ethRawTransaction);
        // check raw transaction
        EthTransaction ethTransaction = new EthTransaction(etheSendEncode);
        log.debug("sender : {} ", HexUtil.toHexString(ethTransaction.getSendAddress()));
        log.debug("Receive : {}", HexUtil.toHexString(ethTransaction.getReceiveAddress()));
        log.debug("{} WEI", ethTransaction.getValue());

        // STEP 2 : Propose Issue processing
        JsonObject processJson = new JsonObject();
        processJson.addProperty("proposeId", proposeIssueId);
        processJson.addProperty("rawTransaction", ethRawTransaction);
        processJson.addProperty("fee", BigInteger.ZERO);

        receipt = setTxReceipt(transactionId, senderAddress, BRANCH_ID, 10);
        yeedContract.processPropose(processJson);


        receipt.getTxLog().stream().forEach(l -> log.debug(l));

        // Validator transaction confirm (check exist and block height, index)
        // tx id : a077dd42d4421dca5cb8a7a11aca5b27adaf40be2428f9dc8e42a56abdabeb66
        // network : 1

        // make transaction params

        // validator : 81b7e08f65bdf5648606c89998a9cc8164397647
        String validator = "81b7e08f65bdf5648606c89998a9cc8164397647";
        receipt = setTxReceipt(transactionId, validator,BRANCH_ID, 100);

        // txConfirmId : a077dd42d4421dca5cb8a7a11aca5b27adaf40be2428f9dc8e42a56abdabeb66
        // status
        // blockHeight
        // index

        JsonObject params = new JsonObject();
        //params.addProperty("proposeId", "18d48bc062ecab96a6d5b24791b5b41fdb69a12ef76d56f9f842871f92716b7a");
        params.addProperty("txConfirmId", "af22edcccb4d6e4568b701294479c99a04afbd5ab25578adbe58549e36e4abfa");
        params.addProperty("txId", "a077dd42d4421dca5cb8a7a11aca5b27adaf40be2428f9dc8e42a56abdabeb66");
        params.addProperty("status", TxConfirmStatus.DONE.toValue());
        params.addProperty("blockHeight", 1000001L);
        params.addProperty("lastBlockHeight", 1002001L);

        params.addProperty("index", 1);

        yeedContract.transactionConfirm(params);
        receipt.getTxLog().stream().forEach(l -> log.debug(l));

        // check txConfirm Status
        JsonObject param = new JsonObject();
        param.addProperty("txConfirmId", "af22edcccb4d6e4568b701294479c99a04afbd5ab25578adbe58549e36e4abfa");
        JsonObject queryConfirm = yeedContract.queryTransactionConfirm(param);
        Assert.assertEquals(TxConfirmStatus.DONE.toValue(), queryConfirm.get("status").getAsInt());
        log.debug(queryConfirm.toString());

        Assert.assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());
        log.debug("PROPOSE STAKE : {} YEED", getBalance(proposeIssueId));
        // 1010000000000000000
        //   10000000000000000
        Assert.assertTrue(getBalance(proposeIssueId).compareTo(BigInteger.ZERO) == 0);

        // Check propose STATUS
        JsonObject proposeQueryParam = new JsonObject();
        proposeQueryParam.addProperty("proposeId", proposeIssueId);
        JsonObject queryResult = yeedContract.queryPropose(proposeQueryParam);
        log.debug(queryResult.get("status").getAsString());
        Assert.assertEquals("propose Is DONE", queryResult.get("status").getAsString(),
                ProposeStatus.DONE.toString());

    }


}
