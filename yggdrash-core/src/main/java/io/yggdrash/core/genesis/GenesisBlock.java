package io.yggdrash.core.genesis;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.TransactionBody;
import io.yggdrash.core.TransactionHeader;

import java.io.IOException;

public class GenesisBlock {
    private final BlockInfo blockInfo;
    private final BlockHusk block;
    private String contractId;

    /**
     * Build genesis for dynamic branch.json
     *
     * @param branchJson dynamic loaded json
     */
    public GenesisBlock(BranchJson branchJson) throws IOException {
        this.blockInfo = new BlockInfo();
        this.contractId = branchJson.getContractId();
        Block coreBlock = toBlock(branchJson);
        this.block = new BlockHusk(coreBlock.toProtoBlock());
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
