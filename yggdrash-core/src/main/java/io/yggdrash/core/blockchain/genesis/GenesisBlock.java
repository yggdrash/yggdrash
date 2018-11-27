package io.yggdrash.core.blockchain.genesis;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.TransactionBody;
import io.yggdrash.core.blockchain.TransactionHeader;

import java.io.IOException;

public class GenesisBlock {
    private final BlockHusk block;
    private final BranchJson branchJson;

    /**
     * Build genesis for dynamic branch.json
     *
     * @param branchJson dynamic loaded json
     */
    public GenesisBlock(BranchJson branchJson) throws IOException {
        this.branchJson = branchJson;
        this.block = toBlock();
    }

    public BlockHusk getBlock() {
        return block;
    }

    public String getContractId() {
        return branchJson.contractId;
    }

    private BlockHusk toBlock() throws IOException {
        JsonObject jsonObjectBlock = toJsonObjectBlock();

        Block coreBlock = new Block(jsonObjectBlock);
        return new BlockHusk(coreBlock.toProtoBlock());
    }

    private JsonObject toJsonObjectBlock() throws IOException {
        JsonObject jsonObjectTx = toJsonObjectTx();
        JsonArray jsonArrayBody = new JsonArray();
        jsonArrayBody.add(jsonObjectTx);

        BlockBody blockBody = new BlockBody(jsonArrayBody);

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
        jsonObjectBlock.add("body", jsonArrayBody);

        return toJsonObject(blockHeader.toJsonObject(), branchJson.signature, jsonArrayBody);
    }

    private JsonObject toJsonObjectTx() {
        JsonArray jsonArrayBody = toJsonArrayTxBody();
        // todo: change values(version, type) using the configuration.
        TransactionHeader txHeader = new TransactionHeader(
                branchJson.branchId().getBytes(),
                new byte[8],
                new byte[8],
                branchJson.longTimestamp(),
                new TransactionBody(jsonArrayBody));

        return toJsonObject(txHeader.toJsonObject(), branchJson.signature, jsonArrayBody);
    }

    private JsonArray toJsonArrayTxBody() {
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

    private JsonObject toJsonObject(JsonObject header, String signature, JsonArray body) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("header", header);
        jsonObject.addProperty("signature", signature);
        jsonObject.add("body", body);
        return jsonObject;
    }
}
