package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CoinContractTest {

    private static final CoinContract coinContract = new CoinContract();

    @Before
    public void setUp() {
        StateStore<CoinContractStateValue> coinContractStateStore = new StateStore<>();
        coinContract.init(coinContractStateStore, new TransactionReceiptStore());
        genesis();
    }

    public void genesis() {
        String genesisStr = "{\"alloc\": {\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\":"
                + " {\"balance\": \"1000000000\"},\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\":"
                + " {\"balance\": \"1000000000\"},\"cee3d4755e47055b530deeba062c5bd0c17eb00f\":"
                + " {\"balance\": \"998000000000\"}}}";

        TransactionReceipt result = coinContract.genesis(createParams(genesisStr));

        assertTrue(result.isSuccess());
        assertEquals(4, result.getTxLog().size());
    }

    @Test
    public void specification() {
        StateStore<CoinContractStateValue> coinContractStateStore = new StateStore<>();
        MetaCoinContract metaCoinContract = new MetaCoinContract();
        metaCoinContract.init(coinContractStateStore, new TransactionReceiptStore());

        List<String> methods = metaCoinContract.specification();

        assertFalse(methods.isEmpty());
        assertEquals(8, methods.size());

        methods = coinContract.specification();

        assertFalse(methods.isEmpty());
        assertEquals(7, methods.size());
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
        String paramStr = "{\"to\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\","
                + "\"amount\" : \"10\"}";

        // tx 가 invoke 되지 않아 baseContract 에 sender 가 세팅되지 않아서 설정해줌
        coinContract.sender = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        TransactionReceipt result = coinContract.transfer(createParams(paramStr));

        assertTrue(result.isSuccess());

        String paramStr2 = "{\"address\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\"}";
        String paramStr3 = "{\"address\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\"}";

        assertEquals(BigDecimal.valueOf(1000000010),
                coinContract.balanceof(createParams(paramStr2)));
        assertEquals(BigDecimal.valueOf(999999990),
                coinContract.balanceof(createParams(paramStr3)));
    }

    @Test
    public void transferFrom() {
        String owner = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        String spender = "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e";
        approveByOwner(owner, spender, "1000");

        String to = "cee3d4755e47055b530deeba062c5bd0c17eb00f";
        String transferParams = "{\"from\" : \"" + owner + "\","
                + "\"to\" : \"" + to + "\",\"amount\" : \"700\"}";

        coinContract.sender = spender;
        TransactionReceipt result = coinContract.transferfrom(createParams(transferParams));
        assertTrue(result.isSuccess());
        assertTransferFrom(to, owner, spender);

        TransactionReceipt result2 = coinContract.transferfrom(createParams(transferParams));
        // not enough amount allowed
        assertFalse(result2.isSuccess());
        assertTransferFrom(to, owner, spender);
    }

    private void approveByOwner(String owner, String spender, String amount) {
        String approveParams = "{\"spender\" : \"" + spender + "\","
                + "\"amount\" : \"" + amount + "\"}";

        coinContract.sender = owner;
        TransactionReceipt result = coinContract.approve(createParams(approveParams));

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
        return Utils.parseJsonObject(paramStr);
    }

    public class MetaCoinContract extends CoinContract {
        public TransactionReceipt hello(JsonObject params) {
            TransactionReceipt txReceipt = new TransactionReceipt();
            txReceipt.putLog("hello", params.toString());
            txReceipt.setStatus(TransactionReceipt.SUCCESS);
            log.info(txReceipt.toString());
            return txReceipt;
        }
    }
}