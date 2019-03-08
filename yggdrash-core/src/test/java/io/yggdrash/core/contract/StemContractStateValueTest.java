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

//    @Test
//    public void initTest() {
//        assertThat(stateValue.getFee()).isNull();
//        assertThat(stateValue.get()).isNull();
//        assertThat(stateValue.getContractHistory()).isEmpty();
//
//        stateValue.init();
//
//        assertThat(stateValue.getType()).isEqualTo(Branch.BranchType.TEST);
//        assertThat(stateValue.getTag()).isNotEmpty();
//        // TODO Stem Contract Change all of them
//    }
//
//    @Test
//    public void setFeeTest() {
//        stateValue.setType("private");
//        assertThat(stateValue.getType()).isEqualTo(Branch.BranchType.PRIVATE);
//        assertThat(stateValue.getJson().get("type").getAsString()).isEqualTo("private");
//    }
//
//    @Test
//    public void setBlockHeightTest() {
//        String tag = "0.2";
//        stateValue.setTag(tag);
//        assertThat(stateValue.getTag()).isEqualTo(tag);
//        assertThat(stateValue.getJson().get("tag").getAsString()).isEqualTo(tag);
//    }
}