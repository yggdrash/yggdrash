package io.yggdrash.core.blockchain.genesis;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.TransactionBody;
import io.yggdrash.core.blockchain.TransactionHeader;
import io.yggdrash.core.contract.ContractId;

import java.io.IOException;
import java.io.InputStream;

public class GenesisBlock {
    private final BlockHusk block;
    private final Branch branch;

    /**
     * Build genesis for dynamic branch.json
     *
     * @param branch branch info
     */
    private GenesisBlock(Branch branch) throws IOException {
        this.branch = branch;
        this.block = toBlock();
    }

    public BlockHusk getBlock() {
        return block;
    }

    public ContractId getContractId() {
        return branch.getContractId();
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
                branch.getBranchId().getBytes(),
                new byte[8],
                new byte[8],
                new byte[32],
                0L,
                branch.getTimestamp(),
                blockBody.getMerkleRoot(),
                blockBody.length());

        return toJsonObject(blockHeader.toJsonObject(), jsonArrayBody);
    }

    private JsonObject toJsonObjectTx() {
        JsonArray jsonArrayBody = toJsonArrayTxBody();
        // todo: change values(version, type) using the configuration.
        TransactionHeader txHeader = new TransactionHeader(
                branch.getBranchId().getBytes(),
                new byte[8],
                new byte[8],
                branch.getTimestamp(),
                new TransactionBody(jsonArrayBody));

        return toJsonObject(txHeader.toJsonObject(), jsonArrayBody);
    }

    private JsonArray toJsonArrayTxBody() {
        JsonArray jsonArrayTxBody = new JsonArray();
        JsonObject jsonObjectTx = new JsonObject();
        jsonArrayTxBody.add(jsonObjectTx);

        jsonObjectTx.addProperty("method", "genesis");
        JsonArray params = toGenesisParams();
        jsonObjectTx.add("params", params);
        jsonObjectTx.add("branch", branch.getJson());

        return jsonArrayTxBody;
    }

    private JsonArray toGenesisParams() {
        JsonArray params = new JsonArray();
        if (branch.isStem()) {
            JsonObject param = new JsonObject();
            param.add(branch.getBranchId().toString(), branch.getJson());
            params.add(param);
        } else {
            params.add(branch.getJson().get("genesis"));
        }

        return params;
    }

    private JsonObject toJsonObject(JsonObject header, JsonArray body) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("header", header);
        jsonObject.addProperty("signature", branch.getSignature().toString());
        jsonObject.add("body", body);
        return jsonObject;
    }

    public static GenesisBlock of(InputStream is) throws IOException {
        Branch branch = Branch.of(is);
        return new GenesisBlock(branch);
    }

}
