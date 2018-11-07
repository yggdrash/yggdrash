package io.yggdrash.core.genesis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionBody;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.core.exception.NotValidateException;
import org.spongycastle.util.encoders.Hex;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GenesisBlock {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BlockInfo blockInfo;
    private final BlockHusk block;
    private String contractId;

    /**
     * Build genesis for static genesis.json
     *
     * @param branchInfoStream static generated genesis json
     */
    public GenesisBlock(InputStream branchInfoStream) {
        try {
            this.blockInfo = MAPPER.readValue(branchInfoStream, BlockInfo.class);
            this.block = new BlockHusk(toBlock().toProtoBlock());
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    /**
     * Build genesis for dynamic branch.json
     *
     * @param branchJson dynamic loaded json
     */
    GenesisBlock(BranchJson branchJson) throws Exception {
        this.blockInfo = new BlockInfo();
        this.contractId = branchJson.version;
        this.block = new BlockHusk(toBlock(branchJson).toProtoBlock());
    }

    public BlockHusk getBlock() {
        return block;
    }

    public String getContractId() {
        if (blockInfo.body != null && !blockInfo.body.isEmpty()) {
            TransactionInfo txInfo = blockInfo.body.get(0);
            if (txInfo.body != null && !txInfo.body.isEmpty()) {
                return txInfo.body.get(0).contractId;
            }
        }
        return contractId;
    }

    private Transaction toTransaction(TransactionInfo txi) {

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

    private Block toBlock() {
        BlockHeader blockHeader = new BlockHeader(
                Hex.decode(blockInfo.header.chain),
                Hex.decode(blockInfo.header.version),
                Hex.decode(blockInfo.header.type),
                Hex.decode(blockInfo.header.prevBlockHash),
                ByteUtil.byteArrayToLong(Hex.decode(blockInfo.header.index)),
                ByteUtil.byteArrayToLong(Hex.decode(blockInfo.header.timestamp)),
                Hex.decode(blockInfo.header.merkleRoot),
                ByteUtil.byteArrayToLong(Hex.decode(blockInfo.header.bodyLength))
        );

        List<Transaction> txList = new ArrayList<>();

        for (TransactionInfo txi : blockInfo.body) {
            txList.add(toTransaction(txi));
        }

        BlockBody txBody = new BlockBody(txList);

        return new Block(blockHeader, Hex.decode(blockInfo.signature), txBody);
    }

    private Block toBlock(BranchJson branchJson) throws Exception {
        long timestamp = ByteUtil.byteArrayToLong(Hex.decode(branchJson.timestamp));
        byte[] chain = org.spongycastle.util.encoders.Hex.decode(branchJson.branchId);

        JsonObject jsonObjectBlock = toJsonObjectBlock(branchJson, chain, timestamp);

        return new Block(jsonObjectBlock);
    }

    private JsonObject toJsonObjectBlock(BranchJson branchJson, byte[] chain, long timestamp)
            throws Exception {
        JsonObject jsonObjectTx = toJsonObjectTx(branchJson, chain, timestamp);
        JsonArray jsonArrayBlockBody = new JsonArray();
        jsonArrayBlockBody.add(jsonObjectTx);

        BlockBody blockBody = new BlockBody(jsonArrayBlockBody);

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

        JsonObject jsonObjectBlock = new JsonObject();
        jsonObjectBlock.add("header", blockHeader.toJsonObject());
        jsonObjectBlock.addProperty("signature", branchJson.signature);
        jsonObjectBlock.add("body", jsonArrayBlockBody);

        return jsonObjectBlock;
    }

    private JsonObject toJsonObjectTx(BranchJson branchJson, byte[] chain, long timestamp) {
        JsonArray jsonArrayTxBody = toJsonArrayTxBody(branchJson);
        // todo: change values(version, type) using the configuration.
        TransactionHeader txHeader = new TransactionHeader(
                chain,
                new byte[8],
                new byte[8],
                timestamp,
                new TransactionBody(jsonArrayTxBody));

        JsonObject jsonObjectTx = new JsonObject();
        jsonObjectTx.add("header", txHeader.toJsonObject());
        jsonObjectTx.addProperty("signature", branchJson.signature);
        jsonObjectTx.add("body", jsonArrayTxBody);

        return jsonObjectTx;
    }

    private JsonArray toJsonArrayTxBody(BranchJson branchJson) {
        JsonArray jsonArrayTxBody = new JsonArray();
        JsonObject jsonObjectTx = new JsonObject();
        jsonArrayTxBody.add(jsonObjectTx);

        jsonObjectTx.addProperty("method", "genesis");

        JsonArray params = new JsonArray();
        params.add(Utils.parseJsonObject(branchJson.genesis));
        jsonObjectTx.add("params", params);

        return jsonArrayTxBody;
    }
}
