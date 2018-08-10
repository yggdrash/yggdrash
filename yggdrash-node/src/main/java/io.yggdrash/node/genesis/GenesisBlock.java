package io.yggdrash.node.genesis;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.oracle.javafx.jmx.json.JSONException;
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.Wallet;
import io.yggdrash.util.FileUtil;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GenesisBlock {

    private final DefaultConfig defaultConfig = new DefaultConfig();
    private Block genesisBlock;

    public GenesisBlock() throws IOException, InvalidCipherTextException {
        Wallet wallet = new Wallet();

        String transactionFileName = defaultConfig.getConfig().getString("genesis.config");
        JsonObject genesisObject = getJsonObjectFromFile(transactionFileName);

        String frontierFileName = defaultConfig.getConfig().getString("genesis.frontier");
        JsonObject frontierObject = getJsonObjectFromFile(frontierFileName);
        genesisObject.add("frontier", frontierObject.get("frontier"));

        String nodeListFileName = defaultConfig.getConfig().getString("genesis.nodeList");
        JsonObject nodeListObject = getJsonObjectFromFile(nodeListFileName);
        genesisObject.add("nodeList", nodeListObject.get("nodeList"));

        Transaction tx = new Transaction(wallet, genesisObject);
        List<Transaction> txs = new ArrayList<>();
        txs.add(tx);

        genesisBlock = new Block(wallet, null, new BlockBody(txs));
    }

    private JsonObject getJsonObjectFromFile(String fileName) throws JSONException, IOException {
        StringBuilder result = new StringBuilder("");
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

    public Block getGenesisBlock() {
        return genesisBlock;
    }

    public void generateGenesisBlockFile() throws IOException {
        //todo: change the method to serializing method

        JsonObject jsonObject = this.genesisBlock.toJsonObject();

        ClassLoader classLoader = getClass().getClassLoader();
        File genesisFile = new File(classLoader.getResource(
                defaultConfig.getConfig().getString("genesis.block")).getFile());
        FileUtil.writeStringToFile(genesisFile, jsonObject.toString());
    }

}
