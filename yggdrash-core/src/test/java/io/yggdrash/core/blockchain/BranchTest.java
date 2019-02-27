package io.yggdrash.core.blockchain;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.TestConstants;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.util.FileUtil;
import io.yggdrash.common.util.JsonUtil;
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
        String consensusString = "{\"consensus\": {\n" +
                "    \"algorithm\": \"pbft\",\n" +
                "    \"period\": \"2\",\n" +
                "    \"validator\": {\n" +
                "      \"527e5997e79cc0935d9d86a444380a11cdc296b6bcce2c6df5e5439a3cd7bffb945e77aacf881f36a668284984b628063f5d18a214002ac7ad308e04b67bcad8\": {\n" +
                "        \"host\": \"127.0.0.1\",\n" +
                "        \"port\": \"32911\"\n" +
                "      },\n" +
                "      \"e12133df65a2e7dec4310f3511b1fa6b35599770e900ffb50f795f2a49d0a22b63e013a393affe971ea4db08cc491118a8a93719c3c1f55f2a12af21886d294d\": {\n" +
                "        \"host\": \"127.0.0.1\",\n" +
                "        \"port\": \"32912\"\n" +
                "      },\n" +
                "      \"8d69860332aa6202df489581fd618fc085a6a5af89964d9e556a398d232816c9618fe15e90015d0a2d15037c91587b79465106f145c0f4db6d18b105659d2bc8\": {\n" +
                "        \"host\": \"127.0.0.1\",\n" +
                "        \"port\": \"32913\"\n" +
                "      },\n" +
                "      \"b49fbee055a4b3bd2123a60b24f29d69bc0947e45a75eb4880fe9c5b07904c650729e5edcdaff2523c8839889925079963186bd38c22c96433bdbf4465960527\": {\n" +
                "        \"host\": \"127.0.0.1\",\n" +
                "        \"port\": \"32914\"\n" +
                "      }\n" +
                "    }\n" +
                "  }}";
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