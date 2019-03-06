package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import org.junit.Before;

public class StemContractStateValueTest {

    private StemContractStateValue stateValue;

    @Before
    public void setUp() {
        JsonObject json = ContractTestUtils.createSampleBranchJson();
        this.stateValue = StemContractStateValue.of(json);
    }

}