package io.yggdrash.core.blockchain.genesis;

import com.google.gson.JsonObject;
import io.yggdrash.common.config.Constants;
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
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenesisBlock {
    private static final Logger log = LoggerFactory.getLogger(GenesisBlock.class);
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
        validatorTransaction(builder);
        Transaction tx = builder.buildTransaction();

        // Make Genesis Block
        Block coreBlock = generatorGenesisBlock(tx);

        return new BlockHusk(coreBlock.toProtoBlock());
    }

    // Validator initial value
    private TransactionBuilder validatorTransaction(TransactionBuilder builder) {
        List<String> validators = branch.getValidators();
        JsonObject validatorParams = new JsonObject();
        validatorParams.addProperty("validator", String.join(",",validators));
        // TODO Validator Contract Version
        builder.addTxBody(Constants.VALIDATOR_CONTRACT_VERSION, "init", validatorParams);

        return builder;
    }

    // Contract initial value
    private TransactionBuilder contractTransaction(TransactionBuilder builder) {
        List<BranchContract> contracts = branch.getBranchContracts();
        builder.setBranchId(branch.getBranchId())
                .setTimeStamp(branch.getTimestamp())
        ;
        contracts.stream().forEach(c -> {
            builder.addTxBody(c.getContractVersion(), "init", c.getInit());
        });
        return builder;
    }


    private Block generatorGenesisBlock(Transaction tx) {
        BlockBody blockBody = new BlockBody(Arrays.asList(tx));
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
        // TODO jsonFormat convert to Transaction
        TransactionHeader txHeader = new TransactionHeader(
                branch.getBranchId().getBytes(),
                new byte[8],
                new byte[8],
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
