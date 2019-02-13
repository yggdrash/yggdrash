package io.yggdrash.validator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.util.FileUtil;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBody;
import io.yggdrash.core.blockchain.TransactionHeader;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static io.yggdrash.common.config.Constants.EMPTY_BYTE32;
import static io.yggdrash.common.config.Constants.EMPTY_BYTE8;

public class GenesisBlock {

    private Block genesisBlock;

    public GenesisBlock() throws IOException, InvalidCipherTextException {

        JsonObject genesisObject = getJsonObjectFromFile("./genesis/config.json");

        JsonObject frontierObject = getJsonObjectFromFile("./genesis/frontier.json");
        genesisObject.add("frontier", frontierObject.get("frontier"));

        JsonObject nodeListObject = getJsonObjectFromFile("./genesis/node.json");
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

        DefaultConfig defaultConfig = new DefaultConfig();
        Wallet wallet = new Wallet(defaultConfig);
        Transaction tx = new Transaction(txHeader, wallet, txBody);
        List<Transaction> txList = new ArrayList<>();
        txList.add(tx);

        BlockBody blockBody = new BlockBody(txList);

        // todo: change values(version, type) using the configuration.
        BlockHeader blockHeader = new BlockHeader(
                chain,
                EMPTY_BYTE8,
                EMPTY_BYTE8,
                EMPTY_BYTE32,
                0L,
                timestamp,
                blockBody.getMerkleRoot(),
                blockBody.length());

        genesisBlock = new Block(blockHeader, wallet, blockBody);

    }

    private JsonObject getJsonObjectFromFile(String fileName) {
        StringBuilder result = new StringBuilder();
        ClassLoader classLoader = getClass().getClassLoader();
        File file;
        Scanner scanner;
        try {
            file = new File(classLoader.getResource(fileName).getFile());
            scanner = new Scanner(file);
        } catch (Exception e) {
            throw new NotValidateException();
        }

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

    public void generateGenesisBlockFile() {
        //todo: change the method to serializing method

        JsonObject jsonObject = this.genesisBlock.toJsonObject();

        ClassLoader classLoader = getClass().getClassLoader();

        try {
            File genesisFile = new File(classLoader.getResource("./genesis/genesis.json").getFile());
            FileUtil.writeStringToFile(genesisFile,
                    new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject),
                    StandardCharsets.UTF_8, false);
        } catch (Exception e) {
            throw new NotValidateException();
        }
    }

}
