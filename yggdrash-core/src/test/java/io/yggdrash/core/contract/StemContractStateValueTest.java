package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.core.blockchain.Branch;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StemContractStateValueTest {

    private StemContractStateValue stateValue;

    @Before
    public void setUp() {
        JsonObject json = TestUtils.createSampleBranchJson();
        this.stateValue = StemContractStateValue.of(json);
    }

    @Test
    public void initTest() {
        assertThat(stateValue.getType()).isNull();
        assertThat(stateValue.getTag()).isNull();
        assertThat(stateValue.getContractHistory()).isEmpty();

        stateValue.init();

        assertThat(stateValue.getType()).isEqualTo(Branch.BranchType.TEST);
        assertThat(stateValue.getTag()).isNotEmpty();
        assertThat(stateValue.getContractHistory()).contains(stateValue.getContractId());
    }

    @Test
    public void setTypeTest() {
        stateValue.setType("private");
        assertThat(stateValue.getType()).isEqualTo(Branch.BranchType.PRIVATE);
        assertThat(stateValue.getJson().get("type").getAsString()).isEqualTo("private");
    }

    @Test(expected = IllegalStateException.class)
    public void inValidTypeTest() {
        JsonObject json = TestUtils.createSampleBranchJson();
        StemContractStateValue branch = StemContractStateValue.of(json);
        branch.setType("unknown");
    }

    @Test
    public void setTagTest() {
        String tag = "0.2";
        stateValue.setTag(tag);
        assertThat(stateValue.getTag()).isEqualTo(tag);
        assertThat(stateValue.getJson().get("tag").getAsString()).isEqualTo(tag);
    }

    @Test
    public void setDescriptionTest() {
        String desc = "test description";
        stateValue.setDescription(desc);
        assertThat(stateValue.getDescription()).isEqualTo(desc);
        assertThat(stateValue.getJson().get("description").getAsString()).isEqualTo(desc);
    }

    @Test
    public void updateContractIdTest() {
        stateValue.init();
        assertThat(stateValue.getContractHistory()).containsOnly(stateValue.getContractId());
        assertContractAndHistory(stateValue.getContractId(), 0);

        // update exist contractId
        stateValue.updateContract(stateValue.getContractId().toString());
        assertThat(stateValue.getContractHistory()).containsOnly(stateValue.getContractId());

        ContractId newContractId = ContractId.of("00");
        stateValue.updateContract(newContractId.toString());
        assertContractAndHistory(newContractId, 1);
    }

    private void assertContractAndHistory(ContractId contractId, int contractHistoryIndex) {
        assertThat(stateValue.getContractId()).isEqualTo(contractId);
        assertThat(stateValue.getJson().get("contractId").getAsString())
                .isEqualTo(contractId.toString());

        assertThat(stateValue.getContractHistory()).contains(contractId);
        JsonArray contractHistory = stateValue.getJson().getAsJsonArray("contractHistory");
        assertThat(contractHistory.get(contractHistoryIndex).getAsString())
                .isEqualTo(contractId.toString());
    }
}