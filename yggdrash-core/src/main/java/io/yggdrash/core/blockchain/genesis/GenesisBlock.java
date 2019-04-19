package io.yggdrash.core.blockchain.genesis;

import io.yggdrash.common.config.Constants;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchContract;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class GenesisBlock {
    private final Block block;
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

    private GenesisBlock(Branch branch, Block block) {
        this.branch = branch;
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    public Branch getBranch() {
        return branch;
    }

    private Block toBlock() {
        // Divided Branch Transaction
        // TODO Save Branch Genesis Transaction
        TransactionBuilder builder = new TransactionBuilder();
        // Contract init Transaction
        contractTransaction(builder);
        // Save Validator Transaction
        //validatorTransaction(builder);
        Transaction tx = builder.buildTransaction();

        // Make Genesis Block
        return generatorGenesisBlock(tx);
    }

    // Contract initial value
    private TransactionBuilder contractTransaction(TransactionBuilder builder) {
        List<BranchContract> contracts = branch.getBranchContracts();
        builder.setBranchId(branch.getBranchId())
                .setTimeStamp(branch.getTimestamp());

        for (BranchContract c : contracts) {
            builder.addTxBody(c.getContractVersion(), "init", c.getInit(), c.isSystem(),
                    branch.getConsensus());
        }
        return builder;
    }

    private Block generatorGenesisBlock(Transaction tx) {

        BlockBody blockBody = new BlockBody(Collections.singletonList(tx));

        BlockHeader blockHeader = new BlockHeader(
                branch.getBranchId().getBytes(),
                Constants.EMPTY_BYTE8,
                Constants.EMPTY_BYTE8,
                Constants.EMPTY_HASH,
                0L,
                branch.getTimestamp(),
                blockBody.getMerkleRoot(),
                blockBody.length());
        return new Block(blockHeader, Constants.EMPTY_SIGNATURE, blockBody);
    }

    public static GenesisBlock of(InputStream is) throws IOException {
        Branch branch = Branch.of(is);
        return new GenesisBlock(branch);
    }

    public static GenesisBlock of(Branch branch, Block block) {
        return new GenesisBlock(branch, block);
    }

}
