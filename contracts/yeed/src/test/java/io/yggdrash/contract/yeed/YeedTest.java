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
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptAdapter;
import io.yggdrash.contract.core.TransactionReceiptImpl;
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
    private TransactionReceiptAdapter adapter;

    private BigInteger BASE_CURRENCY = BigInteger.TEN.pow(18);
    // 0.01 YEED
    private BigInteger DEFAULT_FEE = BASE_CURRENCY.divide(BigInteger.valueOf(100L));

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
        StateStore coinContractStateStore = new StateStore(new HashMapDbSource());
        BranchStateStore branchStateStore = branchStateStoreMock();
        branchStateStore.getValidators().getValidatorMap()
                .put("81b7e08f65bdf5648606c89998a9cc8164397647",
                        new Validator("81b7e08f65bdf5648606c89998a9cc8164397647"));

        adapter = new TransactionReceiptAdapter();
        yeedContract.txReceipt = adapter;
        yeedContract.store = coinContractStateStore;
        yeedContract.branchStateStore = branchStateStore;

        TransactionReceipt result = new TransactionReceiptImpl();
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

        BigInteger sendAmount = BASE_CURRENCY;
        BigInteger senderAmount = getBalance(ADDRESS_1);
        BigInteger receiveAmount = getBalance(ADDRESS_2);
        BigInteger feeAmount = DEFAULT_FEE;

        JsonObject paramObj = new JsonObject();
        paramObj.addProperty("to", ADDRESS_2);
        paramObj.addProperty("amount", sendAmount);
        paramObj.addProperty("fee", feeAmount);

        log.debug("{}:{}", ADDRESS_1, senderAmount);
        log.debug("{}:{}", ADDRESS_2, receiveAmount);

        // SETUP
        TransactionReceipt receipt = setUpReceipt(ADDRESS_1);

        receipt = yeedContract.transfer(paramObj);

        assertTrue(receipt.isSuccess());

        BigInteger senderRemainAmount = senderAmount.subtract(feeAmount).subtract(sendAmount);
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

        TransactionReceipt receipt = setUpReceipt(ADDRESS_1);

        receipt = yeedContract.transfer(paramObj);

        assertFalse(receipt.isSuccess());
    }

    @Test
    public void transferFrom() {
        String owner = ADDRESS_1;
        String spender = ADDRESS_2;
        String to = "cee3d4755e47055b530deeba062c5bd0c17eb00f";

        BigInteger approveBalance = BASE_CURRENCY.multiply(BigInteger.valueOf(1000L));
        BigInteger transferFromBalance = BASE_CURRENCY.multiply(BigInteger.valueOf(700L));
        BigInteger feeBalance = DEFAULT_FEE;
        BigInteger toBalance = getBalance(to);
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
        transferFromObject.addProperty("fee", feeBalance);

        TransactionReceipt receipt = setUpReceipt(spender);

        // Transfer From owner to receiver by spender
        yeedContract.transferFrom(transferFromObject);

        assertTrue(receipt.isSuccess());
        assertEquals(approveBalance.subtract(transferFromBalance).subtract(feeBalance),
                getAllowance(owner, spender));
        assertEquals(toBalance.add(transferFromBalance), getBalance(to));

        String logFormat = "{} : {}";
        log.debug(logFormat, to, getBalance(to));
        log.debug(logFormat, owner, getBalance(owner));
        log.debug(logFormat, spender, getBalance(spender));
        log.debug("getAllowance : {}", getAllowance(owner, spender));
        printTxLog();

        TransactionReceipt receipt2 = setUpReceipt("1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e");

        yeedContract.transferFrom(transferFromObject);

        // Insufficient funds
        assertFalse(receipt2.isSuccess());

        allowanceBalance = getAllowance(owner, spender);
        // subtract feeBalance
        allowanceBalance = allowanceBalance.subtract(feeBalance);
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

        BigInteger spenderBalance = getBalance(spender);
        BigInteger ownerBalance = getBalance(owner);
        TransactionReceipt receipt = setUpReceipt(owner);

        yeedContract.approve(paramObj);

        adapter.getTxLog().stream().forEach(l -> log.debug(l));

        assertTrue(receipt.isSuccess());
        assertEquals(spenderBalance, getBalance(spender));
        assertTrue(ownerBalance.compareTo(getBalance(owner)) > 0);
    }

    @Test
    public void issuePropose() {
        final String transactionId = "0x00";

        String receiverAddress = "c3cf7a283a4415ce3c41f5374934612389334780";
        BigInteger receiveAsset = new BigInteger("10000000");
        int receiveChainId = 1;       // Ethereum chain id
        long networkBlockHeight = 10; // Ethereum network block height
        ProposeType proposeType = ProposeType.YEED_TO_ETHER;

        String senderAddress = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        String inputData = null;

        String issuer = "c3cf7a283a4415ce3c41f5374934612389334780";
        long targetBlockHeight = 1000000L;
        BigInteger stakeYeed = new BigInteger("1000000000000000");

        JsonObject proposal = createProposal(receiverAddress, receiveAsset, receiveChainId, networkBlockHeight,
                proposeType, senderAddress, inputData, stakeYeed, targetBlockHeight, DEFAULT_FEE);

        final BigInteger issuerOriginBalance = getBalance(issuer);

        TransactionReceipt receipt = setUpReceipt(transactionId, issuer, BRANCH_ID, 1); // 1 == current blockHeight

        yeedContract.issuePropose(proposal);

        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());
        printTxLog();

        String proposeIssueIdPatten = "Propose [a-f0-9]{64} ISSUED";
        Pattern p = Pattern.compile(proposeIssueIdPatten);
        Matcher matcher = p.matcher(receipt.getTxLog().get(1));

        boolean isMatched = matcher.find();
        assertTrue(isMatched);

        String proposeIssuedReceiptLog = matcher.group();
        String proposeId = proposeIssuedReceiptLog.replaceAll("Propose ", "").replaceAll(" ISSUED", "");

        BigInteger balanceOfProposeId = getBalance(proposeId);
        BigInteger issuerBalanceAfterProposalIssued = getBalance(issuer);

        log.debug("Proposal Stake YEED {}", balanceOfProposeId.toString());
        log.debug("Issuer Origin YEED {}", issuerOriginBalance.toString());
        log.debug("Issuer After Proposal Issued YEED {}", issuerBalanceAfterProposalIssued.toString());

        assertEquals(0, balanceOfProposeId.compareTo(stakeYeed.add(DEFAULT_FEE)));
        assertEquals(0, issuerOriginBalance.subtract(stakeYeed.add(DEFAULT_FEE))
                .compareTo(issuerBalanceAfterProposalIssued));

        // Query proposal by its id
        JsonObject queryProposeParam = new JsonObject();
        queryProposeParam.addProperty("proposeId", proposeId);
        JsonObject queryPropose = yeedContract.queryPropose(queryProposeParam);
        log.debug("Proposal = {}", queryPropose.toString());

        assertEquals(receiverAddress, queryPropose.get("receiverAddress").getAsString());
    }

    @Test
    public void closeIssueFail() {
        issuePropose();

        String transactionId = "0x01";
        String issuer = "d3cf7a283a4415ce3c41f5374934612389334780";
        JsonObject param = new JsonObject();
        String proposeId = "ea79c0652a5c88db8a0f53d37a2944a56ff2eaf4185370191c98313843b35056";
        param.addProperty("proposeId", proposeId);

        setUpReceipt(transactionId, issuer, BRANCH_ID, 1000000L);

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Transaction issuer is not the proposal issuer");

        yeedContract.closePropose(param);
    }

    @Test
    public void closeIssueFail2() {
        issuePropose();

        String transactionId = "0x01";
        String issuer = "c3cf7a283a4415ce3c41f5374934612389334780";
        JsonObject param = new JsonObject();
        String proposeId = "ea79c0652a5c88db8a0f53d37a2944a56ff2eaf4185370191c98313843b35056";
        param.addProperty("proposeId", proposeId);

        setUpReceipt(transactionId, issuer, BRANCH_ID, 100L);

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("The proposal has not expired");

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

        setUpReceipt(transactionId, issuer, BRANCH_ID, 1000001L);

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("The proposal already CLOSE");

        yeedContract.closePropose(param);
    }

    @Test
    public void closeIssue() {
        issuePropose();

        String transactionId = "0x01";
        String issuer = "c3cf7a283a4415ce3c41f5374934612389334780";
        JsonObject param = new JsonObject();
        String proposeId = "ea79c0652a5c88db8a0f53d37a2944a56ff2eaf4185370191c98313843b35056";
        param.addProperty("proposeId", proposeId);

        // Success case
        TransactionReceipt receipt = setUpReceipt(transactionId, issuer, BRANCH_ID, 1000000L);

        BigInteger originProposalBalance = yeedContract.getBalance(proposeId);

        yeedContract.closePropose(param);

        BigInteger currentProposalBalance = yeedContract.getBalance(proposeId);

        assertSame(ExecuteStatus.SUCCESS, receipt.getStatus());
        assertTrue(originProposalBalance.compareTo(BigInteger.ZERO) > 0);
        assertEquals(0, currentProposalBalance.compareTo(BigInteger.ZERO));
    }

    @Test
    public void processingPropose() {
        // Type 1 - Issuer validate transaction
        final String transactionId = "0x02";

        String receiverAddress = "c3cf7a283a4415ce3c41f5374934612389334780";
        BigInteger receiveAsset = new BigInteger("1000000000000000000");
        int receiveChainId = 1;
        long networkBlockHeight = 10;
        ProposeType proposeType = ProposeType.YEED_TO_ETHER;

        String senderAddress = "5e032243d507c743b061ef021e2ec7fcc6d3ab89";
        String inputData = null;

        String issuer = "c3cf7a283a4415ce3c41f5374934612389334780";
        long targetBlockHeight = 1000000L;
        BigInteger stakeYeed = new BigInteger("1000000000000000000");
        BigInteger fee = new BigInteger("10000000000000000");

        JsonObject proposal = createProposal(receiverAddress, receiveAsset, receiveChainId, networkBlockHeight,
                proposeType, senderAddress, inputData, stakeYeed, targetBlockHeight, fee);

        BigInteger issuerOriginBalance = getBalance(issuer);
        log.debug("IssuerOriginBalance : {} ", issuerOriginBalance);

        TransactionReceipt receipt = setUpReceipt(transactionId, issuer, BRANCH_ID, 1);  // 1 == current blockHeight

        yeedContract.issuePropose(proposal);

        assertSame(ExecuteStatus.SUCCESS, receipt.getStatus());

        BigInteger issuerBalanceAfterProposalIssued = getBalance(issuer);
        log.debug("Issuer Balance After Proposal Issued : {} ", issuerBalanceAfterProposalIssued);

        assertEquals(0, issuerOriginBalance.subtract(stakeYeed.add(fee)).compareTo(issuerBalanceAfterProposalIssued));
        assertEquals(0, issuerOriginBalance.subtract(issuerBalanceAfterProposalIssued).compareTo(stakeYeed.add(fee)));
        assertSame(ExecuteStatus.SUCCESS, receipt.getStatus());

        String proposeIssueIdPatten = "Propose [a-f0-9]{64} ISSUED";
        Pattern p = Pattern.compile(proposeIssueIdPatten);
        String firstLog = receipt.getTxLog().get(1);
        Matcher matcher = p.matcher(firstLog);
        log.debug("Log 1 : {} ", firstLog);

        boolean isMatched = matcher.find();
        assertTrue(isMatched);

        String proposeId = matcher.group()
                .replaceAll("Propose ", "")
                .replaceAll(" ISSUED", "");
        log.debug("ProposeId : {}", proposeId);

        // Process the proposal from now on

        String ethRawTransaction = "0xf86f830414ac850df847580082afc894c3cf7a283a4415ce3c41f5374934612389"
                + "334780880de0b6b3a76400008026a0c9938e35c6281a2003531ef19c0368fb0ec680d1bc073ee2881"
                + "3602616ce172ca03885e6218dbd7a09fc250ce4eb982114cc25c0974f4adfbd08c4e834f9c74dc3";

        byte[] etheSendEncode = HexUtil.hexStringToBytes(ethRawTransaction);

        EthTransaction ethTransaction = new EthTransaction(etheSendEncode);
        log.debug("Sender : {} ", HexUtil.toHexString(ethTransaction.getSendAddress()));
        log.debug("Receiver : {}", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        log.debug("{} WEI", ethTransaction.getValue());

        BigInteger networkFee = BASE_CURRENCY.divide(BigInteger.valueOf(100L));

        JsonObject processJson = new JsonObject();
        processJson.addProperty("proposeId", proposeId);
        processJson.addProperty("rawTransaction", ethRawTransaction);
        processJson.addProperty("fee", networkFee);

        receipt = setUpReceipt(transactionId, issuer, BRANCH_ID, 10); // 1 == current blockHeight

        // Propose Processing
        yeedContract.processPropose(processJson);

        BigInteger issuerBalanceAfterProposalDone = getBalance(issuer);
        log.debug("Issuer Balance After Proposal Done : {} ", issuerBalanceAfterProposalDone);
        log.debug("fee : {} ", fee);
        printTxLog();

        // The issuer has done processing and has received 1/2 fee back

        assertEquals("Transaction is Success", ExecuteStatus.SUCCESS, receipt.getStatus());

        // Issuer used network fee so subtract networkFee
        issuerBalanceAfterProposalIssued = issuerBalanceAfterProposalIssued.subtract(networkFee);

        BigInteger remainIssuerBalance =
                issuerBalanceAfterProposalDone.subtract(issuerBalanceAfterProposalIssued)
                ;

        assertEquals("Half propose stake fund will remain issuer",0,
                remainIssuerBalance.compareTo(fee.divide(BigInteger.valueOf(2L))));

        assertTrue("Propose Done,Issuer will more fund remain proposal issued",
                issuerBalanceAfterProposalDone.compareTo(issuerBalanceAfterProposalIssued) > 0);

        receipt = setUpReceipt("0x03", issuer, BRANCH_ID, 50);  // 1 == current blockHeight

        yeedContract.processPropose(processJson);

        assertSame(ExecuteStatus.FALSE, receipt.getStatus()); // Propose cannot proceed (ProposeStatus=DONE)
    }

    @Test
    public void processingInvalid() {
        final String transactionId = "0x02";

        String receiverAddress = "ad8992d6f78d9cc597438efbccd8940d7c02bc6d";
        BigInteger receiveAsset = new BigInteger("11000000000000000000");
        int receiveChainId = 1;
        long networkBlockHeight = 10;
        ProposeType proposeType = ProposeType.YEED_TO_ETHER;

        String senderAddress = "dcf94a3153398b9e78a3202ffb7d0c606348f616";

        BigInteger stakeYeed = new BigInteger("1000000000000000000");
        long targetBlockHeight = 1000000L;
        BigInteger fee = new BigInteger("10000000000000000");
        String issuer = "c3cf7a283a4415ce3c41f5374934612389334780";

        JsonObject proposal = createProposal(receiverAddress, receiveAsset, receiveChainId, networkBlockHeight,
                proposeType, senderAddress, null, stakeYeed, targetBlockHeight, fee);

        BigInteger issuerOriginBalance = getBalance(issuer);
        log.debug("issuerOriginBalance  : {} ", issuerOriginBalance);

        TransactionReceipt receipt = setUpReceipt(transactionId, issuer, BRANCH_ID, 1);

        yeedContract.issuePropose(proposal);

        assertSame(ExecuteStatus.SUCCESS, receipt.getStatus());

        String proposalIssuedLog = receipt.getTxLog().get(1);
        log.debug("Log 1 : {} ", proposalIssuedLog);
        String proposeIssueIdPatten = "Propose [a-f0-9]{64} ISSUED";
        Pattern p = Pattern.compile(proposeIssueIdPatten);
        Matcher matcher = p.matcher(proposalIssuedLog);

        boolean isMatched = matcher.find();
        assertTrue(isMatched);

        String proposeId = matcher.group()
                .replaceAll("Propose ", "")
                .replaceAll(" ISSUED", "");
        log.debug("ProposeId={}", proposeId);
        log.debug("Proposal={}", proposal.toString());

        // Create an error transaction
        String ethHexString = "0xf86e81b68502540be400830493e094735c4b587ae018c4733df6a8ef59711d15f551b48"
                + "80de0b6b3a76400008025a09a5ebf9b742c5a1fb3a6fd931cc419afefdcf5cca371c411a1d5e2b"
                + "55de8dee1a04cbfe5ec19ec5c8fa4a762fa49f17749b0a13a380567da1b75cf80d2faa1a8c9";

        byte[] etheSendEncode = HexUtil.hexStringToBytes(ethHexString);

        EthTransaction ethTransaction = new EthTransaction(etheSendEncode);
        log.debug("Sender : {} ", HexUtil.toHexString(ethTransaction.getSendAddress()));
        log.debug("Receiver : {}", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        log.debug("{} WEI", ethTransaction.getValue());

        JsonObject processJson = new JsonObject();
        processJson.addProperty("proposeId", proposeId);
        processJson.addProperty("rawTransaction", ethHexString);
        processJson.addProperty("fee", DEFAULT_FEE);

        receipt = setUpReceipt(transactionId, issuer, BRANCH_ID, 10);

        yeedContract.processPropose(processJson);

        assertEquals("Proposal processing failed", ExecuteStatus.FALSE, receipt.getStatus());

        receipt.getTxLog().forEach(log::debug);
    }

    @Test
    public void proposeIssueFail() {
        String receiverAddress = "ad8992d6f78d9cc597438efbccd8940d7c02bc6d";
        BigInteger receiveAsset = new BigInteger("11000000000000000000");
        int receiveChainId = 1;
        long networkBlockHeight = 10;
        ProposeType proposeType = ProposeType.YEED_TO_ETHER;

        String senderAddress = "dcf94a3153398b9e78a3202ffb7d0c606348f616";

        BigInteger stakeYeed = new BigInteger("-1000000000000000000");
        long targetBlockHeight = 1000000L;
        BigInteger fee = new BigInteger("10000000000000000");
        String issuer = "c3cf7a283a4415ce3c41f5374934612389334780";

        JsonObject proposal = createProposal(receiverAddress, receiveAsset, receiveChainId, networkBlockHeight,
                proposeType, senderAddress, null, stakeYeed, targetBlockHeight, fee);

        BigInteger issuerOriginBalance = getBalance(issuer);
        log.debug("issuerOriginBalance : {} ", issuerOriginBalance);

        String transactionId = "0x02";

        TransactionReceipt receipt = setUpReceipt(transactionId, issuer, BRANCH_ID, 1);

        yeedContract.issuePropose(proposal);
        assertSame(ExecuteStatus.FALSE, receipt.getStatus());
        printTxLog();

        // Change stakeYeed and fee value of the proposal
        stakeYeed = new BigInteger("1000000000000000000");
        proposal.addProperty("stakeYeed", stakeYeed);
        proposal.addProperty("fee", new BigInteger("-10000000000000000"));

        yeedContract.issuePropose(proposal);

        assertSame(ExecuteStatus.ERROR, receipt.getStatus());
        printTxLog();

        stakeYeed = new BigInteger("1000000000000000000");
        fee = new BigInteger("10000000000000000");
        proposal.addProperty("stakeYeed", stakeYeed);
        proposal.addProperty("fee", fee);

        yeedContract.issuePropose(proposal);

        assertSame(ExecuteStatus.SUCCESS, receipt.getStatus());
    }

    @Test
    public void processingProposeConfirm() {
        // Type 1 - Issuer validate transaction
        final String transactionId = "0x02";

        String receiverAddress = "c3cf7a283a4415ce3c41f5374934612389334780";
        BigInteger receiveAsset = new BigInteger("1000000000000000000");
        int receiveChainId = 1;
        long networkBlockHeight = 10;
        ProposeType proposeType = ProposeType.YEED_TO_ETHER;

        String senderAddress = "5e032243d507c743b061ef021e2ec7fcc6d3ab89";

        BigInteger stakeYeed = new BigInteger("1000000000000000000");
        long targetBlockHeight = 1000000L;
        BigInteger fee = new BigInteger("10000000000000000");
        String issuer = "c3cf7a283a4415ce3c41f5374934612389334780";

        JsonObject proposal = createProposal(receiverAddress, receiveAsset, receiveChainId, networkBlockHeight,
                proposeType, senderAddress, null, stakeYeed, targetBlockHeight, fee);

        BigInteger issuerOriginBalance = getBalance(issuer);
        log.debug("issuerOriginBalance : {} ", issuerOriginBalance);

        TransactionReceipt receipt = setUpReceipt(transactionId, issuer, BRANCH_ID, 1);

        yeedContract.issuePropose(proposal);

        assertSame(ExecuteStatus.SUCCESS, receipt.getStatus());

        BigInteger issuerBalanceAfterProposalIssued = getBalance(issuer);
        log.debug("Issuer Balance After Proposal Issued : {} ", issuerBalanceAfterProposalIssued);
        assertEquals(0, issuerOriginBalance.subtract(stakeYeed.add(fee)).compareTo(issuerBalanceAfterProposalIssued));
        assertEquals(0, issuerOriginBalance.subtract(issuerBalanceAfterProposalIssued).compareTo(stakeYeed.add(fee)));

        // Get propose ID
        String receiptForProposalIssued = receipt.getTxLog().get(1);
        log.debug("Log 1 : {} ", receiptForProposalIssued);
        String proposeIssueIdPatten = "Propose [a-f0-9]{64} ISSUED";
        Pattern p = Pattern.compile(proposeIssueIdPatten);
        Matcher matcher = p.matcher(receiptForProposalIssued);

        boolean isMatched = matcher.find();
        assertTrue(isMatched);

        String proposeId = matcher.group().replaceAll("Propose ", "").replaceAll(" ISSUED", "");
        log.debug("ProposeId : {}", proposeId);

        // Type-2 Send transaction in ethereum, and get raw transaction

        String ethRawTransaction = "0xf86f830414ac850df847580082afc894c3cf7a283a4415ce3c41f5374934612389"
                + "334780880de0b6b3a76400008026a0c9938e35c6281a2003531ef19c0368fb0ec680d1bc073ee2881"
                + "3602616ce172ca03885e6218dbd7a09fc250ce4eb982114cc25c0974f4adfbd08c4e834f9c74dc3";

        byte[] etheSendEncode = HexUtil.hexStringToBytes(ethRawTransaction);

        // Check raw transaction
        EthTransaction ethTransaction = new EthTransaction(etheSendEncode);
        log.debug("Sender : {} ", HexUtil.toHexString(ethTransaction.getSendAddress()));
        log.debug("Receiver : {}", HexUtil.toHexString(ethTransaction.getReceiverAddress()));
        log.debug("{} WEI", ethTransaction.getValue());

        // Process the issued proposal
        JsonObject processJson = new JsonObject();
        processJson.addProperty("proposeId", proposeId);
        processJson.addProperty("rawTransaction", ethRawTransaction);
        processJson.addProperty("fee", DEFAULT_FEE);

        receipt = setUpReceipt(transactionId, senderAddress, BRANCH_ID, 10);

        yeedContract.processPropose(processJson);

        assertEquals("", ExecuteStatus.SUCCESS, receipt.getStatus());
        printTxLog();

        // Validator transaction confirm (check exist and block height, index)
        // tx id : a077dd42d4421dca5cb8a7a11aca5b27adaf40be2428f9dc8e42a56abdabeb66
        // network : 1
        // validator : 81b7e08f65bdf5648606c89998a9cc8164397647

        // Create a transactionReceipt which the issuer is a validator
        String validator = "81b7e08f65bdf5648606c89998a9cc8164397647";

        receipt = setUpReceipt(transactionId, validator, BRANCH_ID, 100);

        // Create params obj for transaction confirmation
        JsonObject params = new JsonObject();
        //params.addProperty("proposeId", "18d48bc062ecab96a6d5b24791b5b41fdb69a12ef76d56f9f842871f92716b7a");
        params.addProperty("txConfirmId", "af22edcccb4d6e4568b701294479c99a04afbd5ab25578adbe58549e36e4abfa");
        params.addProperty("txId", "a077dd42d4421dca5cb8a7a11aca5b27adaf40be2428f9dc8e42a56abdabeb66");
        params.addProperty("status", TxConfirmStatus.DONE.toValue());
        params.addProperty("blockHeight", 1000001L);
        params.addProperty("lastBlockHeight", 1002001L);
        params.addProperty("index", 1);

        yeedContract.transactionConfirm(params);


        printTxLog();

        // Check txConfirm status
        JsonObject param = new JsonObject();
        param.addProperty("txConfirmId", "af22edcccb4d6e4568b701294479c99a04afbd5ab25578adbe58549e36e4abfa");
        JsonObject queryTxConfirmResult = yeedContract.queryTransactionConfirm(param);
        log.debug(queryTxConfirmResult.toString());
        log.debug("Proposal stake : {} YEED", getBalance(proposeId));

        assertEquals(TxConfirmStatus.DONE.toValue(), queryTxConfirmResult.get("status").getAsInt());
        assertSame(ExecuteStatus.SUCCESS, receipt.getStatus());

        // 1010000000000000000
        //   10000000000000000
        assertEquals("PROPOSE IS DONE",0, getBalance(proposeId).compareTo(BigInteger.ZERO));

        // Check proposal status
        JsonObject proposeQueryParam = new JsonObject();
        proposeQueryParam.addProperty("proposeId", proposeId);
        JsonObject queryProposeResult = yeedContract.queryPropose(proposeQueryParam);
        log.debug(queryProposeResult.get("status").getAsString());
        assertEquals("propose Is DONE", queryProposeResult.get("status").getAsString(), ProposeStatus.DONE.toString());
    }

    @Test
    public void faucetTest() {
        String issuer = "691af5cbc92d8f4e5683246d27d199ccfa2548d6";

        setUpReceipt("0x00", issuer, BRANCH_ID, 1);

        JsonObject emptyParam = new JsonObject();
        BigInteger totalSupply = yeedContract.totalSupply();

        JsonObject testTransfer = new JsonObject();
        testTransfer.addProperty("to","81b7e08f65bdf5648606c89998a9cc8164397647");
        testTransfer.addProperty("amount", BigInteger.valueOf(100L));
        testTransfer.addProperty("fee", DEFAULT_FEE);

        yeedContract.transfer(testTransfer);
        assertSame(ExecuteStatus.ERROR, yeedContract.txReceipt.getStatus()); // Insufficient funds

        yeedContract.faucet(emptyParam);
        assertSame(ExecuteStatus.SUCCESS, yeedContract.txReceipt.getStatus());
        assertTrue(yeedContract.totalSupply().compareTo(totalSupply) != 0);

        // Call faucet one more
        yeedContract.faucet(emptyParam);
        assertSame(ExecuteStatus.ERROR, yeedContract.txReceipt.getStatus()); // Already received or has balance

        yeedContract.transfer(testTransfer);
        assertSame(ExecuteStatus.SUCCESS, yeedContract.txReceipt.getStatus());
    }

    @Test
    public void sendSameAccount() {
        String issuer = "4d01e237570022440aa126ca0b63065d7f5fd589";
        BigInteger balance = yeedContract.getBalance(issuer);
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
        BigInteger serviceFee = DEFAULT_FEE;
        BigInteger amount = BASE_CURRENCY.multiply(BigInteger.valueOf(1000L));
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
        amount = amount.subtract(serviceFee);
        transferChannelTx.addProperty("from", testContractName);
        transferChannelTx.addProperty("to", from);
        transferChannelTx.addProperty("amount", amount);
        log.debug(transferChannelTx.toString());
        result = yeedContract.transferChannel(transferChannelTx);

        assertTrue("Result is True", result);
        BigInteger withdrawBalance = getBalance(from);
        assertEquals("", depositBalance.add(amount), withdrawBalance);

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

    private TransactionReceipt createReceipt(String issuer) {
        TransactionReceipt receipt = new TransactionReceiptImpl("0x00", 200L, issuer);
        receipt.setBranchId(BRANCH_ID);

        return receipt;
    }

    private TransactionReceipt createReceipt(String transactionId, String issuer, String branchId, long blockHeight) {
        TransactionReceipt receipt = new TransactionReceiptImpl(transactionId, 200L, issuer);
        receipt.setBranchId(branchId);
        receipt.setBlockHeight(blockHeight);
        return receipt;
    }

    private void setUpReceipt(TransactionReceipt receipt) {
        adapter.setTransactionReceipt(receipt);
    }

    private TransactionReceipt setUpReceipt(String issuer) {
        TransactionReceipt receipt = createReceipt(issuer);
        setUpReceipt(receipt);
        return receipt;
    }

    private TransactionReceipt setUpReceipt(String transactionId, String issuer, String branchId, long blockHeight) {
        TransactionReceipt receipt = createReceipt(transactionId, issuer, branchId, blockHeight);
        setUpReceipt(receipt);
        return receipt;
    }

    private JsonObject createProposal(String receiverAddress, BigInteger receiveAsset, int receiveChainId,
                                      long networkBlockHeight, ProposeType proposeType, String senderAddress,
                                      String inputData, BigInteger stakeYeed, long targetBlockHeight, BigInteger fee) {
        JsonObject proposal = new JsonObject();
        proposal.addProperty("receiverAddress", receiverAddress);
        proposal.addProperty("receiveAsset", receiveAsset);
        proposal.addProperty("receiveChainId", receiveChainId);
        proposal.addProperty("networkBlockHeight", networkBlockHeight);
        proposal.addProperty("proposeType", proposeType.toValue());
        proposal.addProperty("senderAddress", senderAddress);
        proposal.addProperty("inputData", inputData);
        proposal.addProperty("stakeYeed", stakeYeed);
        proposal.addProperty("blockHeight", targetBlockHeight);
        proposal.addProperty("fee", fee);
        return proposal;
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
        log.debug("txLog={}", adapter.getTxLog());
    }
}
