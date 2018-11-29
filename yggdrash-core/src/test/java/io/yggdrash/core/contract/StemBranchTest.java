package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.core.blockchain.Branch;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StemBranchTest {

    private StemBranch branch;

    @Before
    public void setUp() {
        JsonObject json = TestUtils.createSampleBranchJson();
        this.branch = StemBranch.of(json);
    }

    @Test
    public void initTest() {
        assertThat(branch.getType()).isNull();
        assertThat(branch.getTag()).isNull();
        assertThat(branch.getContractHistory()).isEmpty();

        branch.init();

        assertThat(branch.getType()).isEqualTo(Branch.BranchType.TEST);
        assertThat(branch.getTag()).isNotEmpty();
        assertThat(branch.getContractHistory()).contains(branch.getContractId());
    }

    @Test
    public void setTypeTest() {
        branch.setType("private");
        assertThat(branch.getType()).isEqualTo(Branch.BranchType.PRIVATE);
        assertThat(branch.getJson().get("type").getAsString()).isEqualTo("private");
    }

    @Test(expected = IllegalStateException.class)
    public void inValidTypeTest() {
        JsonObject json = TestUtils.createSampleBranchJson();
        StemBranch branch = StemBranch.of(json);
        branch.setType("unknown");
    }

    @Test
    public void setTagTest() {
        String tag = "0.2";
        branch.setTag(tag);
        assertThat(branch.getTag()).isEqualTo(tag);
        assertThat(branch.getJson().get("tag").getAsString()).isEqualTo(tag);
    }

    @Test
    public void setDescriptionTest() {
        String desc = "test description";
        branch.setDescription(desc);
        assertThat(branch.getDescription()).isEqualTo(desc);
        assertThat(branch.getJson().get("description").getAsString()).isEqualTo(desc);
    }

    @Test
    public void updateContractIdTest() {
        branch.init();
        assertThat(branch.getContractHistory()).containsOnly(branch.getContractId());
        assertContractAndHistory(branch.getContractId(), 0);

        // update exist contractId
        branch.updateContract(branch.getContractId().toString());
        assertThat(branch.getContractHistory()).containsOnly(branch.getContractId());

        ContractId newContractId = ContractId.of("00");
        branch.updateContract(newContractId.toString());
        assertContractAndHistory(newContractId, 1);
    }

    private void assertContractAndHistory(ContractId contractId, int contractHistoryIndex) {
        assertThat(branch.getContractId()).isEqualTo(contractId);
        assertThat(branch.getJson().get("contractId").getAsString())
                .isEqualTo(contractId.toString());

        assertThat(branch.getContractHistory()).contains(contractId);
        JsonArray contractHistory = branch.getJson().getAsJsonArray("contractHistory");
        assertThat(contractHistory.get(contractHistoryIndex).getAsString())
                .isEqualTo(contractId.toString());
    }
}