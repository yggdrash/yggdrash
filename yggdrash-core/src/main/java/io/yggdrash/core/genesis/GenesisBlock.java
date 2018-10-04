package io.yggdrash.core.genesis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionBody;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.core.Wallet;
import io.yggdrash.util.TimeUtils;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class GenesisBlock {

    private Block genesisBlock;

    GenesisBlock() throws IOException, InvalidCipherTextException {

        DefaultConfig defaultConfig = new DefaultConfig();
        String transactionFileName = defaultConfig.getConfig().getString("genesis.contract");
        JsonObject genesisObject = getJsonObjectFromFile(transactionFileName);

        String delegatorListFileName = defaultConfig.getConfig().getString("genesis.delegator");
        JsonObject delegatorListObject = getJsonObjectFromFile(delegatorListFileName);
        genesisObject.add("delegator", delegatorListObject.get("delegator"));

        String nodeListFileName = defaultConfig.getConfig().getString("genesis.node");
        JsonObject nodeListObject = getJsonObjectFromFile(nodeListFileName);
        genesisObject.add("node", nodeListObject.get("node"));

        JsonArray jsonArrayTxBody = new JsonArray();
        jsonArrayTxBody.add(genesisObject);

        TransactionBody txBody = new TransactionBody(jsonArrayTxBody);

        long timestamp = TimeUtils.time();

        // todo: change values(version, type) using the configuration.
        TransactionHeader txHeader = new TransactionHeader(
                new byte[20],
                new byte[8],
                new byte[8],
                timestamp,
                txBody);

        String branchId = genesisObject.get("branchId").getAsString();
        byte[] chain = Hex.decode(branchId);

        // todo: change values(version, type) using the configuration.
        txHeader = new TransactionHeader(
                chain,
                new byte[8],
                new byte[8],
                timestamp,
                txBody);

        Wallet wallet = new Wallet(defaultConfig);
        Transaction tx = new Transaction(txHeader, wallet, txBody);
        List<Transaction> txList = new ArrayList<>();
        txList.add(tx);

        BlockBody blockBody = new BlockBody(txList);

        // todo: change values(version, type) using the configuration.
        BlockHeader blockHeader = new BlockHeader(
                chain,
                new byte[8],
                new byte[8],
                new byte[32],
                0L,
                timestamp,
                blockBody.getMerkleRoot(),
                blockBody.length());

        genesisBlock = new Block(blockHeader, wallet, blockBody);
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

    String getGenesisJson() {
        //todo: change the method to serializing method

        JsonObject jsonObject = this.genesisBlock.toJsonObject();
        return new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject);
    }

}
