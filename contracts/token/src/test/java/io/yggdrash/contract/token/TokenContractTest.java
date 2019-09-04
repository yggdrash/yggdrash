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
        String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";

        // INSUFFICIENT YEED BALANCE TO STAKE
        Receipt tx = new ReceiptImpl("0x00", 300L, owner);
        this.adapter.setReceipt(tx);

        JsonObject createToken = new JsonObject();
        createToken.addProperty("tokenId", "TEST_TOKEN");
        createToken.addProperty("tokenName", "TTOKEN");
        createToken.addProperty("tokenInitYeedStakeAmount", BigInteger.TEN.pow(50));
        createToken.addProperty("tokenInitMintAmount", BigInteger.TEN.pow(30));
        createToken.addProperty("tokenMintable", true);
        createToken.addProperty("tokenBurnable", true);
        createToken.addProperty("tokenExT2YEnabled", true);
        createToken.addProperty("tokenExT2YType", "TOKEN_EX_T2Y_TYPE_FIXED");
        createToken.addProperty("tokenExT2YRate", new BigDecimal("1.0"));

        tokenContract.createToken(createToken);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Token creation with YEED stake over balance should be failed", tx.isSuccess());

        // NORMAL
        createToken("0x01", null, null, null, BigInteger.TEN.pow(24), null, null, null, null, null, null);
    }

    @Test
    public void createTokenDuplicated() {
        // NORMAL
        createToken(null, null, null, null, null, null, null, null, null, null, null);

        // DUPLICATED
        Receipt tx = new ReceiptImpl("0x02", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        JsonObject createToken = new JsonObject();
        createToken.addProperty("tokenId", "TEST_TOKEN");
        createToken.addProperty("tokenName", "TTOKEN");
        createToken.addProperty("tokenInitYeedStakeAmount", BigInteger.TEN.pow(24));
        createToken.addProperty("tokenInitMintAmount", BigInteger.TEN.pow(30));
        createToken.addProperty("tokenMintable", true);
        createToken.addProperty("tokenBurnable", true);
        createToken.addProperty("tokenExT2YEnabled", true);
        createToken.addProperty("tokenExT2YType", "TOKEN_EX_T2Y_TYPE_FIXED");
        createToken.addProperty("tokenExT2YRate", new BigDecimal("1.0"));

        tokenContract.createToken(createToken);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Duplicated token creation should be failed", tx.isSuccess());
    }

    @Test
    public void totalSupply() {
        Receipt tx = _testInit();

        // NONEXISTENT TOKEN
        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        BigInteger totalSupply = tokenContract.totalSupply(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals("The result must be null", null, totalSupply);

        // NORMAL
        params.addProperty("tokenId", "TEST_TOKEN");

        totalSupply = tokenContract.totalSupply(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals("Total supply should match with initial mint", 0, totalSupply.compareTo(BigInteger.TEN.pow(30)));
    }

    @Test
    public void balanceOf() {
        Receipt tx = _testInit();

        // NONEXISTENT TOKEN
        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");
        params.addProperty("address", tx.getIssuer());

        BigInteger result = tokenContract.totalSupply(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals("The result must be null", null, result);

        // NORMAL
        params.addProperty("tokenId", "TEST_TOKEN");

        result = tokenContract.totalSupply(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals("The balance should match with initial mint!!!", 0, result.compareTo(BigInteger.TEN.pow(30)));
    }

    @Test
    public void getYeedBalanceOf() {
        Receipt tx = _testInit();

        // NONEXISTENT TOKEN
        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        BigInteger result = tokenContract.getYeedBalanceOf(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals("The result must be null", null, result);

        // NORMAL
        params.addProperty("tokenId", "TEST_TOKEN");

        result = tokenContract.getYeedBalanceOf(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertEquals("The result should match with initial YEED stake", 0, result.compareTo(BigInteger.TEN.pow(24)));
    }

    @Test
    public void allowance() {
        final String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        final String spender = "0000000000000000000000000000000000000000";
        final String account2 = "2222222222222222222222222222222222222222";

        Receipt tx = _testInit();

        // NONEXISTENT TOKEN
        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");
        params.addProperty("owner", owner);
        params.addProperty("spender", spender);

        BigInteger result = tokenContract.allowance(params);

        Assert.assertEquals("Allowance at nonexistent token should returns null", null, result);

        // NOT APPROVED ACCOUNT
        tx = new ReceiptImpl("0x03", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("tokenId", "TEST_TOKEN");

        result = tokenContract.allowance(params);

        Assert.assertEquals("Allowance of not approved account should be ZERO", 0, result.compareTo(BigInteger.ZERO));

        // approve
        tx = new ReceiptImpl("0x03", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", getBigInt18(100000));

        tokenContract.approve(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("The approve is failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x04", 300L, owner);
        this.adapter.setReceipt(tx);

        result = tokenContract.allowance(params);

        Assert.assertEquals("The result should match with approved value", 0, result.compareTo(getBigInt18(100000)));

        // transferFrom
        tx = new ReceiptImpl("0x05", 300L, spender);
        this.adapter.setReceipt(tx);

        params.addProperty("from", owner);
        params.addProperty("to", account2);
        params.addProperty("amount", getBigInt18(10000));

        tokenContract.transferFrom(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Transfer from owner to account2 by spender is failed", tx.isSuccess());

        // NORMAL after transferFrom
        tx = new ReceiptImpl("0x06", 300L, owner);
        this.adapter.setReceipt(tx);

        result = tokenContract.allowance(params);

        Assert.assertEquals("The result should match with approved value", 0, result.compareTo(getBigInt18(90000)));
    }

    @Test
    public void depositYeedStake() {
        Receipt tx = _testInit();

        // NONEXISTENT TOKEN
        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");
        params.addProperty("amount", getBigInt18(100));

        tokenContract.depositYeedStake(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("The deposit to nonexistent token should be failed", tx.isSuccess());

        // NOT OWNER
        String issuer = "1111111111111111111111111111111111111111";
        tx = new ReceiptImpl("0x03", 300L, issuer);
        this.adapter.setReceipt(tx);

        params.addProperty("tokenId", "TEST_TOKEN");
        params.addProperty("amount", getBigInt18(100));

        tokenContract.depositYeedStake(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Deposit by who does not own token should be failed", tx.isSuccess());

        // OVER BALANCE
        String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        tx = new ReceiptImpl("0x04", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.TEN.pow(50)); // balance == TEN.pow(40)

        tokenContract.depositYeedStake(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Deposit over balance should be failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x05", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", getBigInt18(100));

        tokenContract.depositYeedStake(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Additional deposit of YEED stake is failed", tx.isSuccess());

        // YEED BALANCE
        BigInteger result = tokenContract.getYeedBalanceOf(params);

        Assert.assertEquals("The result should match with current YEED stake", 0, result.compareTo(getBigInt18(1000100)));
    }

    @Test
    public void withdrawYeedStake() {
        Receipt tx = _testInit();

        // NONEXISTENT TOKEN
        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.withdrawYeedStake(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Withdraw from nonexistent token should be failed", tx.isSuccess());

        // NOT OWNER
        tx = new ReceiptImpl("0x04", 300L, "1111111111111111111111111111111111111111");
        this.adapter.setReceipt(tx);

        params.addProperty("tokenId", "TEST_TOKEN");

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Withdraw by who does not own token should be failed", tx.isSuccess());

        // OVER BALANCE
        tx = new ReceiptImpl("0x05", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.TEN.pow(50)); // balance == TEN.pow(40)

        tokenContract.withdrawYeedStake(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Withdraw over YEED stake balance should be failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x06", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        params.addProperty("amount", getBigInt18(100));

        tokenContract.withdrawYeedStake(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Withdraw of YEED stake is failed", tx.isSuccess());

        // CHECK BALANCE
        BigInteger result = tokenContract.getYeedBalanceOf(params);

        Assert.assertEquals("The result should match with current YEED stake", 0, result.compareTo(getBigInt18(999900)));
    }

    @Test
    public void movePhaseRun() {
        createToken();

        // NONE_TOKEN
        Receipt tx = new ReceiptImpl("0x02", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move of nonexistent token should be failed", tx.isSuccess());

        // NOT_OWNER
        params.addProperty("tokenId", "TEST_TOKEN");

        tx = new ReceiptImpl("0x03", 300L, "1111111111111111111111111111111111111111");
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move by who does not own token should be failed", tx.isSuccess());

        // INIT -> RUN NORMAL
        tx = new ReceiptImpl("0x04", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from INIT to RUN is failed", tx.isSuccess());

        // RUN -> PAUSE -> RUN NORMAL
        tx = new ReceiptImpl("0x05", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        tokenContract.movePhasePause(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from RUN to PAUSE is failed", tx.isSuccess());

        tx = new ReceiptImpl("0x06", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from PAUSE to RUN is failed", tx.isSuccess());

        // RUN -> RUN FAIL
        tx = new ReceiptImpl("0x07", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move RUN to RUN should be failed", tx.isSuccess());
    }

    @Test
    public void movePhasePause() {
        createToken();

        // NONE_TOKEN
        Receipt tx = new ReceiptImpl("0x02", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.movePhasePause(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move of nonexistent token should be failed", tx.isSuccess());

        // NOT_OWNER
        params.addProperty("tokenId", "TEST_TOKEN");

        tx = new ReceiptImpl("0x03", 300L, "1111111111111111111111111111111111111111");
        this.adapter.setReceipt(tx);

        tokenContract.movePhasePause(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move by who does not own token should be failed", tx.isSuccess());

        // RUN -> PAUSE NORMAL
        tx = new ReceiptImpl("0x04", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from INIT to RUN is failed", tx.isSuccess());

        tx = new ReceiptImpl("0x05", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        tokenContract.movePhasePause(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from RUN to PAUSE is failed", tx.isSuccess());

        // PAUSE -> PAUSE FAIL
        tx = new ReceiptImpl("0x07", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        tokenContract.movePhasePause(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move PAUSE to PAUSE should be failed", tx.isSuccess());
    }

    @Test
    public void movePhaseStop() {
        createToken();

        // NONE_TOKEN
        Receipt tx = new ReceiptImpl("0x02", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.movePhaseStop(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move of nonexistent token should be failed", tx.isSuccess());

        // NOT_OWNER
        params.addProperty("tokenId", "TEST_TOKEN");

        tx = new ReceiptImpl("0x03", 300L, "1111111111111111111111111111111111111111");
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseStop(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move by who does not own token should be failed", tx.isSuccess());

        // RUN -> PAUSE -> STOP NORMAL
        tx = new ReceiptImpl("0x04", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from INIT to RUN is failed", tx.isSuccess());

        tx = new ReceiptImpl("0x05", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        tokenContract.movePhasePause(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from RUN to PAUSE is failed", tx.isSuccess());

        tx = new ReceiptImpl("0x06", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseStop(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from PAUSE to STOP is failed", tx.isSuccess());

        // STOP -> STOP FAIL
        tx = new ReceiptImpl("0x07", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseStop(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move STOP to STOP should be failed", tx.isSuccess());

        // STOP -> RUN FAIL
        tx = new ReceiptImpl("0x08", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        tokenContract.movePhaseRun(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Phase move STOP to RUN should be failed", tx.isSuccess());
    }

    @Test
    public void transfer() {
        createToken();

        // NONE_TOKEN
        String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        String account1 = "1111111111111111111111111111111111111111";

        Receipt tx = new ReceiptImpl("0x02", 300L, owner);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.transfer(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of nonexistent token should be failed", tx.isSuccess());

        // NOT_RUNNING
        tx = new ReceiptImpl("0x03", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.transfer(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of not running token should be failed", tx.isSuccess());

        // move phase to run
        tx = new ReceiptImpl("0x04", 300L, owner);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseRun(params);

        // NEGATIVE AMOUNT
        tx = new ReceiptImpl("0x05", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("to", account1);
        params.addProperty("amount", BigInteger.valueOf(-1).multiply(BigInteger.TEN.pow(18)));

        tokenContract.transfer(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of negative amount should be failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x06", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));

        tokenContract.transfer(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Transfer is failed", tx.isSuccess());

        // INSUFFICIENT BALANCE
        tx = new ReceiptImpl("0x07", 300L, account1);
        this.adapter.setReceipt(tx);

        params.addProperty("to", owner);
        params.addProperty("amount", BigInteger.valueOf(101).multiply(BigInteger.TEN.pow(18)));

        tokenContract.transfer(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer from insufficient balance should be failed", tx.isSuccess());
    }

    @Test
    public void approve() {
        String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        String spender = "0000000000000000000000000000000000000000";

        createToken();

        // NONEXISTENT TOKEN
        Receipt tx = new ReceiptImpl("0x02", 300L, owner);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.approve(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Approve for nonexistent token should be failed", tx.isSuccess());

        // NOT RUNNING
        tx = new ReceiptImpl("0x04", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.approve(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Approve for not running token should be failed", tx.isSuccess());

        // move phase to run
        tx = new ReceiptImpl("0x05", 300L, owner);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseRun(params);

        // INSUFFICIENT BALANCE
        tx = new ReceiptImpl("0x06", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.TEN.pow(40));
        params.addProperty("spender", "0000000000000000000000000000000000000000");

        tokenContract.approve(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Approve over balance should be failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x07", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(100000).multiply(BigInteger.TEN.pow(18)));

        tokenContract.approve(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("The approve is failed", tx.isSuccess());

        // ALLOWANCE
        tx = new ReceiptImpl("0x08", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("owner", owner);
        params.addProperty("spender", spender);

        BigInteger result = tokenContract.allowance(params);

        Assert.assertEquals("The result should match with approved value", 0, result.compareTo(getBigInt18(100000)));
    }

    @Test
    public void transferFrom() {
        createToken();

        String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        String account1 = "1111111111111111111111111111111111111111";
        String account2 = "2222222222222222222222222222222222222222";

        // NONEXISTENT
        Receipt tx = new ReceiptImpl("0x02", 300L, account1);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.transferFrom(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of nonexistent token should be failed", tx.isSuccess());

        // NOT RUNNING
        tx = new ReceiptImpl("0x03", 300L, account1);
        this.adapter.setReceipt(tx);

        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.transferFrom(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of nonexistent token should be failed", tx.isSuccess());

        // move phase to run
        tx = new ReceiptImpl("0x04", 300L, owner);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseRun(params);

        // NEGATIVE AMOUNT
        tx = new ReceiptImpl("0x05", 300L, account1);
        this.adapter.setReceipt(tx);

        params.addProperty("from", owner);
        params.addProperty("to", account2);
        params.addProperty("amount", BigInteger.valueOf(-1).multiply(BigInteger.TEN.pow(18)));

        tokenContract.transferFrom(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer of negative amount should be failed", tx.isSuccess());

        // approve 100
        tx = new ReceiptImpl("0x06", 300L, owner);
        this.adapter.setReceipt(tx);

        JsonObject paramsApprove = new JsonObject();
        paramsApprove.addProperty("tokenId", "TEST_TOKEN");
        paramsApprove.addProperty("amount", BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));
        paramsApprove.addProperty("spender", account1);

        tokenContract.approve(paramsApprove);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("The approve is failed", tx.isSuccess());

        // INSUFFICIENT BALANCE 200
        tx = new ReceiptImpl("0x07", 300L, account1);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(200).multiply(BigInteger.TEN.pow(18)));

        tokenContract.transferFrom(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Transfer from insufficient approved balance should be failed", tx.isSuccess());

        // NORMAL 50
        tx = new ReceiptImpl("0x08", 300L, account1);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(50).multiply(BigInteger.TEN.pow(18)));

        tokenContract.transferFrom(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
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
        Receipt tx = new ReceiptImpl("0x02", 300L, owner);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.mint(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Mint of nonexistent token should be failed", tx.isSuccess());

        // NOT OWNER
        tx = new ReceiptImpl("0x03", 300L, account1);
        this.adapter.setReceipt(tx);

        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.mint(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Mint by non-owner account should be failed", tx.isSuccess());

        // NEGATIVE AMOUNT
        tx = new ReceiptImpl("0x05", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(-1).multiply(BigInteger.TEN.pow(18)));

        tokenContract.mint(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Mint of negative amount should be failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x06", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));

        tokenContract.mint(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Mint is failed", tx.isSuccess());

        // TOTAL SUPPLY
        BigInteger expected = BigInteger.TEN.pow(30).add(BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));
        BigInteger result = tokenContract.totalSupply(params);
        Assert.assertEquals("Total supply should be same as expected", expected, result);

        // move phase to stop
        tx = new ReceiptImpl("0x07", 300L, owner);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseRun(params);

        tx = new ReceiptImpl("0x08", 300L, owner);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseStop(params);

        // STOPPED PHASE
        tx = new ReceiptImpl("0x09", 300L, owner);
        this.adapter.setReceipt(tx);

        tokenContract.mint(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Mint at stop phase should be failed", tx.isSuccess());
    }

    @Test
    public void mintNotMintable() {
        Boolean mintable = false;
        createToken(null, null, null, null, null, null, mintable, null, null, null, null);

        Receipt tx = new ReceiptImpl("0x02", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.mint(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Mint of non-mintable token should be failed", tx.isSuccess());
    }

    @Test
    public void burn() {
        createToken();

        String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        String account1 = "1111111111111111111111111111111111111111";

        // NONEXISTENT TOKEN
        Receipt tx = new ReceiptImpl("0x02", 300L, owner);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.burn(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Burn of nonexistent token should be failed", tx.isSuccess());

        // NOT OWNER
        tx = new ReceiptImpl("0x03", 300L, account1);
        this.adapter.setReceipt(tx);

        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.burn(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Burn by non-owner account should be failed", tx.isSuccess());

        // NEGATIVE AMOUNT
        tx = new ReceiptImpl("0x05", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(-1).multiply(BigInteger.TEN.pow(18)));

        tokenContract.burn(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Burn of negative amount should be failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x06", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));

        tokenContract.burn(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Burn is failed", tx.isSuccess());

        // TOTAL SUPPLY
        BigInteger expected = BigInteger.TEN.pow(30).subtract(BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)));
        BigInteger result = tokenContract.totalSupply(params);
        Assert.assertEquals("Total supply should be same as expected", expected, result);

        // move phase to stop
        tx = new ReceiptImpl("0x07", 300L, owner);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseRun(params);

        tx = new ReceiptImpl("0x08", 300L, owner);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseStop(params);

        // STOPPED PHASE
        tx = new ReceiptImpl("0x09", 300L, owner);
        this.adapter.setReceipt(tx);

        tokenContract.burn(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Burn at stop phase should be failed", tx.isSuccess());
    }

    @Test
    public void burnNotBurnable() {
        Boolean burnable = false;
        createToken(null, null, null, null, null, null, null, burnable, null, null, null);

        Receipt tx = new ReceiptImpl("0x02", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.burn(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Burn of non-mintable token should be failed", tx.isSuccess());
    }

    @Test
    public void exchangeT2Y() {
        createToken();

        // NONEXISTENT TOKEN
        String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        Receipt tx = new ReceiptImpl("0x02", 300L, owner);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.exchangeT2Y(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange of nonexistent token should be failed", tx.isSuccess());

        // NOT_RUNNING
        tx = new ReceiptImpl("0x03", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.exchangeT2Y(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange of not running token should be failed", tx.isSuccess());

        // move phase to run
        tx = new ReceiptImpl("0x04", 300L, owner);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseRun(params);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Move phase to run is failed", tx.isSuccess());

        // issue token to account1
        String account1 = "1111111111111111111111111111111111111111";

        tx = new ReceiptImpl("0x05", 300L, owner);
        this.adapter.setReceipt(tx);
        params.addProperty("to", account1);
        params.addProperty("amount", BigInteger.valueOf(2000000).multiply(BigInteger.TEN.pow(18)));
        tokenContract.transfer(params);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Transfer is failed", tx.isSuccess());

        // NEGATIVE AMOUNT
        tx = new ReceiptImpl("0x06", 300L, account1);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(-1).multiply(BigInteger.TEN.pow(18)));

        tokenContract.exchangeT2Y(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange of negative amount should be failed", tx.isSuccess());

        // INSUFFICIENT BALANCE
        tx = new ReceiptImpl("0x07", 300L, account1);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(2000001).multiply(BigInteger.TEN.pow(18)));

        tokenContract.exchangeT2Y(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange over balance should be failed", tx.isSuccess());

        // OVER YEED STAKE
        tx = new ReceiptImpl("0x08", 300L, account1);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(1000001).multiply(BigInteger.TEN.pow(18)));

        tokenContract.exchangeT2Y(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange over yeed stake should be failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x09", 300L, account1);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(400000).multiply(BigInteger.TEN.pow(18)));

        tokenContract.exchangeT2Y(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Exchange is failed", tx.isSuccess());

        // CHECK TOKEN BALANCE
        params.addProperty("address", account1);
        BigInteger tokenBalance = tokenContract.balanceOf(params);
        Assert.assertEquals("Token balance should be 600000", 0, tokenBalance.compareTo(BigInteger.valueOf(1600000).multiply(BigInteger.TEN.pow(18))));

        // CHECK YEED STAKE BALANCE
        BigInteger yeedBalance = tokenContract.getYeedBalanceOf(params);
        Assert.assertEquals("Yeed stake balance should be 600000", 0, yeedBalance.compareTo(BigInteger.valueOf(600000).multiply(BigInteger.TEN.pow(18))));
    }

    @Test
    public void exchangeY2T() {
        createToken();

        // NONEXISTENT TOKEN
        String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        Receipt tx = new ReceiptImpl("0x02", 300L, owner);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");

        tokenContract.exchangeY2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange YEED to token of nonexistent token should be failed", tx.isSuccess());

        // NOT_RUNNING
        tx = new ReceiptImpl("0x03", 300L, owner);
        this.adapter.setReceipt(tx);

        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.exchangeY2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange YEED to token of not running token should be failed", tx.isSuccess());

        // move phase to run
        tx = new ReceiptImpl("0x04", 300L, owner);
        this.adapter.setReceipt(tx);
        tokenContract.movePhaseRun(params);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Move phase to run is failed", tx.isSuccess());

        // NEGATIVE AMOUNT
        String account1 = "1111111111111111111111111111111111111111";

        tx = new ReceiptImpl("0x06", 300L, account1);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(-1).multiply(BigInteger.TEN.pow(18)));

        tokenContract.exchangeY2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange YEED to token of negative amount should be failed", tx.isSuccess());

        // INSUFFICIENT BALANCE (current YEED amount = 1234)
        tx = new ReceiptImpl("0x07", 300L, account1);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(10000).multiply(BigInteger.TEN.pow(18)));

        tokenContract.exchangeY2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange YEED to token over YEED balance should be failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x09", 300L, account1);
        this.adapter.setReceipt(tx);

        params.addProperty("amount", BigInteger.valueOf(1000).multiply(BigInteger.TEN.pow(18)));

        tokenContract.exchangeY2T(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Exchange YEED to token is failed", tx.isSuccess());

        // CHECK TOKEN BALANCE (1000)
        params.addProperty("address", account1);
        BigInteger tokenBalance = tokenContract.balanceOf(params);
        Assert.assertEquals("Token balance should be 1000", 0, tokenBalance.compareTo(BigInteger.valueOf(1000).multiply(BigInteger.TEN.pow(18))));

        // CHECK YEED STAKE BALANCE (1000000 + 1000)
        BigInteger yeedBalance = tokenContract.getYeedBalanceOf(params);
        Assert.assertEquals("Yeed stake balance should be 600000", 0, yeedBalance.compareTo(BigInteger.valueOf(1001000).multiply(BigInteger.TEN.pow(18))));
    }

    @Test
    public void exchangeT2TOpen() {
        Receipt tx = _testInit();

        // NONEXISTENT TOKEN
        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "NONE_TOKEN");
        params.addProperty("tokenExT2TTargetTokenId", "TARGET_TOKEN");

        tokenContract.exchangeT2TOpen(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange open from nonexistent token should be failed", tx.isSuccess());

        // NOT OWNER
        tx = new ReceiptImpl("0x04", 300L, "1111111111111111111111111111111111111111");
        this.adapter.setReceipt(tx);

        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.exchangeT2TOpen(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange open by non-owner account should be failed", tx.isSuccess());

        // NONEXISTENT TARGET TOKEN
        tx = new ReceiptImpl("0x05", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        tokenContract.exchangeT2TOpen(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange open to nonexistent target token should be failed", tx.isSuccess());

        // create target token
        createToken("0x06", "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e",
                "TARGET_TOKEN", "targetToken",
                null, null, null, null, null, null, null);

        // NORMAL
        tx = new ReceiptImpl("0x07", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        params.addProperty("tokenExT2TRate", new BigDecimal("1.0"));

        tokenContract.exchangeT2TOpen(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Exchange open to target token is failed", tx.isSuccess());

        // ALREADY OPEN
        tx = new ReceiptImpl("0x08", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
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
        params.addProperty("tokenId", "NONE_TOKEN");
        params.addProperty("tokenExT2TTargetTokenId", "TARGET_TOKEN");

        tokenContract.exchangeT2TClose(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange close from nonexistent token should be failed", tx.isSuccess());

        // NOT OWNER
        tx = new ReceiptImpl("0x04", 300L, "1111111111111111111111111111111111111111");
        this.adapter.setReceipt(tx);

        params.addProperty("tokenId", "TEST_TOKEN");

        tokenContract.exchangeT2TClose(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange close by non-owner account should be failed", tx.isSuccess());

        // NONEXISTENT TARGET TOKEN
        tx = new ReceiptImpl("0x05", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        tokenContract.exchangeT2TClose(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange close to nonexistent target token should be failed", tx.isSuccess());

        // create target token
        createToken("0x06", "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e",
                "TARGET_TOKEN", "targetToken",
                null, null, null, null, null, null, null);

        // ALREADY CLOSED
        tx = new ReceiptImpl("0x07", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        tokenContract.exchangeT2TClose(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange close to already closed target token should be failed", tx.isSuccess());

        // open
        tx = new ReceiptImpl("0x08", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        params.addProperty("tokenExT2TRate", new BigDecimal("1.0"));

        tokenContract.exchangeT2TOpen(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Exchange open to target token is failed", tx.isSuccess());

        // NORMAL
        tx = new ReceiptImpl("0x09", 300L, "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        this.adapter.setReceipt(tx);

        tokenContract.exchangeT2TClose(params);

        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Exchange close to target token is failed", tx.isSuccess());
    }

    @Test
    public void exchangeT2T() {
        String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        String targetOwner = "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e";
        String account1 = "1111111111111111111111111111111111111111";

        // NONEXISTENT TOKEN
        Receipt rct = new ReceiptImpl("0x02", 300L, account1);
        this.adapter.setReceipt(rct);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");
        params.addProperty("tokenExT2TTargetTokenId", "TARGET_TOKEN");

        tokenContract.exchangeT2T(params);

        rct.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange from nonexistent token should be failed", rct.isSuccess());

        //     create token
        createToken();

        // NOT RUNNING
        rct = new ReceiptImpl("0x04", 300L, account1);
        this.adapter.setReceipt(rct);

        tokenContract.exchangeT2T(params);

        rct.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange of not running token should be failed", rct.isSuccess());

        //     move phase to run
        rct = new ReceiptImpl("0x05", 300L, owner);
        this.adapter.setReceipt(rct);

        tokenContract.movePhaseRun(params);

        rct.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase move from INIT to RUN is failed", rct.isSuccess());

        //     transfer 10000 to account1
        rct = new ReceiptImpl("0x03", 300L, owner);
        this.adapter.setReceipt(rct);

        params.addProperty("to", account1);
        params.addProperty("amount", getBigInt18(10000));

        tokenContract.transfer(params);

        rct.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Transfer is failed", rct.isSuccess());

        //     create target token
        createToken("0x06", targetOwner,
                "TARGET_TOKEN", "targetToken",
                null, null, null, null, null, null, null);

        // NOT OPEN TO TARGET TOKEN
        rct = new ReceiptImpl("0x07", 300L, account1);
        this.adapter.setReceipt(rct);

        tokenContract.exchangeT2T(params);

        rct.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange to not open target token should be failed", rct.isSuccess());

        //     open to target token
        rct = new ReceiptImpl("0x08", 300L, owner);
        this.adapter.setReceipt(rct);

        params.addProperty("tokenExT2TRate", new BigDecimal("1.0"));

        tokenContract.exchangeT2TOpen(params);

        rct.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Exchange open to target token is failed", rct.isSuccess());

        // TARGET TOKEN NOT RUNNING
        rct = new ReceiptImpl("0x09", 300L, account1);
        this.adapter.setReceipt(rct);

        JsonObject targetParams = new JsonObject();
        targetParams.addProperty("tokenId", "TARGET_TOKEN");

        tokenContract.exchangeT2T(targetParams);

        rct.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange to not running target token should be failed", rct.isSuccess());

        //     target token move phase to run
        rct = new ReceiptImpl("0x10", 300L, targetOwner);
        this.adapter.setReceipt(rct);

        tokenContract.movePhaseRun(targetParams);

        rct.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Phase of target token move from INIT to RUN is failed", rct.isSuccess());

        // ZERO AMOUNT
        rct = new ReceiptImpl("0x11", 300L, account1);
        this.adapter.setReceipt(rct);

        params.addProperty("amount", BigInteger.ZERO);

        tokenContract.exchangeT2T(params);

        rct.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange amount should be greater than ZERO", rct.isSuccess());

        // INSUFFICIENT BALANCE
        rct = new ReceiptImpl("0x11", 300L, account1);
        this.adapter.setReceipt(rct);

        params.addProperty("amount", getBigInt18(20000));

        tokenContract.exchangeT2T(params);

        rct.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertFalse("Exchange amount should be less than balance", rct.isSuccess());

        // NORMAL
        rct = new ReceiptImpl("0x12", 300L, account1);
        this.adapter.setReceipt(rct);

        params.addProperty("amount", getBigInt18(1000));

        tokenContract.exchangeT2T(params);

        rct.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Exchange failed", rct.isSuccess());

        // TOKEN BALANCE
        params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");
        params.addProperty("address", account1);

        BigInteger tokenBalance = tokenContract.balanceOf(params);
        Assert.assertEquals("Token balance should match", 0, tokenBalance.compareTo(getBigInt18(9000)));

        // TARGET TOKEN BALANCE
        params.addProperty("tokenId", "TARGET_TOKEN");

        BigInteger targetTokenBalance = tokenContract.balanceOf(params);
        Assert.assertEquals("Target token balance should match", 0, targetTokenBalance.compareTo(getBigInt18(1000)));
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
            BigDecimal exRateT2Y) {

        if (txId == null) txId = "0x01";
        if (owner == null) owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        if (tokenId == null) tokenId = "TEST_TOKEN";
        if (tokenName == null) tokenName = "TTOKEN";
        if (initStake == null) initStake = BigInteger.TEN.pow(24); // 1백만
        if (initMint == null) initMint = BigInteger.TEN.pow(30); // 1조 개 (1 * 10^12)
        if (mintable == null) mintable = true;
        if (burnable == null) burnable = true;
        if (exchangeable == null) exchangeable = true;
        if (exType == null) exType = "TOKEN_EX_T2Y_TYPE_FIXED";
        if (exRateT2Y == null) exRateT2Y = new BigDecimal("1.0");

        Receipt tx = new ReceiptImpl(txId, 300L, owner);
        this.adapter.setReceipt(tx);

        JsonObject createToken = new JsonObject();
        createToken.addProperty("tokenId", tokenId);
        createToken.addProperty("tokenName", tokenName);
        createToken.addProperty("tokenInitYeedStakeAmount", initStake);
        createToken.addProperty("tokenInitMintAmount", initMint);
        createToken.addProperty("tokenMintable", mintable);
        createToken.addProperty("tokenBurnable", burnable);

        createToken.addProperty("tokenExT2YEnabled", exchangeable);
        createToken.addProperty("tokenExT2YType", exType);
        createToken.addProperty("tokenExT2YRate", exRateT2Y);

        tokenContract.createToken(createToken);
        tx.getLog().stream().forEach(l -> log.debug(l));
        Assert.assertTrue("Token creation is failed", tx.isSuccess());
    }

    private Receipt _testInit() {
        createToken();

        String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        Receipt tx = new ReceiptImpl("0x02", 300L, owner);
        this.adapter.setReceipt(tx);

        JsonObject params = new JsonObject();
        params.addProperty("tokenId", "TEST_TOKEN");

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