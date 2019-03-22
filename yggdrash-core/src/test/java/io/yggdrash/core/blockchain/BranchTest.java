package io.yggdrash.core.blockchain;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.TestConstants;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class BranchTest {
    private static final Logger log = LoggerFactory.getLogger(BranchTest.class);

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
        String consensusString = new StringBuilder()
                .append("{\"consensus\": {\n")
                .append("    \"algorithm\": \"pbft\",\n")
                .append("    \"period\": \"* * * * * *\",\n")
                .append("    \"validator\": [\n")
                .append("      \"77283a04b3410fe21ba5ed04c7bd3ba89e70b78c\",\n")
                .append("      \"9911fb4663637706811a53a0e0b4bcedeee38686\",\n")
                .append("      \"2ee2eb80c93d031147c21ba8e2e0f0f4a33f5312\",\n")
                .append("      \"51e2128e8deb622c2ec6dc38f9d895f0be044eb4\",\n")
                .append("      \"047269a50640ed2b0d45d461488c13abad1e0fac\",\n")
                .append("      \"21640f2116389a3e37462fd6b68b969e490b6a50\",\n")
                .append("      \"63fef4912dc8b0781351b18eb9be450638ea2c17\"\n")
                .append("    ]\n}")
                .append("  }").toString();
        JsonObject consensus = new Gson().fromJson(consensusString, JsonObject.class);

        JsonObject branchJson = BranchBuilder.builder()
                .setName(name)
                .setSymbol(symbol)
                .setProperty(property)
                .setDescription(description)
                .setTimeStamp(timestamp)
                .addValidator(TestConstants.wallet().getHexAddress())
                .addConsensus(consensus)
                .buildJson();



        Branch branch = Branch.of(branchJson);
        log.debug(branch.getJson().toString());


        assertThat(branch.getName()).isEqualTo(name);
        assertThat(branch.getSymbol()).isEqualTo(symbol);
        assertThat(branch.getProperty()).isEqualTo(property);
        assertThat(branch.getDescription()).isEqualTo(description);
        assertThat(branch.getTimestamp())
                .isEqualTo(HexUtil.hexStringToLong(timestamp));

        JsonObject toJson = branch.toJsonObject();
        log.debug(toJson.toString());
        Branch fromJson = Branch.of(toJson);
        log.debug(fromJson.getJson().toString());
        log.debug(branch.getJson().toString());
        assertEquals(fromJson.getBranchId(), branch.getBranchId());
    }


    @Test
    public void loadTest() throws IOException {

        File genesisFile = new File(
                getClass().getClassLoader().getResource("./branch-yggdrash.json").getFile());

        String genesisString = FileUtil.readFileToString(genesisFile, StandardCharsets.UTF_8);
        JsonObject branch = new JsonParser().parse(genesisString).getAsJsonObject();
        Branch yggdrashBranch = Branch.of(branch);
        assert "YGGDRASH".equals(yggdrashBranch.getName());

    }


    @Test
    public void generatorGenesisBlock() throws IOException {
        File genesisFile = new File(
                getClass().getClassLoader().getResource("./branch-yggdrash.json").getFile());

        String genesisString = FileUtil.readFileToString(genesisFile, StandardCharsets.UTF_8);
        JsonObject branch = new JsonParser().parse(genesisString).getAsJsonObject();
        Branch yggdrashBranch = Branch.of(branch);

        FileInputStream inputBranch = new FileInputStream(genesisFile);
        GenesisBlock block = GenesisBlock.of(inputBranch);
        assert block.getBlock().getIndex() == 0;
        assert yggdrashBranch.getName().equals(block.getBranch().getName());
        log.debug(JsonUtil.prettyFormat(block.getBlock().toJsonObject()));

        List<TransactionHusk> txs = block.getBlock().getBody();

        // TODO Genesis Block has more by Transaction Type
        assert txs.size() == 1;

    }




}