package io.yggdrash.core.blockchain;

import com.google.gson.JsonObject;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.core.exception.InvalidSignatureException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchTest {

    @Test
    public void defaultTest() {
        String name = "STEM";
        String symbol = "STEM";
        String property = "ecosystem";
        String description =
                "The Basis of the YGGDRASH Ecosystem. "
                        + "It is also an aggregate and a blockchain containing information "
                        + "of all Branch Chains.";
        String contractId = "d399cd6d34288d04ba9e68ddfda9f5fe99dd778e";
        String timestamp = "00000166c837f0c9";
        JsonObject genesis = new JsonObject();
        JsonObject json = ContractTestUtils.createBranchJson(name, symbol, property,
                description, contractId, timestamp, genesis);

        Branch branch = Branch.of(json);

        assertThat(branch.getName()).isEqualTo(name);
        assertThat(branch.getSymbol()).isEqualTo(symbol);
        assertThat(branch.getProperty()).isEqualTo(property);
        assertThat(branch.getDescription()).isEqualTo(description);
        assertThat(branch.getContractId().toString()).isEqualTo(contractId);
        assertThat(branch.getTimestamp())
                .isEqualTo(HexUtil.hexStringToLong(timestamp));
        assertThat(branch.getOwner().toString()).isEqualTo(TestConstants.wallet().getHexAddress());
        assertThat(branch.verify()).isTrue();
    }

    @Test
    public void notVerifiedTest() {
        JsonObject json = ContractTestUtils.createSampleBranchJson();
        String invalidSig = "1cb1f610ecfb3a8a49988a17ac1a4cd846dece0bcf797c701a8049d16b88faff9d63df"
                + "601000a2f2afebb5a19c058bf388c6b7506763badb68cbedbd85732bd9a0";
        json.addProperty("signature", invalidSig);
        Branch branch = Branch.of(json);
        assertThat(branch.verify()).isFalse();
    }

    @Test(expected = InvalidSignatureException.class)
    public void inValidSignatureTest() {
        JsonObject json = ContractTestUtils.createSampleBranchJson();
        json.addProperty("signature", "00");
        Branch.of(json).verify();
    }

}