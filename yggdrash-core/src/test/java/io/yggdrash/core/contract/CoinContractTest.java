package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.util.ContractUtils;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.runtime.annotation.ContractStateStore;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CoinContractTest {

    private static final CoinContract coinContract = new CoinContract();
    private static final Logger log = LoggerFactory.getLogger(CoinContractTest.class);
    private Field txReceiptField;

    @Before
    public void setUp() throws IllegalAccessException {
        StateStore<JsonObject> coinContractStateStore = new StateStore<>(new HashMapDbSource());

        List<Field> txReceipt = ContractUtils.txReceipt(coinContract);
        if (txReceipt.size() == 1) {
            txReceiptField = txReceipt.get(0);
        }
        for (Field f : ContractUtils.contractFields(coinContract, ContractStateStore.class)) {
            f.setAccessible(true);
            f.set(coinContract, coinContractStateStore);
        }

        genesis();
    }

    public void genesis() {
        String genesisStr = "{\"alloc\": {\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\":"
                + " {\"balance\": \"1000000000\"},\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\":"
                + " {\"balance\": \"1000000000\"},\"cee3d4755e47055b530deeba062c5bd0c17eb00f\":"
                + " {\"balance\": \"998000000000\"}}}";

        TransactionReceipt result = new TransactionReceiptImpl();

        try {
            txReceiptField.set(coinContract, result);
            coinContract.genesis(createParams(genesisStr));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }


        assertTrue(result.isSuccess());
        assertEquals(4, result.getTxLog().size());
    }

    @Test
    public void totalSupply() {
        BigDecimal res = coinContract.totalsupply();

        assertEquals(BigDecimal.valueOf(1000000000000L), res);
    }

    @Test
    public void balanceOf() {
        String paramStr = "{\"address\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\"}";

        BigDecimal res = coinContract.balanceof(createParams(paramStr));
        assertEquals(BigDecimal.valueOf(1000000000), res);
    }

    @Test
    public void allowance() {
        String paramStr = "{\"owner\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\","
                + "\"spender\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\"}";

        BigDecimal res = coinContract.allowance(createParams(paramStr));

        assertEquals(BigDecimal.ZERO, res);
    }

    @Test
    public void transfer() {
        final String paramStr = "{\"to\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\","
                + "\"amount\" : \"10\"}";

        // tx 가 invoke 되지 않아 baseContract 에 sender 가 세팅되지 않아서 설정해줌
        String balanceOf = "{\"address\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\"}";
        String toBalnce = "{\"address\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\"}";

        log.debug("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94 : "
                + coinContract.balanceof(createParams(balanceOf)).toString());
        log.debug("1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e : "
                + coinContract.balanceof(createParams(toBalnce)).toString());

        JsonObject param = createParams(paramStr);

        TransactionReceipt result = new TransactionReceiptImpl();
        result.setIssuer("c91e9d46dd4b7584f0b6348ee18277c10fd7cb94");
        try {
            txReceiptField.set(coinContract, result);
            result = coinContract.transfer(param);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        assertTrue(result.isSuccess());

        String paramStr2 = "{\"address\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\"}";
        String paramStr3 = "{\"address\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\"}";

        assertEquals(BigDecimal.valueOf(1000000010),
                coinContract.balanceof(createParams(paramStr2)));
        assertEquals(BigDecimal.valueOf(999999990),
                coinContract.balanceof(createParams(paramStr3)));

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
        String transferParams = "{\"from\" : \"" + owner + "\","
                + "\"to\" : \"" + to + "\",\"amount\" : \"700\"}";

        JsonObject transferFromObject = createParams(transferParams);

        TransactionReceipt result = new TransactionReceiptImpl();
        result.setIssuer(spender);
        try {
            txReceiptField.set(coinContract, result);
            result = coinContract.transferfrom(transferFromObject);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        assertTrue(result.isSuccess());
        assertEquals(BigDecimal.valueOf(300), getAllowance(owner, spender));
        log.debug(to + ": " + getBalance(to).toString());
        log.debug(owner + ": " + getBalance(owner).toString());
        log.debug(spender + ": " + getBalance(spender).toString());
        log.debug("getAllowance : " + getAllowance(owner, spender));

        TransactionReceipt result2 = new TransactionReceiptImpl();
        try {
            txReceiptField.set(coinContract, result);
            coinContract.transferfrom(transferFromObject);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        // not enough amount allowed
        assertFalse(result2.isSuccess());

        transferFromObject.addProperty("amount", getAllowance(owner, spender));
        result2 = coinContract.transferfrom(transferFromObject);
        assertTrue(result2.isSuccess());
        // reset
        assertEquals(BigDecimal.ZERO, getAllowance(owner, spender));
    }

    private void approveByOwner(String owner, String spender, String amount) {
        String approveParams = "{\"spender\" : \"" + spender + "\","
                + "\"amount\" : \"" + amount + "\"}";

        TransactionReceipt result = new TransactionReceiptImpl();
        result.setIssuer(owner);
        try {
            txReceiptField.set(coinContract, result);
            coinContract.approve(createParams(approveParams));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        assertTrue(result.isSuccess());

        String spenderParams = "{\"address\" : \"" + spender + "\"}";
        String senderParams = "{\"address\" : \"" + owner + "\"}";

        assertEquals(BigDecimal.valueOf(1000000000),
                coinContract.balanceof(createParams(spenderParams)));
        assertEquals(BigDecimal.valueOf(1000000000),
                coinContract.balanceof(createParams(senderParams)));

        String allowanceParams = "{\"owner\" : \"" + owner + "\","
                + "\"spender\" : \"" + spender + "\"}";

        assertEquals(new BigDecimal(amount), coinContract.allowance(createParams(allowanceParams)));
    }

    private void assertTransferFrom(String to, String owner, String spender) {

        String allowanceParams = "{\"owner\" : \"" + owner + "\","
                + "\"spender\" : \"" + spender + "\"}";
        assertEquals(BigDecimal.valueOf(300),
                coinContract.allowance(createParams(allowanceParams)));

        String toParams = "{\"address\" : \"" + to + "\"}";
        assertEquals(BigDecimal.valueOf(998000000700L),
                coinContract.balanceof(createParams(toParams)));

        String fromParams = "{\"address\" : \"" + owner + "\"}";
        assertEquals(BigDecimal.valueOf(999999300),
                coinContract.balanceof(createParams(fromParams)));

        String spenderParams = "{\"address\" : \"" + spender + "\"}";
        assertEquals(BigDecimal.valueOf(1000000000),
                coinContract.balanceof(createParams(spenderParams)));
    }

    private JsonObject createParams(String paramStr) {
        return JsonUtil.parseJsonObject(paramStr);
    }

    public class MetaCoinContract extends CoinContract {
        public TransactionReceipt hello(JsonObject params) {
            TransactionReceipt txReceipt = new TransactionReceiptImpl();
            txReceipt.putLog("hello", params.toString());
            txReceipt.setStatus(ExecuteStatus.SUCCESS);
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