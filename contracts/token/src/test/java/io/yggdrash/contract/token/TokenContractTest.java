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
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        Assert.assertEquals("init Success", ExecuteStatus.SUCCESS, adapter.getStatus());
    }

    @Test
    public void createToken() {
        String issuer = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        TransactionReceipt tx = new TransactionReceiptImpl("0x01", 300L, issuer);
        this.adapter.setTransactionReceipt(tx);

        JsonObject createToken = new JsonObject();
        createToken.addProperty("TOKEN_ID", "TEST_TOKEN");
        // 100 만개
        createToken.addProperty("TOKEN_INIT_YEED_STAKE_AMOUNT", BigInteger.TEN.pow(24));

        // Create Token Object
        createToken.addProperty("TOKEN_NAME", "TTOKEN");
        // 100 억개
        createToken.addProperty("TOKEN_INIT_MINT_AMOUNT", BigInteger.TEN.pow(30));

        createToken.addProperty("TOKEN_MINTABLE", true);
        createToken.addProperty("TOKEN_BURNABLE", true);
        createToken.addProperty("TOKEN_EXCHANGEABLE", true);

        createToken.addProperty("TOKEN_EX_TYPE", "TOKEN_EX_TYPE_FIXED");
        createToken.addProperty("TOKEN_EX_RATE_T2Y", 1.1);


        tokenContract.createToken(createToken);

        tx.getTxLog().stream().forEach(l -> log.debug(l));

        Assert.assertTrue("create Success", tx.isSuccess());




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
        System.out.println(service.depositYeedStakeToToken(null));
        System.out.println(service.destroyToken(null));
        System.out.println(service.exchangeT2T(null));
        System.out.println(service.exchangeT2Y(null));
        System.out.println(service.exchangeY2T(null));
        System.out.println(service.getTokenBalanceOf(null));
        System.out.println(service.getYeedBalanceOf(null));
        System.out.println(service.init(null));
        System.out.println(service.mint(null));
        System.out.println(service.movePhasePause(null));
        System.out.println(service.movePhaseRun(null));
        System.out.println(service.movePhaseStop(null));
        System.out.println(service.exchangeOpen(null));
        System.out.println(service.transfer(null));
        System.out.println(service.transferFrom(null));
        System.out.println(service.withdrawYeedStakeFromToken(null));
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