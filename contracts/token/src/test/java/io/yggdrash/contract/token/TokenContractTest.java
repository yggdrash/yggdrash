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
        String issuer = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        TransactionReceipt tx = new TransactionReceiptImpl("0x01", 300L, issuer);
        this.adapter.setTransactionReceipt(tx);

        JsonObject createToken = new JsonObject();
        createToken.addProperty("tokenId", "TEST_TOKEN");
        // 100 만개
        createToken.addProperty("tokenInitYeedStakeAmount", BigInteger.TEN.pow(24));

        // Create Token Object
        createToken.addProperty("tokenName", "TTOKEN");
        // 1조 개 (1 * 10^12)
        createToken.addProperty("tokenInitMintAmount", BigInteger.TEN.pow(30));

        createToken.addProperty("tokenMintable", true);
        createToken.addProperty("tokenBurnable", true);
        createToken.addProperty("tokenExchangeable", true);

        createToken.addProperty("tokenExType", "TOKEN_EX_TYPE_FIXED");
        createToken.addProperty("tokenExRateT2Y", 1.0);


        tokenContract.createToken(createToken);

        tx.getTxLog().stream().forEach(l -> log.debug(l));

        Assert.assertTrue("Token creation is failed", tx.isSuccess());
    }

    @Test
    public void createTokenDuplicate() {
        createToken();

        String issuer = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        TransactionReceipt tx = new TransactionReceiptImpl("0x02", 300L, issuer);
        this.adapter.setTransactionReceipt(tx);

        JsonObject createToken = new JsonObject();
        createToken.addProperty("tokenId", "TEST_TOKEN");
        // 100 만개
        createToken.addProperty("tokenInitYeedStakeAmount", BigInteger.TEN.pow(24));

        // Create Token Object
        createToken.addProperty("tokenName", "TTOKEN");
        // 1조 개 (1 * 10^12)
        createToken.addProperty("tokenInitMintAmount", BigInteger.TEN.pow(30));

        createToken.addProperty("tokenMintable", true);
        createToken.addProperty("tokenBurnable", true);
        createToken.addProperty("tokenExchangeable", true);

        createToken.addProperty("tokenExType", "TOKEN_EX_TYPE_FIXED");
        createToken.addProperty("tokenExRateT2Y", 1.0);


        tokenContract.createToken(createToken);

        tx.getTxLog().stream().forEach(l -> log.debug(l));

        Assert.assertFalse("Duplicated token id creation should be failed", tx.isSuccess());
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
        Assert.assertFalse("The deposit to not existed token should be failed", tx.isSuccess());
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
        params.addProperty("amount", BigInteger.valueOf(1000000000000000L).multiply(BigInteger.TEN.pow(18)));

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
        Assert.assertFalse("The withdraw from not existed token should be failed", tx.isSuccess());
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
        Assert.assertFalse("The approve on not existed token should be failed", tx.isSuccess());
    }

    private void ref() {
        TokenContract contract = new TokenContract();
        TokenContract.TokenService service = new TokenContract.TokenService();

        System.out.println(service.approve(null));
        System.out.println(service.allowance(null));
        System.out.println(service.totalSupply(null));
        System.out.println(service.balanceOf(null));
        System.out.println(service.burn(null));
        System.out.println(service.exchangeClose(null));
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
        System.out.println(service.exchangeOpen(null));
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
}