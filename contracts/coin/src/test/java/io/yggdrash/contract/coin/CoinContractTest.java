package io.yggdrash.contract.coin;

import com.google.gson.JsonObject;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.utils.ContractUtils;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.ReceiptImpl;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.exception.ContractException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CoinContractTest {
    private static final CoinContract.CoinService coinContract = new CoinContract.CoinService();
    private static final Logger log = LoggerFactory.getLogger(CoinContractTest.class);

    private static final String ADDRESS_1 = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
    private static final String ADDRESS_2 = "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e";
    private static final String ADDRESS_FORMAT = "{\"address\" : \"%s\"}";
    private static final String ADDRESS_JSON_1 = String.format(ADDRESS_FORMAT, ADDRESS_1);
    private static final String ADDRESS_JSON_2 = String.format(ADDRESS_FORMAT, ADDRESS_2);
    private static final String INVALID_PARAMS = "Error Code:34001, Msg:Params not allowed";
    private static final String INSUFFICIENT_FUNDS = "Insufficient funds";
    private Field txReceiptField;

    @Before
    public void setUp() throws IllegalAccessException {
        StateStore coinContractStateStore = new StateStore(new HashMapDbSource());

        List<Field> txReceipt = ContractUtils.txReceiptFields(coinContract);
        if (txReceipt.size() == 1) {
            txReceiptField = txReceipt.get(0);
        }
        for (Field f : ContractUtils.contractFields(coinContract, ContractStateStore.class)) {
            f.setAccessible(true);
            f.set(coinContract, coinContractStateStore);
        }

        genesis();
    }

    private void genesis() {
        String genesisStr = "{\"alloc\": {\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\":"
                + " {\"balance\": \"1000000000\"},\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\":"
                + " {\"balance\": \"1000000000\"},\"cee3d4755e47055b530deeba062c5bd0c17eb00f\":"
                + " {\"balance\": \"998000000000\"}}}";

        Receipt result = new ReceiptImpl();

        try {
            txReceiptField.set(coinContract, result);
            coinContract.init(createParams(genesisStr));
        } catch (IllegalAccessException e) {
            log.warn(e.getMessage());
        } catch (ContractException e) {
            log.warn(e.getMessage());
            result.addLog(e.getMessage());
        }

        assertTrue(result.isSuccess());
        assertEquals(4, result.getLog().size());
    }

    @Test
    public void totalSupply() {
        BigInteger res = coinContract.totalSupply();

        assertEquals(BigInteger.valueOf(1000000000000L), res);
    }

    @Test
    public void balanceOf() {
        BigInteger res = coinContract.balanceOf(createParams(ADDRESS_JSON_1));

        assertEquals(BigInteger.valueOf(1000000000), res);
    }

    @Test
    public void allowance() {
        String paramStr = "{\"owner\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\","
                + "\"spender\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\"}";

        BigInteger res = coinContract.allowance(createParams(paramStr));

        assertEquals(BigInteger.ZERO, res);
    }

    @Test
    public void transferExceptionTest() { // Handling ParamException & BalanceException
        String invalidParamStr = "{\"hello\" : \"yggdrash\",\"this\" : \"is for test\"}";

        Receipt result = new ReceiptImpl();
        result.setIssuer(ADDRESS_1); // Sender is not set in baseContract because tx is not invoked yet

        try {
            txReceiptField.set(coinContract, result);
            result = coinContract.transfer(createParams(invalidParamStr));
        } catch (IllegalAccessException e) {
            log.warn("Set receipt failed : {}", e.getMessage()); // Setting receipt to coinContract failed
        } catch (ContractException e) {
            log.debug("ContractException : {}", e.getMessage());
            result.addLog(e.getMessage());
        }

        assertTrue(result.getLog().contains(INVALID_PARAMS));
        assertFalse(result.isSuccess()); // No change account balance
        assertEquals(BigInteger.valueOf(1000000000), coinContract.balanceOf(createParams(ADDRESS_JSON_1)));
        assertEquals(BigInteger.valueOf(1000000000), coinContract.balanceOf(createParams(ADDRESS_JSON_2)));

        invalidParamStr
                = String.format("{\"to\" : \"%s\",\"amount\" : \"10000000000000000000\"}", ADDRESS_2);

        try {
            result = coinContract.transfer(createParams(invalidParamStr));
        } catch (ContractException e) {
            log.debug("ContractException : {}", e.getMessage());
            result.addLog(e.getMessage());
        }

        assertTrue(result.getLog().contains(INSUFFICIENT_FUNDS));
        assertFalse(result.isSuccess()); // No change account balance
        assertEquals(BigInteger.valueOf(1000000000), coinContract.balanceOf(createParams(ADDRESS_JSON_1)));
        assertEquals(BigInteger.valueOf(1000000000), coinContract.balanceOf(createParams(ADDRESS_JSON_2)));

        log.debug("{}:{}", ADDRESS_1, coinContract.balanceOf(createParams(ADDRESS_JSON_1)));
        log.debug("{}:{}", ADDRESS_1, coinContract.balanceOf(createParams(ADDRESS_JSON_2)));

        String validParamStr = String.format("{\"to\" : \"%s\",\"amount\" : \"10\"}", ADDRESS_2);
        JsonObject validParamObj = createParams(validParamStr);

        try {
            result = coinContract.transfer(validParamObj);
        } catch (ContractException e) {
            log.debug("ContractException : {}", e.getMessage());
            result.addLog(e.getMessage());
        }

        assertTrue(result.isSuccess());
        assertEquals(BigInteger.valueOf(999999990), coinContract.balanceOf(createParams(ADDRESS_JSON_1)));
        assertEquals(BigInteger.valueOf(1000000010), coinContract.balanceOf(createParams(ADDRESS_JSON_2)));

        // Same amount
        addAmount(validParamObj, BigInteger.valueOf(999999990));
        try {
            result = coinContract.transfer(validParamObj);
        } catch (ContractException e) {
            log.debug("ContractException : {}", e.getMessage());
            result.addLog(e.getMessage());
        }
        assertTrue(result.isSuccess());
    }

    @Test
    public void approveTest() {
        String invalidParamStr = "{\"hello\" : \"yggdrash\",\"this\" : \"is for test\"}";

        Receipt result = new ReceiptImpl();
        result.setIssuer(ADDRESS_1); // Sender is not set in baseContract because tx is not invoked yet

        try {
            txReceiptField.set(coinContract, result);
            result = coinContract.approve(createParams(invalidParamStr));
        } catch (IllegalAccessException e) {
            log.warn("Set receipt failed : {}", e.getMessage()); // Setting receipt to coinContract failed
        } catch (ContractException e) {
            log.debug("ContractException : {}", e.getMessage());
            result.addLog(e.getMessage());
        }

        assertTrue(result.getLog().contains(INVALID_PARAMS));
        assertFalse(result.isSuccess());

        invalidParamStr
                = String.format("{\"spender\" : \"%s\",\"amount\" : \"10000000000000000000000000000000\"}", ADDRESS_2);

        try {
            result = coinContract.approve(createParams(invalidParamStr));
        } catch (ContractException e) {
            log.debug("ContractException : {}", e.getMessage());
            result.addLog(e.getMessage());
        }

        assertTrue(result.getLog().contains(INSUFFICIENT_FUNDS));
        assertFalse(result.isSuccess());

        String validParams = String.format("{\"spender\" : \"%s\",\"amount\" : \"1000\"}", ADDRESS_2);

        try {
            result = coinContract.approve(createParams(validParams));
        } catch (ContractException e) {
            log.debug("ContractException : {}", e.getMessage());
            result.addLog(e.getMessage());
        }

        assertTrue(result.isSuccess());
        assertEquals(BigInteger.valueOf(1000000000), coinContract.balanceOf(createParams(ADDRESS_JSON_1)));
        assertEquals(BigInteger.valueOf(1000000000), coinContract.balanceOf(createParams(ADDRESS_JSON_2)));
    }

    @Test
    public void transferFromTest() {
        String sender = ADDRESS_1;
        String spender = ADDRESS_2;
        String to = "cee3d4755e47055b530deeba062c5bd0c17eb00f";

        approveBySender(spender, sender, "1000");
        assertTransferFrom(to, sender, spender);

        String invalidParamStr = "{\"hello\" : \"yggdrash\",\"this\" : \"is for test\"}";

        Receipt result = new ReceiptImpl();
        result.setIssuer(spender);

        try {
            txReceiptField.set(coinContract, result);
            result = coinContract.transferFrom(createParams(invalidParamStr));
        } catch (IllegalAccessException e) {
            log.warn("Set receipt failed : {}", e.getMessage()); // Setting receipt to coinContract failed
        } catch (ContractException e) {
            log.debug("ContractException : {}", e.getMessage());
            result.addLog(e.getMessage());
        }

        assertTrue(result.getLog().contains(INVALID_PARAMS));
        assertFalse(result.isSuccess());

        invalidParamStr = String.format("{\"from\" : \"%s\",\"to\" : \"%s\",\"amount\" : \"1000000000\"}", sender, to);

        try {
            result = coinContract.transferFrom(createParams(invalidParamStr));
        } catch (ContractException e) {
            log.debug("ContractException : {}", e.getMessage());
            result.addLog(e.getMessage());
        }

        assertTrue(result.getLog().contains(INSUFFICIENT_FUNDS));
        assertFalse(result.isSuccess());

        String validParamStr = String.format("{\"from\" : \"%s\",\"to\" : \"%s\",\"amount\" : \"700\"}", sender, to);

        try {
            result = coinContract.transferFrom(createParams(validParamStr));
        } catch (ContractException e) {
            log.debug("ContractException : {}", e.getMessage());
            result.addLog(e.getMessage());
        }
        assertTrue(result.isSuccess());
        assertEquals(BigInteger.valueOf(998000000700L), getBalance(to));
        assertEquals(BigInteger.valueOf(999999300), getBalance(sender));
        assertEquals(BigInteger.valueOf(1000000000), getBalance(spender));
        assertEquals(BigInteger.valueOf(300), getAllowance(sender, spender));

        validParamStr = String.format(
                "{\"from\" : \"%s\",\"to\" : \"%s\",\"amount\" : \"%d\"}", sender, to, getAllowance(sender, spender));

        try {
            result = coinContract.transferFrom(createParams(validParamStr));
        } catch (ContractException e) {
            log.debug("ContractException : {}", e.getMessage());
            result.addLog(e.getMessage());
        }

        assertTrue(result.isSuccess());
        assertEquals(BigInteger.ZERO, getAllowance(sender, spender));
    }

    @Test
    public void metaCoinTest() {
        MetaCoinContract metaCoinContract = new MetaCoinContract();
        assertTrue(metaCoinContract.hello(new JsonObject()).isSuccess());
    }

    private void approveBySender(String spender, String sender, String amount) {
        String params = String.format("{\"spender\" : \"%s\",\"amount\" : \"%s\"}", ADDRESS_2, amount);

        Receipt result = new ReceiptImpl();
        result.setIssuer(sender);

        try {
            txReceiptField.set(coinContract, result);
            result = coinContract.approve(createParams(params));
        } catch (IllegalAccessException e) {
            log.warn("Set receipt failed : {}", e.getMessage()); // Setting receipt to coinContract failed
        } catch (ContractException e) {
            log.debug("ContractException : {}", e.getMessage());
            result.addLog(e.getMessage());
        }

        assertTrue(result.isSuccess());
        String spenderParams = String.format(ADDRESS_FORMAT, spender);
        String senderParams = String.format(ADDRESS_FORMAT, sender);

        assertEquals(BigInteger.valueOf(1000000000),
                coinContract.balanceOf(createParams(spenderParams)));
        assertEquals(BigInteger.valueOf(1000000000),
                coinContract.balanceOf(createParams(senderParams)));
    }

    private void assertTransferFrom(String to, String sender, String spender) {

        String allowanceParams = "{\"owner\" : \"" + sender + "\", \"spender\" : \"" + spender + "\"}";
        assertEquals(BigInteger.valueOf(1000), coinContract.allowance(createParams(allowanceParams)));

        String toParams = String.format(ADDRESS_FORMAT, to);
        assertEquals(BigInteger.valueOf(998000000000L), coinContract.balanceOf(createParams(toParams)));

        String fromParams = String.format(ADDRESS_FORMAT, sender);
        assertEquals(BigInteger.valueOf(1000000000), coinContract.balanceOf(createParams(fromParams)));

        String spenderParams = String.format(ADDRESS_FORMAT, spender);
        assertEquals(BigInteger.valueOf(1000000000), coinContract.balanceOf(createParams(spenderParams)));
    }

    private void addAmount(JsonObject param, BigInteger amount) {
        param.addProperty("amount", amount);
    }

    private JsonObject createParams(String paramStr) {
        return JsonUtil.parseJsonObject(paramStr);
    }

    private class MetaCoinContract extends CoinContract {
        Receipt hello(JsonObject params) {
            Receipt txReceipt = new ReceiptImpl();
            txReceipt.addLog(params.toString());
            txReceipt.setStatus(ExecuteStatus.SUCCESS);
            log.info("{}", txReceipt);
            return txReceipt;
        }
    }

    private BigInteger getBalance(String address) {
        JsonObject obj = new JsonObject();
        obj.addProperty("address", address);
        return coinContract.balanceOf(obj);
    }

    private BigInteger getAllowance(String owner, String spender) {
        JsonObject obj = new JsonObject();
        obj.addProperty("owner", owner);
        obj.addProperty("spender", spender);
        return coinContract.allowance(obj);
    }
}