package io.yggdrash.core.genesis;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.TimeUtils;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GenesisBlock {

    private final BlockHusk block;

    public GenesisBlock(InputStream branchInfoStream) {
        try {
            BlockInfo blockinfo = new ObjectMapper().readValue(branchInfoStream, BlockInfo.class);
            this.block = new BlockHusk(fromBlockInfo(blockinfo).toProtoBlock());
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    public BlockHusk getBlock() {
        return block;
    }

    private Block fromBlockInfo(BlockInfo blockinfo) {
        BlockHeader blockHeader = new BlockHeader(
                Hex.decode(blockinfo.header.chain),
                Hex.decode(blockinfo.header.version),
                Hex.decode(blockinfo.header.type),
                Hex.decode(blockinfo.header.prevBlockHash),
                ByteUtil.byteArrayToLong(Hex.decode(blockinfo.header.index)),
                ByteUtil.byteArrayToLong(Hex.decode(blockinfo.header.timestamp)),
                Hex.decode(blockinfo.header.merkleRoot),
                ByteUtil.byteArrayToLong(Hex.decode(blockinfo.header.bodyLength))
        );

        List<Transaction> txList = new ArrayList<>();

        for (TransactionInfo txi : blockinfo.body) {
            txList.add(fromTransactionInfo(txi));
        }

        BlockBody txBody = new BlockBody(txList);

        return new Block(blockHeader, Hex.decode(blockinfo.signature), txBody);
    }

    private Transaction fromTransactionInfo(TransactionInfo txi) {

        TransactionHeader txHeader = new TransactionHeader(
                Hex.decode(txi.header.chain),
                Hex.decode(txi.header.version),
                Hex.decode(txi.header.type),
                ByteUtil.byteArrayToLong(Hex.decode(txi.header.timestamp)),
                Hex.decode(txi.header.bodyHash),
                ByteUtil.byteArrayToLong(Hex.decode(txi.header.bodyLength))
        );

        TransactionBody txBody = new TransactionBody(new Gson().toJson(txi.body));

        return new Transaction(txHeader, Hex.decode(txi.signature), txBody);
    }

    static String generate() throws IOException, InvalidCipherTextException {
        //todo: change the method to serializing method

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

        Block genesisBlock = new Block(blockHeader, wallet, blockBody);
        JsonObject jsonObject = genesisBlock.toJsonObject();
        return new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject);
    }

    private static JsonObject getJsonObjectFromFile(String fileName) throws IOException {
        StringBuilder result = new StringBuilder();
        ClassLoader classLoader = GenesisBlock.class.getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());

        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            result.append(line).append("\n");
        }

        scanner.close();

        return new Gson().fromJson(result.toString(), JsonObject.class);
    }
}
