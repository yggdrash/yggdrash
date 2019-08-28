package io.yggdrash.contract.token;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.store.BranchStateStore;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.ContractUtils;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptAdapter;
import io.yggdrash.contract.core.TransactionReceiptImpl;
import io.yggdrash.core.blockchain.osgi.ContractCache;
import io.yggdrash.core.blockchain.osgi.ContractCacheImpl;
import io.yggdrash.core.blockchain.osgi.ContractChannelCoupler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenContractTest {

    private static final Logger log = LoggerFactory.getLogger(TokenContractTest.class);

    private TransactionReceiptAdapter adapter;

    TestYeed testYeed;
    StateStore stateStore;

    ContractCache cache;
    Map<String, Object> contractMap = new HashMap<>();
    ContractChannelCoupler coupler;
    TokenContract.TokenService tokenContract = new TokenContract.TokenService();

    @Before
    public void setUp() throws Exception {

        stateStore = new StateStore(new HashMapDbSource());
        adapter = new TransactionReceiptAdapter();
        testYeed = new TestYeed();

        tokenContract.txReceipt = adapter;
        testYeed.setTxReceipt(adapter);
        BranchStateStore branchStateStore = new TokenBranchStateStore();
        branchStateStore.getValidators().getValidatorMap()
                .put("81b7e08f65bdf5648606c89998a9cc8164397647",
                        new Validator("81b7e08f65bdf5648606c89998a9cc8164397647"));

        tokenContract.branchStateStore = branchStateStore;
        tokenContract.store = stateStore;

        // Default TxReceipt
        TransactionReceipt result = new TransactionReceiptImpl();

        // apply txReceipt
        adapter.setTransactionReceipt(result);

        // ADD contract coupler
        coupler = new ContractChannelCoupler();
        cache = new ContractCacheImpl();
        coupler.setContract(contractMap, cache);

        //for (Field f : ContractUtils.contractFields(stemContract, ContractChannelField.class)) {
        for (Field f : ContractUtils.contractChannelField(tokenContract)) {
            f.setAccessible(true);
            f.set(tokenContract, coupler);
        }

        cache.cacheContract("TOKEN", tokenContract);
        cache.cacheContract("YEED", testYeed);

        contractMap.put("0x01", tokenContract);
        contractMap.put("0x00", testYeed);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void initTest() {
        JsonObject emptyParam = new JsonObject();
        tokenContract.init(emptyParam);

        Assert.assertEquals("adapter status should be SUCCESS", ExecuteStatus.SUCCESS, adapter.getStatus());
    }

    @Test
    public void createToken() {
        createToken(null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    public void createTokenDuplicate() {
        createToken("0x01", null, null, null, null, null, null, null, null, null, null);

        TransactionReceipt tx = new TransactionReceiptImpl("0x02", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        JsonObject createToken = new JsonObject();
        createToken.addProperty("tokenId", "TEST_TOKEN");
        createToken.addProperty("tokenName", "TTOKEN");
        createToken.addProperty("tokenInitYeedStakeAmount", BigInteger.TEN.pow(24));
        createToken.addProperty("tokenInitMintAmount", BigInteger.TEN.pow(30));
        createToken.addProperty("tokenMintable", true);
        createToken.addProperty("tokenBurnable", true);
        createToken.addProperty("tokenExchangeable", true);
        createToken.addProperty("tokenExType", "TOKEN_EX_TYPE_FIXED");
        createToken.addProperty("tokenExRateT2Y", 1.0);

        tokenContract.createToken(createToken);
        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Duplicated token creation should be failed", tx.isSuccess());
    }

    @Test
    public void totalSupply() {
        TransactionReceipt tx = _testInit();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");

        BigInteger totalSupply = tokenContract.totalSupply(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals("Total supply should match with initial mint", 0, totalSupply.compareTo(BigInteger.TEN.pow(30)));
    }

    @Test
    public void totalSupplyNoneToken() {
        TransactionReceipt tx = _testInit();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        BigInteger totalSupply = tokenContract.totalSupply(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals("The result must be null", null, totalSupply);
    }

    @Test
    public void balanceOf() {
        TransactionReceipt tx = _testInit();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");
        params.addProperty("address", tx.getIssuer());

        BigInteger result = tokenContract.totalSupply(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals("The balance should match with initial mint!!!", 0, result.compareTo(BigInteger.TEN.pow(30)));
    }

    @Test
    public void balanceOfNoneToken() {
        TransactionReceipt tx = _testInit();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");
        params.addProperty("address", tx.getIssuer());

        BigInteger result = tokenContract.totalSupply(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals("The result must be null", null, result);
    }

    @Test
    public void getYeedBalanceOf() {
        TransactionReceipt tx = _testInit();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");

        BigInteger result = tokenContract.getYeedBalanceOf(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals("The result should match with initial YEED stake", 0, result.compareTo(BigInteger.TEN.pow(24)));
    }

    @Test
    public void getYeedBalanceOfNoneToken() {
        TransactionReceipt tx = _testInit();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        BigInteger result = tokenContract.getYeedBalanceOf(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals("The result must be null", null, result);
    }


    @Test
    public void allowance() {
        approve();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");
        params.addProperty("owner", "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        params.addProperty("spender", "0000000000000000000000000000000000000000");

        BigInteger result = tokenContract.allowance(params);

        Assert.assertTrue("The result should match with approved value", result.compareTo(BigInteger.valueOf(100000).multiply(BigInteger.TEN.pow(18))) == 0);
    }

    @Test
    public void allowanceNoneToken() {
        approve();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");
        params.addProperty("owner", "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        params.addProperty("spender", "0000000000000000000000000000000000000000");

        BigInteger result = tokenContract.allowance(params);

        Assert.assertEquals("The result must be null", null, result);
    }

    @Test
    public void allowanceNotApprovedAccount() {
        approve();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");
        params.addProperty("owner", "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        params.addProperty("spender", "1111111111111111111111111111111111111111");

        BigInteger result = tokenContract.allowance(params);

        Assert.assertEquals("The allowance of not approved account should be ZERO", 0, result.compareTo(BigInteger.ZERO));
    }

    @Test
    public void depositYeedStake() {
        TransactionReceipt tx = _testInit();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");
        params.addProperty("amount", BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));

        tokenContract.depositYeedStake(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Additional deposit of YEED stake is failed", tx.isSuccess());

        BigInteger result = tokenContract.getYeedBalanceOf(params);

        Assert.assertEquals("The result should match with current YEED stake", 0, result.compareTo(BigInteger.valueOf(1000100).multiply(BigInteger.TEN.pow(18))));
    }

    @Test
    public void depositYeedStakeNoneToken() {
        TransactionReceipt tx = _testInit();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");
        params.addProperty("amount", BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));

        tokenContract.depositYeedStake(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("The deposit to nonexistent token should be failed", tx.isSuccess());
    }

    @Test
    public void depositYeedStakeNotOwner() {
        _testInit();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");
        params.addProperty("amount", BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));

        String issuer = "1111111111111111111111111111111111111111";
        TransactionReceipt tx = new TransactionReceiptImpl("0x04", 300L, issuer);
        this.adapter.setTransactionReceipt(tx);

        tokenContract.depositYeedStake(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Deposit by who does not own token should be failed", tx.isSuccess());
    }

    @Test
    public void depositYeedStakeOverBalance() {
        TransactionReceipt tx = _testInit();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");
        params.addProperty("amount", BigInteger.TEN.pow(50)); // balance == TEN.pow(40)

        tokenContract.depositYeedStake(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("", tx.isSuccess());
    }

    @Test
    public void withdrawYeedStake() {
        TransactionReceipt tx = _testInit();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");
        params.addProperty("amount", BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));

        tokenContract.withdrawYeedStake(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Withdraw of YEED stake is failed", tx.isSuccess());

        BigInteger result = tokenContract.getYeedBalanceOf(params);

        Assert.assertEquals("The result should match with current YEED stake", 0, result.compareTo(BigInteger.valueOf(999900).multiply(BigInteger.TEN.pow(18))));
    }

    @Test
    public void withdrawYeedStakeNoneToken() {
        TransactionReceipt tx = _testInit();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");
        params.addProperty("amount", BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));

        tokenContract.withdrawYeedStake(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Withdraw from nonexistent token should be failed", tx.isSuccess());
    }

    @Test
    public void withdrawYeedStakeNotOwner() {
        _testInit();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");
        params.addProperty("amount", BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));

        String issuer = "1111111111111111111111111111111111111111";
        TransactionReceipt tx = new TransactionReceiptImpl("0x04", 300L, issuer);
        this.adapter.setTransactionReceipt(tx);

        tokenContract.withdrawYeedStake(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Withdraw by who does not own token should be failed", tx.isSuccess());
    }

    @Test
    public void withdrawYeedStakeOverBalance() {
        TransactionReceipt tx = _testInit();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");
        params.addProperty("amount", BigInteger.valueOf(1000000000000000L).multiply(BigInteger.TEN.pow(18)));

        tokenContract.withdrawYeedStake(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Withdraw over YEED stake balance should be failed", tx.isSuccess());
    }

    @Test
    public void movePhaseRun() {
        createToken();

        // NONE_TOKEN
        TransactionReceipt tx = new TransactionReceiptImpl("0x02", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.movePhaseRun(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move of nonexistent token should be failed", tx.isSuccess());

        // NOT_OWNER
        params.addProperty("tokenId", "TEST_TOKEN");

        tx = new TransactionReceiptImpl("0x03", 300L, "1111111111111111111111111111111111111111");
        this.adapter.setTransactionReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move by who does not own token should be failed", tx.isSuccess());

        // INIT -> RUN NORMAL
        tx = new TransactionReceiptImpl("0x04", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from INIT to RUN is failed", tx.isSuccess());

        // RUN -> PAUSE -> RUN NORMAL
        tx = new TransactionReceiptImpl("0x05", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        tokenContract.movePhasePause(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from RUN to PAUSE is failed", tx.isSuccess());

        tx = new TransactionReceiptImpl("0x06", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from PAUSE to RUN is failed", tx.isSuccess());

        // RUN -> RUN FAIL
        tx = new TransactionReceiptImpl("0x07", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move RUN to RUN should be failed", tx.isSuccess());
    }

    @Test
    public void movePhasePause() {
        createToken();

        // NONE_TOKEN
        TransactionReceipt tx = new TransactionReceiptImpl("0x02", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.movePhasePause(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move of nonexistent token should be failed", tx.isSuccess());

        // NOT_OWNER
        params.addProperty("tokenId", "TEST_TOKEN");

        tx = new TransactionReceiptImpl("0x03", 300L, "1111111111111111111111111111111111111111");
        this.adapter.setTransactionReceipt(tx);

        tokenContract.movePhasePause(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move by who does not own token should be failed", tx.isSuccess());

        // RUN -> PAUSE NORMAL
        tx = new TransactionReceiptImpl("0x04", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from INIT to RUN is failed", tx.isSuccess());

        tx = new TransactionReceiptImpl("0x05", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        tokenContract.movePhasePause(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from RUN to PAUSE is failed", tx.isSuccess());

        // PAUSE -> PAUSE FAIL
        tx = new TransactionReceiptImpl("0x07", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        tokenContract.movePhasePause(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move PAUSE to PAUSE should be failed", tx.isSuccess());
    }

    @Test
    public void movePhaseStop() {
        createToken();

        // NONE_TOKEN
        TransactionReceipt tx = new TransactionReceiptImpl("0x02", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.movePhaseStop(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move of nonexistent token should be failed", tx.isSuccess());

        // NOT_OWNER
        params.addProperty("tokenId", "TEST_TOKEN");

        tx = new TransactionReceiptImpl("0x03", 300L, "1111111111111111111111111111111111111111");
        this.adapter.setTransactionReceipt(tx);

        tokenContract.movePhaseStop(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move by who does not own token should be failed", tx.isSuccess());

        // RUN -> PAUSE -> STOP NORMAL
        tx = new TransactionReceiptImpl("0x04", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from INIT to RUN is failed", tx.isSuccess());

        tx = new TransactionReceiptImpl("0x05", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        tokenContract.movePhasePause(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from RUN to PAUSE is failed", tx.isSuccess());

        tx = new TransactionReceiptImpl("0x06", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        tokenContract.movePhaseStop(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from PAUSE to STOP is failed", tx.isSuccess());

        // STOP -> STOP FAIL
        tx = new TransactionReceiptImpl("0x07", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        tokenContract.movePhaseStop(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move STOP to STOP should be failed", tx.isSuccess());

        // STOP -> RUN FAIL
        tx = new TransactionReceiptImpl("0x08", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move STOP to RUN should be failed", tx.isSuccess());
    }

    @Test
    public void transfer() {
        createToken();

        // NONE_TOKEN
        String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        String account1 = "1111111111111111111111111111111111111111";

        TransactionReceipt tx = new TransactionReceiptImpl("0x02", 300L, owner);
        this.adapter.setTransactionReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.transfer(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of nonexistent token should be failed", tx.isSuccess());

        // NOT_RUNNING
        tx = new TransactionReceiptImpl("0x03", 300L, owner);
        this.adapter.setTransactionReceipt(tx);

        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.transfer(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of not running token should be failed", tx.isSuccess());

        // move phase to run
        tx = new TransactionReceiptImpl("0x04", 300L, owner);
        this.adapter.setTransactionReceipt(tx);
        tokenContract.movePhaseRun(params);

        // NEGATIVE AMOUNT
        tx = new TransactionReceiptImpl("0x05", 300L, owner);
        this.adapter.setTransactionReceipt(tx);

        params.addProperty("to", account1);
        params.addProperty("amount", BigInteger.valueOf(-1).multiply(BigInteger.TEN.pow(18)));

        tokenContract.transfer(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of negative amount should be failed", tx.isSuccess());

        // NORMAL
        tx = new TransactionReceiptImpl("0x06", 300L, owner);
        this.adapter.setTransactionReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));

        tokenContract.transfer(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Transfer is failed", tx.isSuccess());

        // INSUFFICIENT BALANCE
        tx = new TransactionReceiptImpl("0x07", 300L, account1);
        this.adapter.setTransactionReceipt(tx);

        params.addProperty("to", owner);
        params.addProperty("amount", BigInteger.valueOf(101).multiply(BigInteger.TEN.pow(18)));

        tokenContract.transfer(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer from insufficient balance should be failed", tx.isSuccess());
    }

    @Test
    public void approve() {
        TransactionReceipt tx = _testInit();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");
        params.addProperty("amount", BigInteger.valueOf(100000).multiply(BigInteger.TEN.pow(18)));
        params.addProperty("spender", "0000000000000000000000000000000000000000");

        tokenContract.approve(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("The approve is failed", tx.isSuccess());
    }

    @Test
    public void approveOverBalance() {
        TransactionReceipt tx = _testInit();

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");
        params.addProperty("amount", BigInteger.valueOf(1000000000001L).multiply(BigInteger.TEN.pow(18)));
        params.addProperty("spender", "0000000000000000000000000000000000000000");

        // try approve initMint + 1
        tokenContract.approve(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("The approve with amount over sender balance should be failed", tx.isSuccess());
    }

    @Test
    public void approveNoneAccount() {
        _testInit();

        String issuer = "1111111111111111111111111111111111111111";
        TransactionReceipt tx = new TransactionReceiptImpl("0x04", 300L, issuer);
        this.adapter.setTransactionReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");
        params.addProperty("amount", BigInteger.valueOf(1000000000001L).multiply(BigInteger.TEN.pow(18)));
        params.addProperty("spender", "0000000000000000000000000000000000000000");

        tokenContract.approve(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("The approve on nonexistent token should be failed", tx.isSuccess());
    }

    @Test
    public void transferFrom() {
        createToken();

        String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        String account1 = "1111111111111111111111111111111111111111";
        String account2 = "2222222222222222222222222222222222222222";

        // NONEXISTENT
        TransactionReceipt tx = new TransactionReceiptImpl("0x02", 300L, account1);
        this.adapter.setTransactionReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.transferFrom(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of nonexistent token should be failed", tx.isSuccess());

        // NOT RUNNING
        tx = new TransactionReceiptImpl("0x03", 300L, account1);
        this.adapter.setTransactionReceipt(tx);

        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.transferFrom(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of nonexistent token should be failed", tx.isSuccess());

        // move phase to run
        tx = new TransactionReceiptImpl("0x04", 300L, owner);
        this.adapter.setTransactionReceipt(tx);
        tokenContract.movePhaseRun(params);

        // NEGATIVE AMOUNT
        tx = new TransactionReceiptImpl("0x05", 300L, account1);
        this.adapter.setTransactionReceipt(tx);

        params.addProperty("from", owner);
        params.addProperty("to", account2);
        params.addProperty("amount", BigInteger.valueOf(-1).multiply(BigInteger.TEN.pow(18)));

        tokenContract.transferFrom(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of negative amount should be failed", tx.isSuccess());

        // approve 100
        tx = new TransactionReceiptImpl("0x06", 300L, owner);
        this.adapter.setTransactionReceipt(tx);

        JsonObject paramsApprove = new JsonObject();
        paramsApprove.addProperty("tokenId", "TEST_TOKEN");
        paramsApprove.addProperty("amount", BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));
        paramsApprove.addProperty("spender", account1);

        tokenContract.approve(paramsApprove);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("The approve is failed", tx.isSuccess());

        // INSUFFICIENT BALANCE 200
        tx = new TransactionReceiptImpl("0x07", 300L, account1);
        this.adapter.setTransactionReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(200).multiply(BigInteger.TEN.pow(18)));

        tokenContract.transferFrom(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer from insufficient approved balance should be failed", tx.isSuccess());

        // NORMAL 50
        tx = new TransactionReceiptImpl("0x08", 300L, account1);
        this.adapter.setTransactionReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(50).multiply(BigInteger.TEN.pow(18)));

        tokenContract.transferFrom(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Transfer from approved is failed", tx.isSuccess());

        // ALLOWANCE 50
        params.addProperty("owner", owner);
        params.addProperty("spender", account1);
        BigInteger result = tokenContract.allowance(params);
        Assert.assertEquals("Allowance should be 50", 0, result.compareTo(BigInteger.valueOf(50).multiply(BigInteger.TEN.pow(18))));
    }

    @Test
    public void mint() {
        createToken();

        String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        String account1 = "1111111111111111111111111111111111111111";

        // NONEXISTENT TOKEN
        TransactionReceipt tx = new TransactionReceiptImpl("0x02", 300L, owner);
        this.adapter.setTransactionReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.mint(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Mint of nonexistent token should be failed", tx.isSuccess());

        // NOT OWNER
        tx = new TransactionReceiptImpl("0x03", 300L, account1);
        this.adapter.setTransactionReceipt(tx);

        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.mint(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Mint by non-owner account should be failed", tx.isSuccess());

        // NEGATIVE AMOUNT
        tx = new TransactionReceiptImpl("0x05", 300L, owner);
        this.adapter.setTransactionReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(-1).multiply(BigInteger.TEN.pow(18)));

        tokenContract.mint(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Mint of negative amount should be failed", tx.isSuccess());

        // NORMAL
        tx = new TransactionReceiptImpl("0x06", 300L, owner);
        this.adapter.setTransactionReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));

        tokenContract.mint(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Mint is failed", tx.isSuccess());

        // TOTAL SUPPLY
        BigInteger expected = BigInteger.TEN.pow(30).add(BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));
        BigInteger result = tokenContract.totalSupply(params);
        Assert.assertEquals("Total supply should be same as expected", expected, result);

        // move phase to stop
        tx = new TransactionReceiptImpl("0x07", 300L, owner);
        this.adapter.setTransactionReceipt(tx);
        tokenContract.movePhaseRun(params);

        tx = new TransactionReceiptImpl("0x08", 300L, owner);
        this.adapter.setTransactionReceipt(tx);
        tokenContract.movePhaseStop(params);

        // STOPPED PHASE
        tx = new TransactionReceiptImpl("0x09", 300L, owner);
        this.adapter.setTransactionReceipt(tx);

        tokenContract.mint(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Mint at stop phase should be failed", tx.isSuccess());
    }

    @Test
    public void mintNotMintable() {
        Boolean mintable = false;
        createToken(null, null, null, null, null, null, mintable, null, null, null, null);

        TransactionReceipt tx = new TransactionReceiptImpl("0x02", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.mint(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Mint of non-mintable token should be failed", tx.isSuccess());
    }

    @Test
    public void burn() {
        createToken();

        String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        String account1 = "1111111111111111111111111111111111111111";

        // NONEXISTENT TOKEN
        TransactionReceipt tx = new TransactionReceiptImpl("0x02", 300L, owner);
        this.adapter.setTransactionReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.burn(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Burn of nonexistent token should be failed", tx.isSuccess());

        // NOT OWNER
        tx = new TransactionReceiptImpl("0x03", 300L, account1);
        this.adapter.setTransactionReceipt(tx);

        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.burn(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Burn by non-owner account should be failed", tx.isSuccess());

        // NEGATIVE AMOUNT
        tx = new TransactionReceiptImpl("0x05", 300L, owner);
        this.adapter.setTransactionReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(-1).multiply(BigInteger.TEN.pow(18)));

        tokenContract.burn(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Burn of negative amount should be failed", tx.isSuccess());

        // NORMAL
        tx = new TransactionReceiptImpl("0x06", 300L, owner);
        this.adapter.setTransactionReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));

        tokenContract.burn(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Burn is failed", tx.isSuccess());

        // TOTAL SUPPLY
        BigInteger expected = BigInteger.TEN.pow(30).subtract(BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));
        BigInteger result = tokenContract.totalSupply(params);
        Assert.assertEquals("Total supply should be same as expected", expected, result);

        // move phase to stop
        tx = new TransactionReceiptImpl("0x07", 300L, owner);
        this.adapter.setTransactionReceipt(tx);
        tokenContract.movePhaseRun(params);

        tx = new TransactionReceiptImpl("0x08", 300L, owner);
        this.adapter.setTransactionReceipt(tx);
        tokenContract.movePhaseStop(params);

        // STOPPED PHASE
        tx = new TransactionReceiptImpl("0x09", 300L, owner);
        this.adapter.setTransactionReceipt(tx);

        tokenContract.burn(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Burn at stop phase should be failed", tx.isSuccess());
    }

    @Test
    public void mintNotBurnable() {
        Boolean burnable = false;
        createToken(null, null, null, null, null, null, null, burnable, null, null, null);

        TransactionReceipt tx = new TransactionReceiptImpl("0x02", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setTransactionReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.burn(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Burn of non-mintable token should be failed", tx.isSuccess());
    }

    @Test
    public void exchangeT2TOpen() {
        TransactionReceipt tx = _testInit();

        // NONEXISTENT TOKEN
        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");
        params.addProperty("tokenExTargetTokenId", "OTHER_TOKEN");

        // NOT OWNER

        // NONEXISTENT TARGET TOKEN

        // create target token

        // NORMAL

        // ALREADY OPEN
    }




    private void createToken(
            String txId,
            String owner,
            String tokenId,
            String tokenName,
            BigInteger initStake,
            BigInteger initMint,
            Boolean mintable,
            Boolean burnable,
            Boolean exchangeable,
            String exType,
            Double exRateT2Y) {

        if (txId == null) txId = "0x01";
        if (owner == null) owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        if (tokenId == null) tokenId = "TEST_TOKEN";
        if (tokenName == null) tokenName = "TTOKEN";
        if (initStake == null) initStake = BigInteger.TEN.pow(24); // 1백만
        if (initMint == null) initMint = BigInteger.TEN.pow(30); // 1조 개 (1 * 10^12)
        if (mintable == null) mintable = true;
        if (burnable == null) burnable = true;
        if (exchangeable == null) exchangeable = true;
        if (exType == null) exType = "TOKEN_EX_TYPE_FIXED";
        if (exRateT2Y == null) exRateT2Y = 1.0;

        TransactionReceipt tx = new TransactionReceiptImpl(txId, 300L, owner);
        this.adapter.setTransactionReceipt(tx);

        JsonObject createToken = new JsonObject();
        createToken.addProperty("tokenId", tokenId);
        createToken.addProperty("tokenName", tokenName);
        createToken.addProperty("tokenInitYeedStakeAmount", initStake);
        createToken.addProperty("tokenInitMintAmount", initMint);
        createToken.addProperty("tokenMintable", mintable.booleanValue());
        createToken.addProperty("tokenBurnable", burnable.booleanValue());
        createToken.addProperty("tokenExchangeable", exchangeable.booleanValue());
        createToken.addProperty("tokenExType", exType);
        createToken.addProperty("tokenExRateT2Y", exRateT2Y.doubleValue());

        tokenContract.createToken(createToken);
        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Token creation is failed", tx.isSuccess());
    }

    private TransactionReceipt _testInit() {
        createToken();

        String issuer = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        TransactionReceipt tx = new TransactionReceiptImpl("0x02", 300L, issuer);
        this.adapter.setTransactionReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.movePhaseRun(params);

        tx.getTxLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("movePhaseRun Success", tx.isSuccess());

        tx = new TransactionReceiptImpl("0x03", 300L, issuer);
        this.adapter.setTransactionReceipt(tx);

        return tx;
    }


    private void ref() {
        TokenContract contract = new TokenContract();
        TokenContract.TokenService service = new TokenContract.TokenService();

        System.out.println(service.approve(null));
        System.out.println(service.allowance(null));
        System.out.println(service.totalSupply(null));
        System.out.println(service.balanceOf(null));
        System.out.println(service.burn(null));
        System.out.println(service.exchangeT2TClose(null));
        System.out.println(service.createToken(null));
        System.out.println(service.depositYeedStake(null));
        System.out.println(service.destroyToken(null));
        System.out.println(service.exchangeT2T(null));
        System.out.println(service.exchangeT2Y(null));
        System.out.println(service.exchangeY2T(null));
        System.out.println(service.getYeedBalanceOf(null));
        System.out.println(service.init(null));
        System.out.println(service.mint(null));
        System.out.println(service.movePhasePause(null));
        System.out.println(service.movePhaseRun(null));
        System.out.println(service.movePhaseStop(null));
        System.out.println(service.exchangeT2TOpen(null));
        System.out.println(service.transfer(null));
        System.out.println(service.transferFrom(null));
        System.out.println(service.withdrawYeedStake(null));
    }

    class TokenBranchStateStore implements BranchStateStore {

        ValidatorSet set = new ValidatorSet();
        List<BranchContract> contracts;

        public TokenBranchStateStore() {

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
    }


}