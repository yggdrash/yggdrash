package io.yggdrash.core.genesis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.husk.BlockHusk;
import io.yggdrash.util.FileUtil;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class GenesisBlock {

    private final DefaultConfig defaultConfig = new DefaultConfig();
    private BlockHusk genesisBlock;

    public GenesisBlock() throws IOException, InvalidCipherTextException {
        Wallet wallet = new Wallet(defaultConfig);

        String transactionFileName = defaultConfig.getConfig().getString("genesis.config");
        JsonObject genesisObject = getJsonObjectFromFile(transactionFileName);

        String frontierFileName = defaultConfig.getConfig().getString("genesis.frontier");
        JsonObject frontierObject = getJsonObjectFromFile(frontierFileName);
        genesisObject.add("frontier", frontierObject.get("frontier"));

        String nodeListFileName = defaultConfig.getConfig().getString("genesis.nodeList");
        JsonObject nodeListObject = getJsonObjectFromFile(nodeListFileName);
        genesisObject.add("nodeList", nodeListObject.get("nodeList"));

        genesisBlock = BlockHusk.genesis(wallet, genesisObject);
    }

    private JsonObject getJsonObjectFromFile(String fileName) throws IOException {
        StringBuilder result = new StringBuilder();
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());

        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            result.append(line).append("\n");
        }

        scanner.close();

        return new Gson().fromJson(result.toString(), JsonObject.class);
    }

    public BlockHusk getGenesisBlock() {
        return genesisBlock;
    }

    public void generateGenesisBlockFile() throws IOException {
        //todo: change the method to serializing method

        JsonObject jsonObject = this.genesisBlock.toJsonObject();

        ClassLoader classLoader = getClass().getClassLoader();
        File genesisFile = new File(classLoader.getResource(
                defaultConfig.getConfig().getString("genesis.block")).getFile());

        FileUtil.writeStringToFile(genesisFile,
                new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject));
    }

}
