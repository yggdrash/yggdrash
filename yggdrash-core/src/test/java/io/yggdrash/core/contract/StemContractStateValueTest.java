package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class StemContractStateValueTest {

    private StemContractStateValue stateValue;

    @Before
    public void setUp() {
        JsonObject json = ContractTestUtils.createSampleBranchJson();
        this.stateValue = StemContractStateValue.of(json);
    }

    @Test
    public void setFeeTest() {
        stateValue.setFee(BigDecimal.valueOf(1000));;
        assertEquals(BigDecimal.valueOf(1000), stateValue.getFee());
    }

    @Test
    public void setBlockHeightTest() {
        Long height = 3L;
        stateValue.setBlockHeight(height);
        assertEquals(height, stateValue.getBlockHeight());

        Long b = stateValue.getJson().get("blockHeight").getAsLong();
        assertEquals(b, stateValue.getBlockHeight());
    }
}