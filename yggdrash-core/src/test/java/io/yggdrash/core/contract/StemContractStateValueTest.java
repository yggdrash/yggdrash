package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StemContractStateValueTest {

    private StemContractStateValue stateValue;

    @Before
    public void setUp() {
        JsonObject json = ContractTestUtils.createSampleBranchJson();
        this.stateValue = StemContractStateValue.of(json);
    }

    @Test
    public void initTest() {
        assertThat(stateValue.getContractHistory()).isEmpty();

        // TODO Stem Contract Change all of them
    }

    @Test
    public void setTypeTest() {
        assertThat(stateValue.getJson().get("type").getAsString()).isEqualTo("private");
    }

    @Test(expected = IllegalStateException.class)
    public void inValidTypeTest() {
        JsonObject json = ContractTestUtils.createSampleBranchJson();
        StemContractStateValue branch = StemContractStateValue.of(json);
    }

    private void assertContractAndHistory(ContractVersion contractVersion, int contractHistoryIndex) {
        assertThat(stateValue.getJson().get("contractVersion").getAsString())
                .isEqualTo(contractVersion.toString());

        assertThat(stateValue.getContractHistory()).contains(contractVersion);
        JsonArray contractHistory = stateValue.getJson().getAsJsonArray("contractHistory");
        assertThat(contractHistory.get(contractHistoryIndex).getAsString())
                .isEqualTo(contractVersion.toString());
    }
}