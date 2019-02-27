package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import org.junit.Before;

import static org.assertj.core.api.Assertions.assertThat;

public class StemContractStateValueTest {

    private StemContractStateValue stateValue;

    @Before
    public void setUp() {
        JsonObject json = ContractTestUtils.createSampleBranchJson();
        this.stateValue = StemContractStateValue.of(json);
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