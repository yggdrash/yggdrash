package io.yggdrash.core.genesis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionBody;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.core.Wallet;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.util.FileUtil;
import io.yggdrash.util.TimeUtils;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.File;
import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GenesisBlock {

    private final DefaultConfig defaultConfig = new DefaultConfig();
    private Block genesisBlock;

    public GenesisBlock() throws IOException, InvalidCipherTextException, SignatureException {
        Wallet wallet = new Wallet(defaultConfig);

        String transactionFileName = defaultConfig.getConfig().getString("genesis.config");
        JsonObject genesisObject = getJsonObjectFromFile(transactionFileName);

        String frontierFileName = defaultConfig.getConfig().getString("genesis.frontier");
        JsonObject frontierObject = getJsonObjectFromFile(frontierFileName);
        genesisObject.add("frontier", frontierObject.get("frontier"));

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

        byte[] chain = HashUtil.sha3omit12(txHeader.toBinary());

        // todo: change values(version, type) using the configuration.
        txHeader = new TransactionHeader(
                chain,
                new byte[8],
                new byte[8],
                timestamp,
                txBody);

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

    public Block getGenesisBlock() {
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
