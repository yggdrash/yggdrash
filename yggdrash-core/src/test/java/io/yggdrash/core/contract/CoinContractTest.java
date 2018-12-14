package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import org.junit.Before;
import org.junit.Test;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoinContractTest {

    private static final CoinContract coinContract = new CoinContract();
    private static final Logger log = LoggerFactory.getLogger(AssetContractTest.class);

    @Before
    public void setUp() {
        StateStore<JsonObject> coinContractStateStore = null;
        coinContractStateStore = new StateStore<>(new HashMapDbSource());
        TransactionReceiptStore txReceip = new TransactionReceiptStore(new HashMapDbSource());
        coinContract.init(coinContractStateStore, txReceip);
        genesis();
    }

    public void genesis() {
        String genesisStr = "{\"alloc\": {\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\":"
                + " {\"balance\": \"1000000000\"},\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\":"
                + " {\"balance\": \"1000000000\"},\"cee3d4755e47055b530deeba062c5bd0c17eb00f\":"
                + " {\"balance\": \"998000000000\"}}}";

        TransactionReceipt result = coinContract.genesis(createParam(genesisStr));

        assertTrue(result.isSuccess());
        assertEquals(4, result.getTxLog().size());
    }

    @Test
    public void specification() {
        StateStore<JsonObject> coinContractStateStore;
        coinContractStateStore = new StateStore<>(new HashMapDbSource());
        MetaCoinContract metaCoinContract = new MetaCoinContract();
        TransactionReceiptStore txReceip = new TransactionReceiptStore(new HashMapDbSource());
        metaCoinContract.init(coinContractStateStore, txReceip);

        List<String> methods = metaCoinContract.specification(null);

        assertFalse(methods.isEmpty());
        assertEquals(8, methods.size());

        methods = coinContract.specification(null);

        assertFalse(methods.isEmpty());
        assertEquals(7, methods.size());
    }

    @Test
    public void totalSupply() {
        BigDecimal res = coinContract.totalsupply(null);

        assertEquals(BigDecimal.valueOf(1000000000000L), res);
    }

    @Test
    public void balanceOf() {
        String paramStr = "{\"address\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\"}";

        BigDecimal res = coinContract.balanceof(createParam(paramStr));
        assertEquals(BigDecimal.valueOf(1000000000), res);
    }

    @Test
    public void allowance() {
        String paramStr = "{\"owner\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\","
                + "\"spender\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\"}";

        BigDecimal res = coinContract.allowance(createParam(paramStr));

        assertEquals(BigDecimal.ZERO, res);
    }

    @Test
    public void transfer() {
        String paramStr = "{\"to\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\","
                + "\"amount\" : \"10\"}";

        // tx 가 invoke 되지 않아 baseContract 에 sender 가 세팅되지 않아서 설정해줌
        coinContract.sender = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        String balanceOf = "{\"address\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\"}";
        String toBalnce = "{\"address\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\"}";

        log.debug("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94 : " + coinContract.balanceof(createParam(balanceOf)).toString());
        log.debug("1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e : " + coinContract.balanceof(createParam(toBalnce)).toString());

        JsonObject param = createParam(paramStr);
        TransactionReceipt result = coinContract.transfer(param);

        assertTrue(result.isSuccess());

        String paramStr2 = "{\"address\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\"}";
        String paramStr3 = "{\"address\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\"}";

        assertEquals(BigDecimal.valueOf(1000000010),
                coinContract.balanceof(createParam(paramStr2)));
        assertEquals(BigDecimal.valueOf(999999990),
                coinContract.balanceof(createParam(paramStr3)));

        // To many amount
        param.addProperty("amount", BigDecimal.valueOf(1000000010));
        result = coinContract.transfer(param);
        assertFalse(result.isSuccess());

        // Same amount
        param.addProperty("amount", BigDecimal.valueOf(999999990));
        result = coinContract.transfer(param);
        assertTrue(result.isSuccess());

    }

    @Test
    public void transferFrom() {
        String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        String spender = "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e";
        approveByOwner(owner, spender, "1000");

        String to = "cee3d4755e47055b530deeba062c5bd0c17eb00f";
        String transferParam = "{\"from\" : \"" + owner + "\","
                + "\"to\" : \"" + to + "\",\"amount\" : \"700\"}";


        JsonObject transferFromObject = createParam(transferParam);
        coinContract.sender = spender;
        TransactionReceipt result = coinContract.transferfrom(transferFromObject);
        assertTrue(result.isSuccess());
        assertEquals(BigDecimal.valueOf(300), getAllowance(owner, spender));
        log.debug(to + ": " + getBalance(to).toString());
        log.debug(owner + ": " + getBalance(owner).toString());
        log.debug(spender + ": " + getBalance(spender).toString());
        log.debug("getAllowance : " + getAllowance(owner, spender));

        TransactionReceipt result2 = coinContract.transferfrom(transferFromObject);
        // not enough amount allowed
        assertFalse(result2.isSuccess());
        //
        transferFromObject.addProperty("amount", getAllowance(owner, spender));
        result2 = coinContract.transferfrom(transferFromObject);
        assertTrue(result2.isSuccess());
        // reset
        assertEquals(BigDecimal.ZERO, getAllowance(owner, spender));
    }

    private void approveByOwner(String owner, String spender, String amount) {
        String approveParam = "{\"spender\" : \"" + spender + "\","
                + "\"amount\" : \"" + amount + "\"}";

        coinContract.sender = owner;
        TransactionReceipt result = coinContract.approve(createParam(approveParam));

        assertTrue(result.isSuccess());

        String spenderParam = "{\"address\" : \"" + spender + "\"}";
        String senderParam = "{\"address\" : \"" + owner + "\"}";

        assertEquals(BigDecimal.valueOf(1000000000),
                coinContract.balanceof(createParam(spenderParam)));
        assertEquals(BigDecimal.valueOf(1000000000),
                coinContract.balanceof(createParam(senderParam)));

        String allowanceParam = "{\"owner\" : \"" + owner + "\","
                + "\"spender\" : \"" + spender + "\"}";

        assertEquals(new BigDecimal(amount), coinContract.allowance(createParam(allowanceParam)));
    }

    private JsonObject createParam(String paramStr) {
        return Utils.parseJsonObject(paramStr);
    }

    public class MetaCoinContract extends CoinContract {
        public TransactionReceipt hello(JsonObject param) {
            TransactionReceipt txReceipt = new TransactionReceipt();
            txReceipt.putLog("hello", param.toString());
            txReceipt.setStatus(TransactionReceipt.SUCCESS);
            log.info(txReceipt.toString());
            return txReceipt;
        }
    }

    private BigDecimal getBalance(String  address) {
        JsonObject obj = new JsonObject();
        obj.addProperty("address", address);
        return coinContract.balanceof(obj);
    }

    private BigDecimal getAllowance(String owner, String spender) {
        JsonObject obj = new JsonObject();
        obj.addProperty("owner", owner);
        obj.addProperty("spender", spender);
        return coinContract.allowance(obj);
    }



}