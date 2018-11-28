package io.yggdrash.core.blockchain;

import com.google.gson.JsonObject;
import io.yggdrash.TestUtils;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.core.exception.InvalidSignatureException;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchTest {

    @Test
    public void defaultTest() {
        String name = "STEM";
        String symbol = "STEM";
        String property = "ecosystem";
        String type = "immunity";
        String description =
                "The Basis of the YGGDRASH Ecosystem. "
                        + "It is also an aggregate and a blockchain containing information "
                        + "of all Branch Chains.";
        String contractId = "d399cd6d34288d04ba9e68ddfda9f5fe99dd778e";
        String timestamp = "00000166c837f0c9";
        JsonObject json = TestUtils.createBranch(name, symbol, property, type, description,
                contractId, timestamp);

        Branch branch = Branch.of(json);

        assertThat(branch.getName()).isEqualTo(name);
        assertThat(branch.getSymbol()).isEqualTo(symbol);
        assertThat(branch.getProperty()).isEqualTo(property);
        assertThat(branch.getType()).isEqualTo(Branch.BranchType.IMMUNITY);
        assertThat(branch.getDescription()).isEqualTo(description);
        assertThat(branch.getContractId().toString()).isEqualTo(contractId);
        assertThat(branch.getTimestamp())
                .isEqualTo(ByteUtil.byteArrayToLong(Hex.decode(timestamp)));
        assertThat(branch.getOwner().toString()).isEqualTo(TestUtils.wallet().getHexAddress());
        assertThat(branch.verify()).isTrue();
    }

    @Test
    public void notVerifiedTest() {
        JsonObject json = TestUtils.getSampleBranch();
        String invalidSig = "1cb1f610ecfb3a8a49988a17ac1a4cd846dece0bcf797c701a8049d16b88faff9d63df"
                + "601000a2f2afebb5a19c058bf388c6b7506763badb68cbedbd85732bd9a0";
        json.addProperty("signature", invalidSig);
        Branch branch = Branch.of(json);
        assertThat(branch.verify()).isFalse();
    }

    @Test(expected = InvalidSignatureException.class)
    public void inValidSignatureTest() {
        JsonObject json = TestUtils.getSampleBranch();
        json.addProperty("signature", "00");
        Branch.of(json).verify();
    }

    @Test
    public void defaultTypeTest() {
        JsonObject json = TestUtils.getSampleBranch();
        Branch branch = Branch.of(json);
        assertThat(branch.getType()).isEqualTo(Branch.BranchType.IMMUNITY);

        json.remove("type");
        branch = Branch.of(json);
        assertThat(branch.getType()).isEqualTo(Branch.BranchType.TEST);

        json.addProperty("type", "");
        branch = Branch.of(json);
        assertThat(branch.getType()).isEqualTo(Branch.BranchType.TEST);
    }

    @Test(expected = IllegalStateException.class)
    public void inValidTypeTest() {
        JsonObject json = TestUtils.getSampleBranch();
        json.addProperty("type", "unknown");
        Branch.of(json);
    }

}