package io.yggdrash.contract.yeed;

import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.crypto.ECKey;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.rlp.RLP;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class InterChainProcessTest {
    private static final YeedContract.YeedService yeedContract = new YeedContract.YeedService();
    private static final Logger log = LoggerFactory.getLogger(InterChainProcessTest.class);

    private static final String ISSUER = "c3cf7a283a4415ce3c41f5374934612389334780";
    private static final String OTHER_ADDRESS = "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e";
    private static final String BRANCH_ID = "0x00";
    private static final String VALIDATOR = "81b7e08f65bdf5648606c89998a9cc8164397647";
    private TransactionReceiptAdapter adapter;

    private static BigInteger BASE_CURRENCY = BigInteger.TEN.pow(18);
    // 0.01 YEED
    private static BigInteger DEFAULT_FEE = BASE_CURRENCY.divide(BigInteger.valueOf(100L));

    private static String proposeIssueIdPatten = "Propose [a-f0-9]{64} ISSUED";


    private JsonObject genesisParams = JsonUtil.parseJsonObject("{\"alloc\": "
            + "{\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\":{\"balance\": \"1000000000\"},"
            + "\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\":{\"balance\": \"1000000000\"},"
            + "\"5e032243d507c743b061ef021e2ec7fcc6d3ab89\":{\"balance\": \"10\"},"
            + "\"cee3d4755e47055b530deeba062c5bd0c17eb00f\":{\"balance\": \"998000000000\"},"
            + "\"c3cf7a283a4415ce3c41f5374934612389334780\":{\"balance\": \"10000000000000000000000\"},"
            + "\"4d01e237570022440aa126ca0b63065d7f5fd589\":{\"balance\": \"10000000000000000000000\"}"
            + "}}");


    @Before
    public void setUp() {

        adapter = new TransactionReceiptAdapter();
        yeedContract.txReceipt = adapter;
        yeedContract.store = new StateStore(new HashMapDbSource());
        yeedContract.branchStateStore = new InterChainBranchStateStore();

        TransactionReceipt result = new TransactionReceiptImpl();
        setUpReceipt(result);

        yeedContract.init(genesisParams);
        assertTrue(result.isSuccess());

    }



    class InterChainBranchStateStore implements BranchStateStore {
        ValidatorSet set = new ValidatorSet();
        List<BranchContract> contracts;

        public InterChainBranchStateStore() {
            // init
            this.set.getValidatorMap().put(VALIDATOR,
                    new Validator(VALIDATOR));

        }

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
            return null;
        }

        @Override
        public void setValidators(ValidatorSet validatorSet) {
            this.set = validatorSet;

        }

        @Override
        public boolean isValidator(String address) {
            return this.set.contains(address);
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
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private String proposeIssueIdFromLog(List<String> logs) {
        Pattern p = Pattern.compile(proposeIssueIdPatten);
        for(String receiptLog : logs) {
            Matcher matcher = p.matcher(receiptLog);
            boolean isMatched = matcher.find();
            if (isMatched) {
                String proposeIssuedReceiptLog = matcher.group();
                return proposeIssuedReceiptLog
                        .replaceAll("Propose ", "")
                        .replaceAll(" ISSUED", "");
            }
        }
        return null;
    }

    @Test
    public void logTest() {
        List<String> logs = new ArrayList<>();
        logs.add("Propose b4963e78d893e147c77bb124e94aeaa96236e2cc9ad76a22ceec780f5bf51b0e check YEED_TO_ETHER network 1 transaction 0559efaeea4ffd637dac0b1cf941175ced794f7e816d0e4ced28b6e650dd5b9f confirm ID bfdc46db681f6a8121d7aab26c971bfafd3ccc49d5614b81f6f9328934fd0d6c");

        log.debug(proposeConfirmIdFromLog(logs));
    }

    private String proposeConfirmIdFromLog(List<String> logs) {
        for (String receiptLog : logs) {
            if (receiptLog.contains("confirm ID")) {
                int idx = receiptLog.indexOf("confirm ID ");
                return receiptLog.substring(idx + 11);
            }

        }
        return null;
    }
    /**
     * YEED 컨트렉트에서 잔고 조회
     * @param address
     * @return
     */
    private BigInteger getBalance(String address) {
        JsonObject obj = new JsonObject();
        obj.addProperty("address", address);
        return yeedContract.balanceOf(obj);
    }

    private void printTxLog() {
        log.debug("txLog={}", adapter.getTxLog());
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

    private TransactionReceipt setUpReceipt(String transactionId, String issuer, String branchId, long blockHeight) {
        TransactionReceipt receipt = createReceipt(transactionId, issuer, branchId, blockHeight);
        setUpReceipt(receipt);
        return receipt;
    }

    /***
     * Proposal Issuer 생성
     * @param receiverAddress 다른 네트워크에서 에셋을 받을 계정
     * @param receiveAsset 다른 네트워크에서 받을 에셋 수량 (wei 단위)
     * @param receiveChainId 다른 네트워크의 네트워크 아이디 (Ethereum 의 경우 숫자, Branch 의 경우 Branch ID)
     * @param networkBlockHeight 다른 네트워크의 현재 블록 높이 - 해당 이슈 생성시 이전 블록의 트랜잭션은 무효
     * @param proposeType 제안 타입
     * @param senderAddress 다른 네트워크에서 에셋을 보낼 계정
     * @param inputData 다른 네트워크에서 추가로 저장할 데이터
     * @param stakeYeed 이그드라시 네티워크에 에스크로할 이드의 수량
     * @param targetBlockHeight 해당 이슈의 소멸기한
     * @param fee 해당 이슈를 생성할 수수료
     * @return Proposal Issue 파라미터
     */
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


    @Test
    public void issueProposeUpperCase() {
        final String transactionId = "0x00"; // 임시
        String receiverAddress = "cee3d4755e47055b530deeba062c5bd0c17eb00f";
        String issuer = receiverAddress;
        String senderAddress = "C91E9D46DD4B7584F0B6348EE18277C10FD7CB94";


        // 1 ETH
        BigInteger receiveAsset = BigInteger.TEN.pow(18);

        // Stake is 100 YEED
        BigInteger stakeYeed = BigInteger.TEN.pow(20);

        JsonObject proposal = createProposal(
                receiverAddress, receiveAsset, 1, 10,
                ProposeType.YEED_TO_ETHER, senderAddress, null, stakeYeed,
                1000000L, DEFAULT_FEE
        );

        log.debug("invalide proposal {}", proposal.toString());


        TransactionReceipt receipt = setUpReceipt(transactionId, issuer, BRANCH_ID, 1); // 1 == current blockHeight

        yeedContract.issuePropose(proposal);

        assertEquals("SUCCESS", ExecuteStatus.SUCCESS, receipt.getStatus());

        receipt.getTxLog().stream().forEach(l -> log.debug(l));
    }


    /***
     * 신규이슈를 생성하는 테스트
     */
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

        String proposeId = proposeIssueIdFromLog(receipt.getTxLog());
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
        receipt = setUpReceipt(transactionId, VALIDATOR, BRANCH_ID, 100);

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
    public void rawTxGenerator() {

        //EthereumTransaction tx = new EthereumTransaction();


        byte[] nonce = RLP.encode(Integer.valueOf(1000));
        byte[] gasPrice = RLP.encode(Integer.valueOf(1000));
        byte[] gasLimit = RLP.encode(Integer.valueOf(1000));
        byte[] receiveAddress = RLP.encodeElement(HexUtil.hexStringToBytes("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94"));
        byte[] value = RLP.encodeElement(BigInteger.TEN.pow(18).toByteArray());
        byte[] data = RLP.encodeElement(HexUtil.hexStringToBytes("0x"));

        byte[] hash = RLP.encodeList(nonce, gasPrice, gasLimit, receiveAddress, value, data);
        byte[] sha3hash = HashUtil.sha3(hash);

        EthereumTransaction tx = new EthereumTransaction(
                0,
                BigInteger.TEN.pow(10), // gasPrice
                21000,
                "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94",
                BigInteger.TEN.pow(18),
                null,
                1
        );
        ECKey key = new ECKey();
        tx.sign(key);

        Assert.assertArrayEquals("", tx.getSender(), key.getAddress());

        log.debug(HexUtil.toHexString(key.getAddress()));
        log.debug(HexUtil.toHexString(tx.getEncoded()));

        log.debug(HexUtil.toHexString(nonce));
    }

    private String issuerPropose(String issuer, String senderAddress, BigInteger stakeYeed, BigInteger fee) {

        BigInteger receiveAsset = BigInteger.TEN.pow(18);
        // StakeYeed = 1YEED
        // Ratio -> 1:1
        int receiveNetwork = 1; // frontier = 1

        JsonObject propose = createProposal(
                ISSUER,
                receiveAsset,
                receiveNetwork,
                100000,
                ProposeType.YEED_TO_ETHER,
                senderAddress,
                null,
                stakeYeed,
                1000,
                fee);

        // Check Balance Issuer
        BigInteger issuerBalance = getBalance(ISSUER);
        BigInteger senderBalance = getBalance(senderAddress);

        TransactionReceipt issueReceipt = setUpReceipt("0x00", ISSUER, BRANCH_ID, 1); // 1 == current blockHeight

        yeedContract.issuePropose(propose);

        Assert.assertEquals("ISSUE SUCCESS", ExecuteStatus.SUCCESS, issueReceipt.getStatus());

        // Get Propose ID
        String proposeId = proposeIssueIdFromLog(issueReceipt.getTxLog());
        Assert.assertNotNull(proposeId);

        BigInteger issueAfterBalance = getBalance(ISSUER);

        Assert.assertTrue("Issuer Subtract Balance",
                issuerBalance.subtract(stakeYeed).subtract(fee)
                        .compareTo(issueAfterBalance) == 0);

        return proposeId;
    }

    @Test
    public void proposeFullTest() {
        // 정상의 경우
        // Create Issue By Receiver
        // Receive Address ISSUER
        // Receive Asset 1ETH
        // Receive Network frontier

        // StakeYeed = 1YEED
        // Ratio -> 1:1
        BigInteger stakeYeed = BigInteger.TEN.pow(18);
        BigInteger fee = BigInteger.TEN.pow(17); // 0.1 YEED
        ECKey senderKey = new ECKey();
        String senderAddress = HexUtil.toHexString(senderKey.getAddress());

        // Check Balance Issuer
        BigInteger issuerBalance = getBalance(ISSUER);
        BigInteger senderBalance = getBalance(senderAddress);

        String proposeId = issuerPropose(ISSUER, senderAddress, stakeYeed, fee);

        BigInteger issueAfterBalance = getBalance(ISSUER);

        Assert.assertTrue("Issuer Subtract Balance",
                issuerBalance.subtract(stakeYeed).subtract(fee)
                .compareTo(issueAfterBalance) == 0);

        TransactionReceipt sendReceipt = setUpReceipt("0x00", ISSUER, BRANCH_ID, 100);
        // Send YEED to senderAddress for Test
        yeedContract.transfer(ISSUER, senderAddress, BigInteger.TEN.pow(18), BigInteger.TEN.pow(17));

        Assert.assertTrue("", getBalance(senderAddress).compareTo(BigInteger.ZERO) > 0);

        // Create Ethereum Transactiono by Sender
        //
        EthereumTransaction ethereumTx = new EthereumTransaction(
                0,
                BigInteger.ZERO, // GasPrice
                0, // GasLimit
                ISSUER, // in ethereum network receiver is issuer
                BigInteger.TEN.pow(18), // 1ETH
                null,
                1 // frontier
        );
        // sender sign and send to network
        ethereumTx.sign(senderKey);

        byte[] rawTransaction = ethereumTx.getEncoded();
        String rawTxString = HexUtil.toHexString(rawTransaction);
        TransactionReceipt proposeReceipt =
                setUpReceipt("0x00", senderAddress, BRANCH_ID, 100);

        JsonObject processJson = new JsonObject();
        processJson.addProperty("proposeId", proposeId);
        processJson.addProperty("rawTransaction", rawTxString);
        processJson.addProperty("fee", fee); // 0.1

        // Issuer process
        yeedContract.processPropose(processJson);

        Assert.assertEquals("Propose processing", ExecuteStatus.SUCCESS, proposeReceipt.getStatus());

        // Confirm By Validator
        String confirmId = proposeConfirmIdFromLog(proposeReceipt.getTxLog());
        Assert.assertNotNull(confirmId);
        BigInteger senderBeforeBalance = getBalance(senderAddress);

        TransactionReceipt confirmReceipt = setUpReceipt("0x00", VALIDATOR, BRANCH_ID, 101);

        // Create params obj for transaction confirmation
        JsonObject params = new JsonObject();
        //params.addProperty("proposeId", "18d48bc062ecab96a6d5b24791b5b41fdb69a12ef76d56f9f842871f92716b7a");
        params.addProperty("txConfirmId", confirmId);
        params.addProperty("txId", HexUtil.toHexString(ethereumTx.getHash()));
        params.addProperty("status", TxConfirmStatus.DONE.toValue());
        params.addProperty("blockHeight", 100010L);
        params.addProperty("lastBlockHeight", 100030L);
        params.addProperty("index", 1);

        yeedContract.transactionConfirm(params);

        Assert.assertEquals("SUCCESS ", ExecuteStatus.SUCCESS, confirmReceipt.getStatus());
        BigInteger senderAfterBalance = getBalance(senderAddress);

        Assert.assertTrue("전송전과 전송 후가 같다", senderAfterBalance.subtract(senderBeforeBalance)
                .compareTo(stakeYeed) == 0);
    }

    @Test
    public void retryEthTransaction() {
        BigInteger stakeYeed = BigInteger.TEN.pow(18);
        BigInteger fee = BigInteger.TEN.pow(17); // 0.1 YEED
        ECKey senderKey = new ECKey();
        String senderAddress = HexUtil.toHexString(senderKey.getAddress());


        yeedContract.transfer(ISSUER, senderAddress, BigInteger.TEN.pow(18), BigInteger.TEN.pow(17));

        String proposeId = issuerPropose(ISSUER, senderAddress, stakeYeed, fee);


        EthereumTransaction ethereumTx = new EthereumTransaction(
                0,
                BigInteger.ZERO, // GasPrice
                0, // GasLimit
                ISSUER, // in ethereum network receiver is issuer
                BigInteger.TEN.pow(17).multiply(BigInteger.valueOf(5L)), // 0.5ETH
                null,
                1 // frontier
        );
        // sender sign and send to network
        ethereumTx.sign(senderKey);

        byte[] rawTransaction = ethereumTx.getEncoded();
        String rawTxString = HexUtil.toHexString(rawTransaction);
        TransactionReceipt proposeReceipt =
                setUpReceipt("0x00", senderAddress, BRANCH_ID, 100);

        JsonObject processJson = new JsonObject();
        processJson.addProperty("proposeId", proposeId);
        processJson.addProperty("rawTransaction", rawTxString);
        processJson.addProperty("fee", fee); // 0.1

        yeedContract.processPropose(processJson);
        printTxLog();
        Assert.assertEquals("Propose processing SUCCESS", ExecuteStatus.SUCCESS, proposeReceipt.getStatus());


        // retry same processing
        thrown.expect(RuntimeException.class);
        yeedContract.processPropose(processJson);
        printTxLog();
        Assert.assertEquals("Propose processing ERROR", ExecuteStatus.ERROR, proposeReceipt.getStatus());



    }

    @Test
    public void invalidRawTransaction() {
        BigInteger stakeYeed = BigInteger.TEN.pow(18);
        BigInteger fee = BigInteger.TEN.pow(17); // 0.1 YEED
        ECKey senderKey = new ECKey();
        String senderAddress = HexUtil.toHexString(senderKey.getAddress());


        yeedContract.transfer(ISSUER, senderAddress, BigInteger.TEN.pow(18), BigInteger.TEN.pow(17));

        String proposeId = issuerPropose(ISSUER, senderAddress, stakeYeed, fee);


        EthereumTransaction ethereumTx = new EthereumTransaction(
                0,
                BigInteger.ZERO, // GasPrice
                0, // GasLimit
                ISSUER, // in ethereum network receiver is issuer
                BigInteger.TEN.pow(17).multiply(BigInteger.valueOf(5L)), // 0.5ETH
                null,
                1 // frontier
        );

        ethereumTx.sign(senderKey);

        log.debug(HexUtil.toHexString(ethereumTx.getValue()));

        // rawValue Change, Value 1ETH to 1ETH - 65536 WEI
        byte[] rawTransaction = ethereumTx.getEncoded();
        String rawTxString = HexUtil.toHexString(rawTransaction);
        rawTxString = rawTxString.replaceAll("06f05b59d3b20000", "06f05b59d3b30000");

        TransactionReceipt proposeReceipt =
                setUpReceipt("0x00", senderAddress, BRANCH_ID, 100);

        JsonObject processJson = new JsonObject();
        processJson.addProperty("proposeId", proposeId);
        processJson.addProperty("rawTransaction", rawTxString);
        processJson.addProperty("fee", fee); // 0.1

        yeedContract.processPropose(processJson);
        printTxLog();

        Assert.assertEquals("Propose processing", ExecuteStatus.FALSE, proposeReceipt.getStatus());
    }

    @Test
    public void diviedEthSendTest() {
        BigInteger stakeYeed = BigInteger.TEN.pow(18);
        BigInteger fee = BigInteger.TEN.pow(17); // 0.1 YEED
        ECKey senderKey = new ECKey();
        String senderAddress = HexUtil.toHexString(senderKey.getAddress());


        yeedContract.transfer(ISSUER, senderAddress, BigInteger.TEN.pow(18), BigInteger.TEN.pow(17));

        String proposeId = issuerPropose(ISSUER, senderAddress, stakeYeed, fee);

        BigInteger halfEth = BigInteger.TEN.pow(17).multiply(BigInteger.valueOf(5L));
        EthereumTransaction ethereum1Tx = new EthereumTransaction(
                0,
                BigInteger.ZERO, // GasPrice
                0, // GasLimit
                ISSUER, // in ethereum network receiver is issuer
                halfEth, // 0.5ETH
                null,
                1 // frontier
        );
        ethereum1Tx.sign(senderKey);

        EthereumTransaction ethereum2Tx = new EthereumTransaction(
                1,
                BigInteger.ZERO, // GasPrice
                0, // GasLimit
                ISSUER, // in ethereum network receiver is issuer
                halfEth, // 0.5ETH
                null,
                1 // frontier
        );
        ethereum2Tx.sign(senderKey);

        BigInteger proposeStakeYeed = getBalance(proposeId);
        log.debug("Propose Stake YEED : {}", proposeStakeYeed);
        TransactionReceipt proposeReceipt =
                setUpReceipt("0x0001", senderAddress, BRANCH_ID, 100);

        JsonObject processJson = new JsonObject();
        processJson.addProperty("proposeId", proposeId);
        processJson.addProperty("rawTransaction", HexUtil.toHexString(ethereum1Tx.getEncoded()));
        processJson.addProperty("fee", fee); // 0.1

        yeedContract.processPropose(processJson);

        Assert.assertTrue("Process Done", proposeReceipt.isSuccess());

        Assert.assertTrue("", proposeStakeYeed.compareTo(getBalance(proposeId)) == 0);
        log.debug("Propose Stake YEED : {}", proposeStakeYeed);

        String confirm1Id = proposeConfirmIdFromLog(proposeReceipt.getTxLog());
        Assert.assertNotNull("", confirm1Id);

        // replace ethereum Tx2
        processJson.addProperty("rawTransaction", HexUtil.toHexString(ethereum2Tx.getEncoded()));
        proposeReceipt =
                setUpReceipt("0x0002", senderAddress, BRANCH_ID, 100);
        yeedContract.processPropose(processJson);

        Assert.assertTrue("Process Done", proposeReceipt.isSuccess());
        String confirm2Id = proposeConfirmIdFromLog(proposeReceipt.getTxLog());
        Assert.assertNotNull("", confirm2Id);

        TransactionReceipt confirmTr = setUpReceipt("0x00", VALIDATOR, BRANCH_ID, 100);

        JsonObject confirmParams = new JsonObject();
        //params.addProperty("proposeId", "18d48bc062ecab96a6d5b24791b5b41fdb69a12ef76d56f9f842871f92716b7a");
        confirmParams.addProperty("txConfirmId", confirm1Id);
        confirmParams.addProperty("txId", HexUtil.toHexString(ethereum1Tx.getHash()));
        confirmParams.addProperty("status", TxConfirmStatus.DONE.toValue());
        confirmParams.addProperty("blockHeight", 100010L);
        confirmParams.addProperty("lastBlockHeight", 100030L);
        confirmParams.addProperty("index", 1);

        yeedContract.transactionConfirm(confirmParams);
        proposeStakeYeed = proposeStakeYeed.subtract(halfEth);
        BigInteger confirm1Balance = getBalance(proposeId);
        log.debug("Remain Stake YEED : {} ", confirm1Balance);
        Assert.assertTrue("", proposeStakeYeed.compareTo(confirm1Balance) == 0);
        Assert.assertTrue("", confirmTr.isSuccess());

        confirmParams.addProperty("txConfirmId", confirm2Id);
        confirmParams.addProperty("txId", HexUtil.toHexString(ethereum2Tx.getHash()));
        confirmParams.addProperty("status", TxConfirmStatus.DONE.toValue());

        yeedContract.transactionConfirm(confirmParams);
        Assert.assertTrue("", confirmTr.isSuccess());


    }

}
