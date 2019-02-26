package io.yggdrash.core.blockchain.genesis;

import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchContract;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class GenesisBlock {
    private final BlockHusk block;
    private final Branch branch;

    /**
     * Build genesis for dynamic branch.json
     *
     * @param branch branch info
     */
    private GenesisBlock(Branch branch) {
        this.branch = branch;
        this.block = toBlock();
    }

    public BlockHusk getBlock() {
        return block;
    }

    public Branch getBranch() {
        return branch;
    }

    private BlockHusk toBlock() {
        // Divided Branch Transaction
        // TODO Save Branch Genesis Transaction
        TransactionBuilder builder = new TransactionBuilder();
        // Contract init Transaction
        contractTransaction(builder);
        // Save Validator Transaction
        //validatorTransaction(builder);
        Transaction tx = builder.buildTransaction();

        // Make Genesis Block
        Block coreBlock = generatorGenesisBlock(tx);

        return new BlockHusk(coreBlock.toProtoBlock());
    }

    // Contract initial value
    private TransactionBuilder contractTransaction(TransactionBuilder builder) {
        List<BranchContract> contracts = branch.getBranchContracts();
        builder.setBranchId(branch.getBranchId())
                .setTimeStamp(branch.getTimestamp())
        ;
        contracts.forEach(c -> builder.addTxBody(c.getContractVersion(), "init", c.getInit(), c.isSystem()));
        return builder;
    }


    private Block generatorGenesisBlock(Transaction tx) {
        BlockBody blockBody = new BlockBody(Collections.singletonList(tx));
        BlockHeader blockHeader = new BlockHeader(
                branch.getBranchId().getBytes(),
                new byte[8],
                new byte[8],
                new byte[32],
                0L,
                branch.getTimestamp(),
                blockBody.getMerkleRoot(),
                blockBody.length());
        Block genesisBlock = new Block(blockHeader, new byte[]{}, blockBody);
        return genesisBlock;
    }


    /*
    private JsonObject toJsonObjectBlock() throws IOException {
        JsonObject jsonObjectTx = toJsonObjectTx();
        JsonArray jsonArrayBody = new JsonArray();
        jsonObjectTx.addProperty("signature", "");
        jsonArrayBody.add(jsonObjectTx);

        BlockBody blockBody = new BlockBody(jsonArrayBody);

        // todo: change values(version, type) using the configuration.
        BlockHeader blockHeader = new BlockHeader(
                branch.getBranchId().getBytes(),
                EMPTY_BYTE8,
                EMPTY_BYTE8,
                EMPTY_BYTE32,
                0L,
                branch.getTimestamp(),
                blockBody.getMerkleRoot(),
                blockBody.length());

        return toJsonObject(blockHeader.toJsonObject(), jsonArrayBody);
    }

    private JsonObject toJsonObjectTx() {
        JsonArray jsonArrayBody = toJsonArrayTxBody();
        // todo: change values(version, type) using the configuration.
        // TODO jsonFormat convert to Transaction
        TransactionHeader txHeader = new TransactionHeader(
                branch.getBranchId().getBytes(),
                EMPTY_BYTE8,
                EMPTY_BYTE8,
                branch.getTimestamp(),
                new TransactionBody(jsonArrayBody));
        log.debug(txHeader.toString());


        return toJsonObject(txHeader.toJsonObject(), jsonArrayBody);
    }

    private JsonArray toJsonArrayTxBody() {
        JsonArray jsonArrayTxBody = new JsonArray();
        JsonObject jsonObjectTx = new JsonObject();
        jsonArrayTxBody.add(jsonObjectTx);

        jsonObjectTx.addProperty("method", "genesis");
        JsonObject params = toGenesisParams();
        jsonObjectTx.add("params", params);
        jsonObjectTx.add("branch", branch.getJson());

        return jsonArrayTxBody;
    }

    // TODO change Genesis Spec
    private JsonObject toGenesisParams() {
        return branch.getJson().getAsJsonObject("genesis");
    }

    private JsonObject toJsonObject(JsonObject header, JsonArray body) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("header", header);
        jsonObject.add("body", body);
        return jsonObject;
    }
    */
    public static GenesisBlock of(InputStream is) throws IOException {
        Branch branch = Branch.of(is);
        return new GenesisBlock(branch);
    }

}
