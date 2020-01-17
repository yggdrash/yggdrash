package io.yggdrash.contract.token;

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
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.ReceiptAdapter;
import io.yggdrash.contract.core.ReceiptImpl;
import io.yggdrash.core.blockchain.osgi.ContractCache;
import io.yggdrash.core.blockchain.osgi.ContractCacheImpl;
import io.yggdrash.core.blockchain.osgi.ContractChannelCoupler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenContractTest {
    private static final String TOKEN_ID = "tokenId";
    private static final String TOKEN_NAME = "tokenName";
    private static final String TOKEN_INIT_YEED_STAKE_AMOUNT = "tokenInitYeedStakeAmount";

    private static final String TOKEN_INIT_MINT_AMOUNT = "tokenInitMintAmount";
    private static final String TOKEN_MINTABLE = "tokenMintable";
    private static final String TOKEN_BURNABLE = "tokenBurnable";

    private static final String TOKEN_EX_T2Y_ENABLED = "tokenExT2YEnabled";
    private static final String TOKEN_EX_T2Y_TYPE = "tokenExT2YType";
    private static final String TOKEN_EX_T2Y_TYPE_FIXED = "TOKEN_EX_T2Y_TYPE_FIXED";
    private static final String TOKEN_EX_T2Y_TYPE_LINKED = "TOKEN_EX_T2Y_TYPE_LINKED";
    private static final String TOKEN_EX_T2Y_RATE = "tokenExT2YRate";

    private static final String TOKEN_EX_T2T_RATE = "tokenExT2TRate";
    private static final String TOKEN_EX_T2T_TARGET_TOKEN_ID = "tokenExT2TTargetTokenId";

    private static final String ADDRESS = "address";
    private static final String AMOUNT = "amount";

    private static final String SPENDER = "spender";
    private static final String OWNER = "owner";

    private static final String TEST_TOKEN_ID = "ThisIsTestTokenThisIsTestTokenThisIsTest".toLowerCase();
    private static final String TEST_OWNER = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
    private static final String TEST_ACCOUNT0 = "0000000000000000000000000000000000000000";
    private static final String TEST_ACCOUNT1 = "1111111111111111111111111111111111111111";
    private static final String TEST_ACCOUNT2 = "2222222222222222222222222222222222222222";
    private static final String TEST_TARGET_TOKEN_ID = "ThisIsTargetTokenThisIsTargetTokenThisIs".toLowerCase();
     

    private static final Logger log = LoggerFactory.getLogger(TokenContractTest.class);

    private ReceiptAdapter adapter;

    TestYeed testYeed;
    StateStore stateStore;

    ContractCache cache;
    Map<String, Object> contractMap = new HashMap<>();
    ContractChannelCoupler coupler;
    TokenContract.TokenService tokenContract = new TokenContract.TokenService();

    @Before
    public void setUp() throws Exception {

        stateStore = new StateStore(new HashMapDbSource());
        adapter = new ReceiptAdapter();
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
        Receipt result = new ReceiptImpl();

        // apply txReceipt
        adapter.setReceipt(result);

        // ADD contract coupler
        coupler = new ContractChannelCoupler();
        cache = new ContractCacheImpl();
        coupler.setContract(contractMap, cache);

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
        // INSUFFICIENT YEED BALANCE TO STAKE
        Receipt tx = new ReceiptImpl("0x00", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject createToken = new JsonObject();
        createToken.addProperty(TOKEN_ID, TEST_TOKEN_ID);
        createToken.addProperty(TOKEN_NAME, "TTOKEN");
        createToken.addProperty(TOKEN_INIT_YEED_STAKE_AMOUNT, BigInteger.TEN.pow(50));
        createToken.addProperty(TOKEN_INIT_MINT_AMOUNT, BigInteger.TEN.pow(30));
        createToken.addProperty(TOKEN_MINTABLE, true);
        createToken.addProperty(TOKEN_BURNABLE, true);
        createToken.addProperty(TOKEN_EX_T2Y_ENABLED, true);
        createToken.addProperty(TOKEN_EX_T2Y_TYPE, TOKEN_EX_T2Y_TYPE_FIXED);
        createToken.addProperty(TOKEN_EX_T2Y_RATE, new BigDecimal("1.0"));

        tokenContract.createToken(createToken);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Token creation with YEED stake over balance should be failed", tx.isSuccess());

        // NORMAL
        tx = _createToken("0x01", null, null, null, BigInteger.TEN.pow(24), null, null, null, null, null, null);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Token creation is failed", tx.isSuccess());
    }

    @Test
    public void createTokenDuplicated() {
        // NORMAL
        Receipt tx = _createToken(null, null, null, null, null, null, null, null, null, null, null);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Token creation is failed", tx.isSuccess());

        // DUPLICATED
        tx = new ReceiptImpl("0x02", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject createToken = new JsonObject();
        createToken.addProperty(TOKEN_ID, TEST_TOKEN_ID);
        createToken.addProperty(TOKEN_NAME, "TTOKEN");
        createToken.addProperty(TOKEN_INIT_YEED_STAKE_AMOUNT, BigInteger.TEN.pow(24));
        createToken.addProperty(TOKEN_INIT_MINT_AMOUNT, BigInteger.TEN.pow(30));
        createToken.addProperty(TOKEN_MINTABLE, true);
        createToken.addProperty(TOKEN_BURNABLE, true);
        createToken.addProperty(TOKEN_EX_T2Y_ENABLED, true);
        createToken.addProperty(TOKEN_EX_T2Y_TYPE, TOKEN_EX_T2Y_TYPE_FIXED);
        createToken.addProperty(TOKEN_EX_T2Y_RATE, new BigDecimal("1.0"));

        tokenContract.createToken(createToken);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Duplicated token creation should be failed", tx.isSuccess());
    }

    @Test
    public void totalSupply() {
        Receipt tx = _testInit();

        // NONEXISTENT TOKEN
        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");

        BigInteger totalSupply = tokenContract.totalSupply(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals("The result must be null", null, totalSupply);

        // NORMAL
        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        totalSupply = tokenContract.totalSupply(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals(
                "Total supply should match with initial mint",
                0, totalSupply.compareTo(BigInteger.TEN.pow(30)));
    }

    @Test
    public void balanceOf() {
        Receipt tx = _testInit();

        // NONEXISTENT TOKEN
        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");
        params.addProperty(ADDRESS, tx.getIssuer());

        BigInteger result = tokenContract.balanceOf(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals("The result must be null", null, result);

        // NORMAL
        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        result = tokenContract.totalSupply(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals(
                "The balance should match with initial mint!!!",
                0, result.compareTo(BigInteger.TEN.pow(30)));
    }

    @Test
    public void getYeedBalanceOf() {
        Receipt tx = _testInit();

        // NONEXISTENT TOKEN
        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");

        BigInteger result = tokenContract.getYeedBalanceOf(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals("The result must be ZERO", 0, BigInteger.ZERO.compareTo(result));

        // NORMAL
        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        result = tokenContract.getYeedBalanceOf(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals(
                "The result should match with initial YEED stake",
                0, result.compareTo(new BigInteger("999999900000000000000000")));
    }

    @Test
    public void getTokenInfo() {
        Receipt tx = _testInit();

        // NORMAL
        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        JsonObject token = tokenContract.getTokenInfo(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals(
                "The result should match",
                TEST_TOKEN_ID, token.get(TOKEN_ID).getAsString());

        Assert.assertEquals(
                "The result should match",
                "RUN", token.get("tokenPhase").getAsString());
    }

    @Test
    public void allowance() {
        Receipt tx = _testInit();

        // NONEXISTENT TOKEN
        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");
        params.addProperty(OWNER, TEST_OWNER);
        params.addProperty(SPENDER, TEST_ACCOUNT0);

        BigInteger result = tokenContract.allowance(params);

        Assert.assertEquals("Allowance at nonexistent token should returns null", null, result);

        // NOT APPROVED ACCOUNT
        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        result = tokenContract.allowance(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals(
                "Allowance of not approved account should be ZERO",
                0, result.compareTo(BigInteger.ZERO));

        // approve
        tx = new ReceiptImpl("0x03", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(100000));

        tokenContract.approve(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("The approve is failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x04", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        result = tokenContract.allowance(params);

        Assert.assertEquals(
                "The result should match with approved value",
                0, result.compareTo(getBigInt18(100000)));

        // transferFrom
        tx = new ReceiptImpl("0x05", 300L, TEST_ACCOUNT0);
        this.adapter.setReceipt(tx);

        params.addProperty("from", TEST_OWNER);
        params.addProperty("to", TEST_ACCOUNT2);
        params.addProperty(AMOUNT, getBigInt18(10000));

        tokenContract.transferFrom(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Transfer from owner to account2 by spender is failed", tx.isSuccess());

        // NORMAL after transferFrom
        tx = new ReceiptImpl("0x06", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        result = tokenContract.allowance(params);

        Assert.assertEquals(
                "The result should match with approved value",
                0, result.compareTo(getBigInt18(90000)));
    }

    @Test
    public void depositYeedStake() {
        Receipt tx = _testInit();

        // NONEXISTENT TOKEN
        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");
        params.addProperty(AMOUNT, getBigInt18(100));

        tokenContract.depositYeedStake(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("The deposit to nonexistent token should be failed", tx.isSuccess());

        // NOT OWNER
        tx = new ReceiptImpl("0x03", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);
        params.addProperty(AMOUNT, getBigInt18(100));

        tokenContract.depositYeedStake(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Deposit by who does not own token should be failed", tx.isSuccess());

        // OVER BALANCE
        tx = new ReceiptImpl("0x04", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, BigInteger.TEN.pow(50)); // balance == TEN.pow(32) YEED

        tokenContract.depositYeedStake(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Deposit over balance should be failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x05", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(100));

        tokenContract.depositYeedStake(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Additional deposit of YEED stake is failed", tx.isSuccess());

        // YEED BALANCE
        BigInteger result = tokenContract.getYeedBalanceOf(params);
        Assert.assertEquals(
                "The result should match with current YEED stake",
                0, result.compareTo(new BigInteger("1000099900000000000000000")));
    }

    @Test
    public void withdrawYeedStake() {
        Receipt tx = _testInit();

        // NONEXISTENT TOKEN
        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");

        tokenContract.withdrawYeedStake(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Withdraw from nonexistent token should be failed", tx.isSuccess());

        // NOT OWNER
        tx = new ReceiptImpl("0x04", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Withdraw by who does not own token should be failed", tx.isSuccess());

        // OVER BALANCE
        tx = new ReceiptImpl("0x05", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, BigInteger.TEN.pow(50)); // balance == TEN.pow(40)

        tokenContract.withdrawYeedStake(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Withdraw over YEED stake balance should be failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x06", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(100));

        tokenContract.withdrawYeedStake(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Withdraw of YEED stake is failed", tx.isSuccess());

        // CHECK BALANCE
        BigInteger result = tokenContract.getYeedBalanceOf(params);
        Assert.assertEquals(
                "The result should match with current YEED stake",
                0, result.compareTo(new BigInteger("999899800000000000000000")));
    }

    @Test
    public void movePhaseRun() {
        createToken();

        // NONE_TOKEN
        Receipt tx = new ReceiptImpl("0x02", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move of nonexistent token should be failed", tx.isSuccess());

        // NOT_OWNER
        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tx = new ReceiptImpl("0x03", 300L, "1111111111111111111111111111111111111111");
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move by who does not own token should be failed", tx.isSuccess());

        // INIT -> RUN NORMAL
        tx = new ReceiptImpl("0x04", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from INIT to RUN is failed", tx.isSuccess());

        // RUN -> PAUSE -> RUN NORMAL
        tx = new ReceiptImpl("0x05", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhasePause(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from RUN to PAUSE is failed", tx.isSuccess());

        tx = new ReceiptImpl("0x06", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from PAUSE to RUN is failed", tx.isSuccess());

        // RUN -> RUN FAIL
        tx = new ReceiptImpl("0x07", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move RUN to RUN should be failed", tx.isSuccess());
    }

    @Test
    public void movePhasePause() {
        createToken();

        // NONE_TOKEN
        Receipt tx = new ReceiptImpl("0x02", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");

        tokenContract.movePhasePause(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move of nonexistent token should be failed", tx.isSuccess());

        // NOT_OWNER
        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tx = new ReceiptImpl("0x03", 300L, "1111111111111111111111111111111111111111");
        this.adapter.setReceipt(tx);

        tokenContract.movePhasePause(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move by who does not own token should be failed", tx.isSuccess());

        // RUN -> PAUSE NORMAL
        tx = new ReceiptImpl("0x04", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from INIT to RUN is failed", tx.isSuccess());

        tx = new ReceiptImpl("0x05", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhasePause(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from RUN to PAUSE is failed", tx.isSuccess());

        // PAUSE -> PAUSE FAIL
        tx = new ReceiptImpl("0x07", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhasePause(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move PAUSE to PAUSE should be failed", tx.isSuccess());
    }

    @Test
    public void movePhaseStop() {
        createToken();

        // NONE_TOKEN
        Receipt tx = new ReceiptImpl("0x02", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");

        tokenContract.movePhaseStop(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move of nonexistent token should be failed", tx.isSuccess());

        // NOT_OWNER
        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tx = new ReceiptImpl("0x03", 300L, "1111111111111111111111111111111111111111");
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseStop(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move by who does not own token should be failed", tx.isSuccess());

        // RUN -> PAUSE -> STOP NORMAL
        tx = new ReceiptImpl("0x04", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from INIT to RUN is failed", tx.isSuccess());

        tx = new ReceiptImpl("0x05", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhasePause(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from RUN to PAUSE is failed", tx.isSuccess());

        tx = new ReceiptImpl("0x06", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseStop(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from PAUSE to STOP is failed", tx.isSuccess());

        // STOP -> STOP FAIL
        tx = new ReceiptImpl("0x07", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseStop(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move STOP to STOP should be failed", tx.isSuccess());

        // STOP -> RUN FAIL
        tx = new ReceiptImpl("0x08", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move STOP to RUN should be failed", tx.isSuccess());
    }

    @Test
    public void destroyToken() {
        createToken();

        // NONE_TOKEN
        Receipt tx = new ReceiptImpl("0x02", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");

        tokenContract.destroyToken(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Destroy of nonexistent token should be failed", tx.isSuccess());

        // NOT_OWNER
        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tx = new ReceiptImpl("0x03", 300L, "1111111111111111111111111111111111111111");
        this.adapter.setReceipt(tx);

        tokenContract.destroyToken(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move by who does not own token should be failed", tx.isSuccess());

        // RUN -> PAUSE -> STOP NORMAL -> Destroy
        tx = new ReceiptImpl("0x04", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from INIT to RUN is failed", tx.isSuccess());

        tx = new ReceiptImpl("0x05", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhasePause(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from RUN to PAUSE is failed", tx.isSuccess());

        tx = new ReceiptImpl("0x06", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseStop(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from PAUSE to STOP is failed", tx.isSuccess());

        tx = new ReceiptImpl("0x16", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.destroyToken(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Destroy is failed", tx.isSuccess());

        // Access to destroyed token FAIL
        tx = new ReceiptImpl("0x07", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseStop(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move to STOP of destroyed token should be failed", tx.isSuccess());

        // Access to destroyed token FAIL
        tx = new ReceiptImpl("0x08", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject tokenInfo = tokenContract.getTokenInfo(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Destroyed token info should be empty", tokenInfo.get(TOKEN_ID) == null);

        // Yeed balance of owner check (0.4 subtracted)
        params = new JsonObject();
        params.addProperty("address", TEST_OWNER);

        BigInteger yeedAmount = testYeed.balanceOf(params);
        Assert.assertEquals(
                "Yeed balance of token owner's account address mismatch!!!",
                0, yeedAmount.compareTo(new BigInteger("9999999999999999999999600000000000000000")));
    }

    @Test
    public void transfer() {
        createToken();

        // NONE_TOKEN
        Receipt tx = new ReceiptImpl("0x02", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");
        params.addProperty("to", TEST_OWNER);

        tokenContract.transfer(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of nonexistent token should be failed", tx.isSuccess());

        // NOT_RUNNING
        tx = new ReceiptImpl("0x03", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tokenContract.transfer(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of not running token should be failed", tx.isSuccess());

        //     move phase to run
        tx = new ReceiptImpl("0x04", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseRun(params);

        // 'to' == issuer
        tx = new ReceiptImpl("0x05", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.transfer(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer 'to' account should different from the issuer!", tx.isSuccess());

        // NEGATIVE AMOUNT
        tx = new ReceiptImpl("0x06", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty("to", TEST_ACCOUNT1);
        params.addProperty(AMOUNT, getBigInt18(-1));

        tokenContract.transfer(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of negative amount should be failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x07", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(100));

        tokenContract.transfer(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Transfer is failed", tx.isSuccess());

        // INSUFFICIENT BALANCE
        tx = new ReceiptImpl("0x08", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty("to", TEST_OWNER);
        params.addProperty(AMOUNT, getBigInt18(101));

        tokenContract.transfer(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer from insufficient balance should be failed", tx.isSuccess());
    }

    @Test
    public void transfer_serviceFee() {
        _createToken(
                null, TEST_OWNER, null, null, BigInteger.TEN.pow(10),
                null, null, null, null, null, null);

        // INSUFFICIENT BALANCE
        Receipt tx = new ReceiptImpl("0x08", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "ThisIsTestTokenThisIsTestTokenThisIsTest");
        params.addProperty("to", TEST_ACCOUNT1);
        params.addProperty(AMOUNT, getBigInt18(101));

        tokenContract.transfer(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer from insufficient balance should be failed", tx.isSuccess());
    }

    @Test
    public void approve() {
        createToken();

        // NONEXISTENT TOKEN
        Receipt tx = new ReceiptImpl("0x02", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");

        tokenContract.approve(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Approve for nonexistent token should be failed", tx.isSuccess());

        // NOT RUNNING
        tx = new ReceiptImpl("0x04", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tokenContract.approve(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Approve for not running token should be failed", tx.isSuccess());

        // move phase to run
        tx = new ReceiptImpl("0x05", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseRun(params);

        // NORMAL
        tx = new ReceiptImpl("0x07", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(SPENDER, TEST_ACCOUNT0);
        params.addProperty(AMOUNT, getBigInt18(100000));

        tokenContract.approve(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("The approve is failed", tx.isSuccess());

        // ALLOWANCE
        tx = new ReceiptImpl("0x08", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(OWNER, TEST_OWNER);
        params.addProperty(SPENDER, TEST_ACCOUNT0);

        BigInteger result = tokenContract.allowance(params);

        Assert.assertEquals(
                "The result should match with approved value",
                0, result.compareTo(getBigInt18(100000)));
    }

    @Test
    public void transferFrom() {
        createToken();

        // NONEXISTENT
        Receipt tx = new ReceiptImpl("0x02", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");

        tokenContract.transferFrom(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of nonexistent token should be failed", tx.isSuccess());

        // NOT RUNNING
        tx = new ReceiptImpl("0x03", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tokenContract.transferFrom(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of not running token should be failed", tx.isSuccess());

        // move phase to run
        tx = new ReceiptImpl("0x04", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseRun(params);

        // NEGATIVE AMOUNT
        tx = new ReceiptImpl("0x05", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty("from", TEST_OWNER);
        params.addProperty("to", TEST_ACCOUNT2);
        params.addProperty(AMOUNT, getBigInt18(-1));

        tokenContract.transferFrom(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of negative amount should be failed", tx.isSuccess());

        // approve 100
        tx = new ReceiptImpl("0x06", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject paramsApprove = new JsonObject();
        paramsApprove.addProperty(TOKEN_ID, TEST_TOKEN_ID);
        paramsApprove.addProperty(AMOUNT, getBigInt18(100));
        paramsApprove.addProperty(SPENDER, TEST_ACCOUNT1);

        tokenContract.approve(paramsApprove);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("The approve is failed", tx.isSuccess());

        // INSUFFICIENT BALANCE 200
        tx = new ReceiptImpl("0x07", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(200));

        tokenContract.transferFrom(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer from insufficient approved balance should be failed", tx.isSuccess());

        // NORMAL 50
        tx = new ReceiptImpl("0x08", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(50));

        tokenContract.transferFrom(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Transfer from approved is failed", tx.isSuccess());

        // ALLOWANCE 50
        params.addProperty(OWNER, TEST_OWNER);
        params.addProperty(SPENDER, TEST_ACCOUNT1);
        BigInteger result = tokenContract.allowance(params);
        Assert.assertEquals("Allowance should be 50", 0, result.compareTo(getBigInt18(50)));

        // INSUFFICIENT OWNER BALANCE TO TRANSFER FROM
        JsonObject paramsTransfer = new JsonObject();
        paramsTransfer.addProperty(TOKEN_ID, TEST_TOKEN_ID);
        paramsTransfer.addProperty(ADDRESS, TEST_OWNER);
        BigInteger ownerBalance = tokenContract.balanceOf(paramsTransfer);

        tx = new ReceiptImpl("0x09", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);
        paramsTransfer.addProperty("to", TEST_ACCOUNT1);
        paramsTransfer.addProperty(AMOUNT, ownerBalance);
        tokenContract.transfer(paramsTransfer);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Transfer is failed", tx.isSuccess());

        tx = new ReceiptImpl("0x10", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);
        tokenContract.transferFrom(params);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer from approved should be failed", tx.isSuccess());
    }

    @Test
    public void mint() {
        createToken();

        // NONEXISTENT TOKEN
        Receipt tx = new ReceiptImpl("0x02", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");

        tokenContract.mint(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Mint of nonexistent token should be failed", tx.isSuccess());

        // NOT OWNER
        tx = new ReceiptImpl("0x03", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tokenContract.mint(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Mint by non-owner account should be failed", tx.isSuccess());

        // NEGATIVE AMOUNT
        tx = new ReceiptImpl("0x05", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(-1));

        tokenContract.mint(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Mint of negative amount should be failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x06", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(100));

        tokenContract.mint(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Mint is failed", tx.isSuccess());

        // TOTAL SUPPLY
        BigInteger expected = BigInteger.TEN.pow(30).add(getBigInt18(100));
        BigInteger result = tokenContract.totalSupply(params);
        Assert.assertEquals("Total supply should be same as expected", expected, result);

        // move phase to stop
        tx = new ReceiptImpl("0x07", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseRun(params);

        tx = new ReceiptImpl("0x08", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseStop(params);

        // STOPPED PHASE
        tx = new ReceiptImpl("0x09", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.mint(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Mint at stop phase should be failed", tx.isSuccess());
    }

    @Test
    public void mintNotMintable() {
        Receipt tx = _createToken(
                null, null, null, null,
                null, null, false, null,
                null, null, null);
        
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Token creation is failed", tx.isSuccess());

        tx = new ReceiptImpl("0x02", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tokenContract.mint(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Mint of non-mintable token should be failed", tx.isSuccess());
    }

    @Test
    public void burn() {
        createToken();

        // NONEXISTENT TOKEN
        Receipt tx = new ReceiptImpl("0x02", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");

        tokenContract.burn(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Burn of nonexistent token should be failed", tx.isSuccess());

        // NOT OWNER
        tx = new ReceiptImpl("0x03", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tokenContract.burn(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Burn by non-owner account should be failed", tx.isSuccess());

        // NEGATIVE AMOUNT
        tx = new ReceiptImpl("0x05", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(-1));

        tokenContract.burn(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Burn of negative amount should be failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x06", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(100));

        tokenContract.burn(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Burn is failed", tx.isSuccess());

        // TOTAL SUPPLY
        BigInteger expected = BigInteger.TEN.pow(30).subtract(getBigInt18(100));
        BigInteger result = tokenContract.totalSupply(params);
        Assert.assertEquals("Total supply should be same as expected", expected, result);

        // move phase to stop
        tx = new ReceiptImpl("0x07", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseRun(params);

        tx = new ReceiptImpl("0x08", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseStop(params);

        // STOPPED PHASE
        tx = new ReceiptImpl("0x09", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.burn(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Burn at stop phase should be failed", tx.isSuccess());
    }

    @Test
    public void burnNotBurnable() {
        Receipt tx = _createToken(
                null, null, null, null,
                null, null, null, false,
                null, null, null);
        
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Token creation is failed", tx.isSuccess());

        tx = new ReceiptImpl("0x02", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tokenContract.burn(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Burn of non-mintable token should be failed", tx.isSuccess());
    }

    @Test
    public void exchangeT2YFixed() {
        // CREATE TOKEN : NEGATIVE EX RATE
        Receipt tx = _createToken(
                "0x99", null, null, null, BigInteger.TEN.pow(24), null, 
                null, null, null, null, new BigDecimal("-1.0"));
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Token creation at negative exchange rate should be failed", tx.isSuccess());

        // CREATE TOKEN NORMAL
        createToken();

        // NONEXISTENT TOKEN
        tx = new ReceiptImpl("0x02", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");

        tokenContract.exchangeT2Y(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange of nonexistent token should be failed", tx.isSuccess());

        // NOT_RUNNING
        tx = new ReceiptImpl("0x03", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tokenContract.exchangeT2Y(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange of not running token should be failed", tx.isSuccess());

        // move phase to run
        tx = new ReceiptImpl("0x04", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseRun(params);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Move phase to run is failed", tx.isSuccess());

        // issue token to account1
        tx = new ReceiptImpl("0x05", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);
        params.addProperty("to", TEST_ACCOUNT1);
        params.addProperty(AMOUNT, getBigInt18(2000000));
        tokenContract.transfer(params);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Transfer is failed", tx.isSuccess());

        // NEGATIVE AMOUNT
        tx = new ReceiptImpl("0x06", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(-1));

        tokenContract.exchangeT2Y(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange of negative amount should be failed", tx.isSuccess());

        // INSUFFICIENT BALANCE
        tx = new ReceiptImpl("0x07", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(2000001));

        tokenContract.exchangeT2Y(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange over balance should be failed", tx.isSuccess());

        // OVER YEED STAKE
        tx = new ReceiptImpl("0x08", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(1000001));

        tokenContract.exchangeT2Y(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange over yeed stake should be failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x09", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(400000));

        tokenContract.exchangeT2Y(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Exchange is failed", tx.isSuccess());

        // CHECK TOKEN BALANCE
        params.addProperty(ADDRESS, TEST_ACCOUNT1);
        BigInteger tokenBalance = tokenContract.balanceOf(params);
        Assert.assertEquals(
                "Token balance should be 1600000",
                0, tokenBalance.compareTo(getBigInt18(1600000)));

        // CHECK YEED STAKE BALANCE
        BigInteger yeedBalance = tokenContract.getYeedBalanceOf(params);
        Assert.assertEquals(
                "Yeed stake balance should be 600000",
                0, yeedBalance.compareTo(new BigInteger("599999700000000000000000")));
    }

    @Test
    public void exchangeT2YLinked() {
        // stake = 1,000,000
        // mint = 10,000,000
        // linked exRate will be 0.1
        Receipt tx = _createToken(
                "0x01", null, null, null,
                getBigInt18(1000000), getBigInt18(10000000), null, null,
                null, TOKEN_EX_T2Y_TYPE_LINKED, null);
        
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Token creation is failed", tx.isSuccess());

        // move phase to run
        tx = new ReceiptImpl("0x04", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tokenContract.movePhaseRun(params);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Move phase to run is failed", tx.isSuccess());

        // transfer token to account1
        tx = new ReceiptImpl("0x05", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);
        params.addProperty("to", TEST_ACCOUNT1);
        params.addProperty(AMOUNT, getBigInt18(1000000));
        tokenContract.transfer(params);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Transfer is failed", tx.isSuccess());

        // NORMAL (400,000 tokens to 40,000 YEEDs)
        tx = new ReceiptImpl("0x09", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(400000));

        tokenContract.exchangeT2Y(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Exchange is failed", tx.isSuccess());

        // CHECK TOKEN BALANCE
        params.addProperty(ADDRESS, TEST_ACCOUNT1);
        BigInteger tokenBalance = tokenContract.balanceOf(params);
        Assert.assertEquals(
                "Token balance should be 600000",
                0, tokenBalance.compareTo(getBigInt18(600000)));

        // CHECK YEED STAKE BALANCE (not strict number for complex of fees & exchange)
        BigInteger yeedBalance = tokenContract.getYeedBalanceOf(params);
        Assert.assertEquals(
                "Yeed stake balance should be 959999.7.....",
                0, yeedBalance.compareTo(new BigInteger("959999707999999999999680")));
    }

    @Test
    public void exchangeY2T() {
        createToken();

        // NONEXISTENT TOKEN
        Receipt tx = new ReceiptImpl("0x02", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");

        tokenContract.exchangeY2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange YEED to token of nonexistent token should be failed", tx.isSuccess());

        // NOT_RUNNING
        tx = new ReceiptImpl("0x03", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tokenContract.exchangeY2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange YEED to token of not running token should be failed", tx.isSuccess());

        // move phase to run
        tx = new ReceiptImpl("0x04", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseRun(params);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Move phase to run is failed", tx.isSuccess());

        // NEGATIVE AMOUNT
        tx = new ReceiptImpl("0x06", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(-1));

        tokenContract.exchangeY2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange YEED to token of negative amount should be failed", tx.isSuccess());

        // INSUFFICIENT BALANCE (current YEED amount = 1234)
        tx = new ReceiptImpl("0x07", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(10000));

        tokenContract.exchangeY2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange YEED to token over YEED balance should be failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x09", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(1000));

        tokenContract.exchangeY2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Exchange YEED to token is failed", tx.isSuccess());

        // CHECK TOKEN BALANCE (1000)
        params.addProperty(ADDRESS, TEST_ACCOUNT1);
        BigInteger tokenBalance = tokenContract.balanceOf(params);
        Assert.assertEquals(
                "Token balance should be 1000",
                0, tokenBalance.compareTo(getBigInt18(1000)));

        // CHECK YEED STAKE BALANCE (1000000 + 1000)
        BigInteger yeedBalance = tokenContract.getYeedBalanceOf(params);
        Assert.assertEquals(
                "Yeed stake balance should be 1000999.9",
                0, yeedBalance.compareTo(new BigInteger("1000999900000000000000000")));
    }

    @Test
    public void exchangeY2TLinked() {
        // stake = 1,000,000
        // mint = 10,000,000
        // linked exRate will be 0.1
        Receipt tx = _createToken(
                "0x01", null, null, null,
                getBigInt18(1000000), getBigInt18(10000000), null, null,
                null, TOKEN_EX_T2Y_TYPE_LINKED, null);
        
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Token creation is failed", tx.isSuccess());

        // move phase to run
        tx = new ReceiptImpl("0x04", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tokenContract.movePhaseRun(params);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Move phase to run is failed", tx.isSuccess());

        // NORMAL (1000 YEEDs to 10,000 tokens)
        tx = new ReceiptImpl("0x09", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(1000));

        tokenContract.exchangeY2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Exchange is failed", tx.isSuccess());

        // CHECK TOKEN BALANCE
        params.addProperty(ADDRESS, TEST_ACCOUNT1);
        BigInteger tokenBalance = tokenContract.balanceOf(params);
        Assert.assertEquals(
                "Token balance should be 10,000",
                0, tokenBalance.compareTo(new BigInteger("10000001000000100000000")));

        // CHECK YEED STAKE BALANCE (1,000,000 + 1,000 = 1,001,000)
        BigInteger yeedBalance = tokenContract.getYeedBalanceOf(params);
        Assert.assertEquals(
                "Yeed stake balance should be 1,001,000",
                0, yeedBalance.compareTo(new BigInteger("1000999900000000000000000")));
    }

    @Test
    public void exchangeT2TOpen() {
        Receipt tx = _testInit();

        // NONEXISTENT TOKEN
        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");
        params.addProperty(TOKEN_EX_T2T_TARGET_TOKEN_ID, TEST_TARGET_TOKEN_ID);

        tokenContract.exchangeT2TOpen(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange open from nonexistent token should be failed", tx.isSuccess());

        // NOT OWNER
        tx = new ReceiptImpl("0x04", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tokenContract.exchangeT2TOpen(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange open by non-owner account should be failed", tx.isSuccess());

        // NONEXISTENT TARGET TOKEN
        tx = new ReceiptImpl("0x05", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.exchangeT2TOpen(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange open to nonexistent target token should be failed", tx.isSuccess());

        // create target token
        tx = _createToken("0x06", "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e",
                TEST_TARGET_TOKEN_ID, "targetToken",
                null, null, null, null,
                null, null, null);
        
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Token creation is failed", tx.isSuccess());

        // RATE smaller than ZERO
        tx = new ReceiptImpl("0x17", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(TOKEN_EX_T2T_RATE, new BigDecimal("-1.0"));

        tokenContract.exchangeT2TOpen(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange rate should be greater than ZERO", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x07", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(TOKEN_EX_T2T_RATE, new BigDecimal("1.0"));

        tokenContract.exchangeT2TOpen(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Exchange open to target token is failed", tx.isSuccess());

        // ALREADY OPEN
        tx = new ReceiptImpl("0x08", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.exchangeT2TOpen(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange open to already open target token should be failed", tx.isSuccess());
    }

    @Test
    public void exchangeT2TClose() {
        Receipt tx = _testInit();

        // NONEXISTENT TOKEN
        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, "NONE_TOKEN");
        params.addProperty(TOKEN_EX_T2T_TARGET_TOKEN_ID, TEST_TARGET_TOKEN_ID);

        tokenContract.exchangeT2TClose(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange close from nonexistent token should be failed", tx.isSuccess());

        // NOT OWNER
        tx = new ReceiptImpl("0x04", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tokenContract.exchangeT2TClose(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange close by non-owner account should be failed", tx.isSuccess());

        // NONEXISTENT TARGET TOKEN
        tx = new ReceiptImpl("0x05", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.exchangeT2TClose(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange close to nonexistent target token should be failed", tx.isSuccess());

        // create target token
        tx = _createToken("0x06", "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e",
                TEST_TARGET_TOKEN_ID, "targetToken",
                null, null, null, null,
                null, null, null);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Token creation is failed", tx.isSuccess());

        // ALREADY CLOSED
        tx = new ReceiptImpl("0x07", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.exchangeT2TClose(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange close to already closed target token should be failed", tx.isSuccess());

        // open
        tx = new ReceiptImpl("0x08", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(TOKEN_EX_T2T_RATE, new BigDecimal("1.0"));

        tokenContract.exchangeT2TOpen(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Exchange open to target token is failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x09", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.exchangeT2TClose(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Exchange close to target token is failed", tx.isSuccess());
    }

    @Test
    public void exchangeT2T() {
        String targetOwner = "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e";

        // NONEXISTENT TOKEN
        Receipt tx = new ReceiptImpl("0x02", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);
        params.addProperty(TOKEN_EX_T2T_TARGET_TOKEN_ID, TEST_TARGET_TOKEN_ID);

        tokenContract.exchangeT2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange from nonexistent token should be failed", tx.isSuccess());

        //     create token
        createToken();

        // NOT RUNNING
        tx = new ReceiptImpl("0x04", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        tokenContract.exchangeT2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange of not running token should be failed", tx.isSuccess());

        //     move phase to run
        tx = new ReceiptImpl("0x05", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from INIT to RUN is failed", tx.isSuccess());

        //     transfer 10000 to account1
        tx = new ReceiptImpl("0x03", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty("to", TEST_ACCOUNT1);
        params.addProperty(AMOUNT, getBigInt18(10000));

        tokenContract.transfer(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Transfer is failed", tx.isSuccess());

        //     create target token
        tx = _createToken("0x06", targetOwner,
                TEST_TARGET_TOKEN_ID, "targetToken",
                null, null, null, null,
                null, null, null);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Token creation is failed", tx.isSuccess());

        // NOT OPEN TO TARGET TOKEN
        tx = new ReceiptImpl("0x07", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        tokenContract.exchangeT2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange to not open target token should be failed", tx.isSuccess());

        //     open to target token
        tx = new ReceiptImpl("0x08", 300L, TEST_OWNER);
        this.adapter.setReceipt(tx);

        params.addProperty(TOKEN_EX_T2T_RATE, new BigDecimal("1.0"));

        tokenContract.exchangeT2TOpen(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Exchange open to target token is failed", tx.isSuccess());

        // TARGET TOKEN NOT RUNNING
        tx = new ReceiptImpl("0x09", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        tokenContract.exchangeT2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange to not running target token should be failed", tx.isSuccess());

        //     target token move phase to run
        tx = new ReceiptImpl("0x10", 300L, targetOwner);
        this.adapter.setReceipt(tx);

        JsonObject targetParams = new JsonObject();
        targetParams.addProperty(TOKEN_ID, TEST_TARGET_TOKEN_ID);

        tokenContract.movePhaseRun(targetParams);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase of target token move from INIT to RUN is failed", tx.isSuccess());

        // ZERO AMOUNT
        tx = new ReceiptImpl("0x11", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, BigInteger.ZERO);

        tokenContract.exchangeT2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange amount should be greater than ZERO", tx.isSuccess());

        // INSUFFICIENT BALANCE
        tx = new ReceiptImpl("0x11", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(20000));

        tokenContract.exchangeT2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange amount should be less than balance", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x12", 300L, TEST_ACCOUNT1);
        this.adapter.setReceipt(tx);

        params.addProperty(AMOUNT, getBigInt18(1000));

        tokenContract.exchangeT2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Exchange failed", tx.isSuccess());

        // TOKEN BALANCE
        params = new JsonObject();
        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);
        params.addProperty(ADDRESS, TEST_ACCOUNT1);

        BigInteger tokenBalance = tokenContract.balanceOf(params);
        Assert.assertEquals(
                "Token balance should match",
                0, tokenBalance.compareTo(getBigInt18(9000)));

        // TARGET TOKEN BALANCE
        params.addProperty(TOKEN_ID, TEST_TARGET_TOKEN_ID);

        BigInteger targetTokenBalance = tokenContract.balanceOf(params);
        Assert.assertEquals(
                "Target token balance should match",
                0, targetTokenBalance.compareTo(getBigInt18(1000)));
    }

    private Receipt _createToken(
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
            BigDecimal exRateT2Y) {

        if (txId == null) {
            txId = "0x01";
        }
        if (owner == null) {
            owner = TEST_OWNER;
        }
        if (tokenId == null) {
            tokenId = TEST_TOKEN_ID;
        }

        if (tokenName == null) {
            tokenName = "TTOKEN";
        }
        if (initStake == null) {
            initStake = BigInteger.TEN.pow(24); // 1,000,000 YEEDs
        }
        if (initMint == null) {
            initMint = BigInteger.TEN.pow(30); // 1,000,000,000,000 tokens
        }
        if (mintable == null) {
            mintable = true;
        }
        if (burnable == null) {
            burnable = true;
        }
        if (exchangeable == null) {
            exchangeable = true;
        }
        if (exType == null) {
            exType = TOKEN_EX_T2Y_TYPE_FIXED;
        }
        if (exRateT2Y == null) {
            exRateT2Y = new BigDecimal("1.0");
        }

        Receipt tx = new ReceiptImpl(txId, 300L, owner);
        this.adapter.setReceipt(tx);

        JsonObject createToken = new JsonObject();
        createToken.addProperty(TOKEN_ID, tokenId);
        createToken.addProperty(TOKEN_NAME, tokenName);
        createToken.addProperty(TOKEN_INIT_YEED_STAKE_AMOUNT, initStake);
        createToken.addProperty(TOKEN_INIT_MINT_AMOUNT, initMint);
        createToken.addProperty(TOKEN_MINTABLE, mintable);
        createToken.addProperty(TOKEN_BURNABLE, burnable);

        createToken.addProperty(TOKEN_EX_T2Y_ENABLED, exchangeable);
        createToken.addProperty(TOKEN_EX_T2Y_TYPE, exType);
        createToken.addProperty(TOKEN_EX_T2Y_RATE, exRateT2Y);

        tokenContract.createToken(createToken);
        return tx;
    }

    private Receipt _testInit() {
        createToken();

        String owner = TEST_OWNER;
        Receipt tx = new ReceiptImpl("0x02", 300L, owner);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty(TOKEN_ID, TEST_TOKEN_ID);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("movePhaseRun Success", tx.isSuccess());

        tx = new ReceiptImpl("0x03", 300L, owner);
        this.adapter.setReceipt(tx);

        return tx;
    }

    private BigInteger getBigInt18(long val) {
        return BigInteger.valueOf(val).multiply(BigInteger.TEN.pow(18));
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