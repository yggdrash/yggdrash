package io.yggdrash.core.blockchain;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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
        String timestamp = "00000166c837f0c9";
        String consensusString = new StringBuilder()
                .append("{\"consensus\": {\n")
                .append("    \"algorithm\": \"pbft\",\n")
                .append("    \"period\": \"* * * * * *\"\n")
                .append("   \n}")
                .append("  }").toString();
        JsonObject consensus = new Gson().fromJson(consensusString, JsonObject.class);

        JsonObject branchJson = BranchBuilder.builder()
                .setName(name)
                .setSymbol(symbol)
                .setProperty(property)
                .setDescription(description)
                .setTimeStamp(timestamp)
                .setConsensus(consensus)
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
        String genesisString = FileUtil.readFileToString(TestConstants.branchFile, FileUtil.DEFAULT_CHARSET);
        JsonObject branch = new JsonParser().parse(genesisString).getAsJsonObject();
        Branch yggdrashBranch = Branch.of(branch);
        Assert.assertEquals("YGGDRASH", yggdrashBranch.getName());
        log.info("YGGDRASH BRANCH ID : {} ", yggdrashBranch.getBranchId().toString());
    }

    @Test
    public void generatorGenesisBlock() throws IOException {

        String genesisString = FileUtil.readFileToString(TestConstants.branchFile, FileUtil.DEFAULT_CHARSET);
        JsonObject branch = new JsonParser().parse(genesisString).getAsJsonObject();
        Branch yggdrashBranch = Branch.of(branch);
        GenesisBlock genesisBlock = BlockChainTestUtils.generateGenesisBlockByFile(TestConstants.branchFile);

        Assert.assertEquals(0, genesisBlock.getBlock().getIndex());
        Assert.assertEquals(yggdrashBranch.getName(), genesisBlock.getBranch().getName());
        log.debug(JsonUtil.prettyFormat(genesisBlock.getBlock().toJsonObject()));

        int genesisTxSize = yggdrashBranch.getBranchContracts().size();
        Assert.assertEquals(genesisTxSize, genesisBlock.getBlock().getBody().getCount());
    }
}