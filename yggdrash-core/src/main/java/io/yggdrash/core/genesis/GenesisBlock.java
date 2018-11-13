package io.yggdrash.core.genesis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionBody;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.core.exception.NotValidateException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
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
    public GenesisBlock(BranchJson branchJson) throws IOException {
        this.blockInfo = new BlockInfo();
        this.contractId = branchJson.getContractId();
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

    private Block toBlock(BranchJson branchJson) throws IOException {

        JsonObject jsonObjectBlock = toJsonObjectBlock(branchJson);

        return new Block(jsonObjectBlock);
    }

    private JsonObject toJsonObjectBlock(BranchJson branchJson) throws IOException {
        JsonObject jsonObjectTx = toJsonObjectTx(branchJson);
        JsonArray jsonArrayBlockBody = new JsonArray();
        jsonArrayBlockBody.add(jsonObjectTx);

        BlockBody blockBody = new BlockBody(jsonArrayBlockBody);

        // todo: change values(version, type) using the configuration.
        BlockHeader blockHeader = new BlockHeader(
                branchJson.branchId().getBytes(),
                new byte[8],
                new byte[8],
                new byte[32],
                0L,
                branchJson.longTimestamp(),
                blockBody.getMerkleRoot(),
                blockBody.length());

        JsonObject jsonObjectBlock = new JsonObject();
        jsonObjectBlock.add("header", blockHeader.toJsonObject());
        jsonObjectBlock.addProperty("signature", branchJson.signature);
        jsonObjectBlock.add("body", jsonArrayBlockBody);

        return jsonObjectBlock;
    }

    private JsonObject toJsonObjectTx(BranchJson branchJson) {
        JsonArray jsonArrayTxBody = toJsonArrayTxBody(branchJson);
        // todo: change values(version, type) using the configuration.
        TransactionHeader txHeader = new TransactionHeader(
                branchJson.branchId().getBytes(),
                new byte[8],
                new byte[8],
                branchJson.longTimestamp(),
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

        JsonObject jsonObjectBranch = branchJson.toJsonObject();

        JsonArray params = toGenesisParams(jsonObjectBranch, branchJson.isStem());
        jsonObjectTx.add("params", params);

        jsonObjectTx.addProperty("method", "genesis");
        jsonObjectTx.add("branch", jsonObjectBranch);

        return jsonArrayTxBody;
    }

    private JsonArray toGenesisParams(JsonObject jsonObjectBranch, boolean isStem) {
        JsonElement jsonElementGenesis = jsonObjectBranch.remove("genesis");
        JsonArray params = new JsonArray();
        if (isStem) {
            JsonObject param = new JsonObject();
            param.add(jsonObjectBranch.get("branchId").getAsString(), jsonObjectBranch);
            params.add(param);
        } else {
            params.add(jsonElementGenesis);
        }

        return params;
    }
}
