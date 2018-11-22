package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CoinContractTest {

    private static final Logger log = LoggerFactory.getLogger(CoinContract.class);
    private static final CoinContract coinContract = new CoinContract();


    @Before
    public void setUp() {
        StateStore<CoinStateTable> coinContractStateStore = new StateStore<>();
        coinContract.init(coinContractStateStore, new TransactionReceiptStore());
        genesis();
    }

    public void genesis() {
        String genesisStr = "{\"alloc\": {\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\":"
                + " {\"balance\": \"1000000000\"},\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\":"
                + " {\"balance\": \"1000000000\"},\"cee3d4755e47055b530deeba062c5bd0c17eb00f\":"
                + " {\"balance\": \"998000000000\"}}}";

        TransactionReceipt result = coinContract.genesis(createParam(genesisStr));

        assertEquals(result.getStatus(), 1);
        assertEquals(result.getTxLog().size(), 4);
    }

    @Test
    public void specification() {
        List<String> methods = coinContract.specification(new JsonArray());

        assertTrue(!methods.isEmpty());
        assertEquals(methods.size(), 7);
    }

    @Test
    public void totalSupply() {
        BigDecimal res = coinContract.totalsupply(new JsonArray());
        BigDecimal totalSupply = new BigDecimal("1000000000000");

        assertEquals(totalSupply, res);
    }

    @Test
    public void balanceOf() {
        String paramStr = "{\"address\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\"}";

        BigDecimal res = coinContract.balanceof(createParam(paramStr));
        BigDecimal balanceOfFrontier = new BigDecimal("1000000000");

        assertEquals(balanceOfFrontier, res);
    }

    @Test
    public void allowance() {
        String paramStr = "{\"owner\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\","
                + "\"spender\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\"}";

        BigDecimal res = coinContract.allowance(createParam(paramStr));
        BigDecimal allowedBalance = BigDecimal.ZERO;

        assertEquals(allowedBalance, res);
    }

    @Test
    public void transfer() {
        String paramStr = "{\"to\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\","
                + "\"amount\" : \"10\"}";

        // tx 가 invoke 되지 않아 baseContract 에 sender 가 세팅되지 않아서 설정해줌
        coinContract.sender = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        TransactionReceipt result = coinContract.transfer(createParam(paramStr));

        assertEquals(result.getStatus(), 1);

        String paramStr2 = "{\"address\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\"}";
        String paramStr3 = "{\"address\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\"}";

        assertEquals(coinContract.balanceof(createParam(paramStr2)),
                new BigDecimal("1000000010"));
        assertEquals(coinContract.balanceof(createParam(paramStr3)),
                new BigDecimal("999999990"));
    }

    @Test
    public void approve() {
        String paramStr = "{\"spender\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\","
                + "\"amount\" : \"1000\"}";

        coinContract.sender = "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94";
        TransactionReceipt result = coinContract.approve(createParam(paramStr));

        assertEquals(result.getStatus(), 1);

        String paramStr2 = "{\"address\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\"}";
        String paramStr3 = "{\"address\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\"}";

        assertEquals(coinContract.balanceof(createParam(paramStr2)),
                new BigDecimal("1000000000"));
        assertEquals(coinContract.balanceof(createParam(paramStr3)),
                new BigDecimal("1000000000"));

        String paramStr4 = "{\"owner\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\","
                + "\"spender\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\"}";

        assertEquals(coinContract.allowance(createParam(paramStr4)),
                new BigDecimal("1000"));
    }

    @Test
    public void transferFrom() {
        approve();

        String paramStr = "{\"from\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\","
                + "\"to\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\",\"amount\" : \"500\"}";
        TransactionReceipt result = coinContract.transferfrom(createParam(paramStr));

        assertEquals(result.getStatus(), 1);

        String paramStr2 = "{\"address\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\"}";
        String paramStr3 = "{\"address\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\"}";

        assertEquals(coinContract.balanceof(createParam(paramStr2)),
                new BigDecimal("1000000500"));
        assertEquals(coinContract.balanceof(createParam(paramStr3)),
                new BigDecimal("999999500"));

        String paramStr4 = "{\"from\" : \"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\","
                + "\"to\" : \"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\",\"amount\" : \"600\"}";
        TransactionReceipt result2 = coinContract.transferfrom(createParam(paramStr4));

        // tx 실행은 성공하지만 allowance 보다 많은 amount 전송은 실패 (이체 불가)
        assertEquals(result2.getStatus(), 1);

        assertEquals(coinContract.balanceof(createParam(paramStr2)),
                new BigDecimal("1000000500"));
        assertEquals(coinContract.balanceof(createParam(paramStr3)),
                new BigDecimal("999999500"));
    }

    private JsonArray createParam(String paramStr) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(paramStr);
        JsonArray param = new JsonArray();
        param.add(jsonObject);

        return param;
    }
}