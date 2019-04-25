package io.yggdrash.core.blockchain.genesis;

import io.yggdrash.common.config.Constants;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.BlockImpl;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchContract;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
        return generatorGenesisBlock(contractTransactions());
    }

    // Contract initial value
    private List<Transaction> contractTransactions() {
        // Divided Branch Transaction
        // TODO Save Branch Genesis Transaction
        List<Transaction> txs = new ArrayList<>();
        List<BranchContract> contracts = branch.getBranchContracts();

        for (BranchContract c : contracts) {
            TransactionBuilder builder = new TransactionBuilder();
            builder.setBranchId(branch.getBranchId())
                    .setTimeStamp(branch.getTimestamp())
                    .addTxBody(c.getContractVersion(), "init", c.getInit(), c.isSystem(), branch.getConsensus());

            txs.add(builder.build());
        }
        return txs;
    }

    private Block generatorGenesisBlock(List<Transaction> txs) {
        BlockBody blockBody = new BlockBody(txs);
        BlockHeader blockHeader = new BlockHeader(
                branch.getBranchId().getBytes(),
                Constants.EMPTY_BYTE8,
                Constants.EMPTY_BYTE8,
                Constants.EMPTY_HASH,
                0L,
                branch.getTimestamp(),
                blockBody);

        return new BlockImpl(blockHeader, Constants.EMPTY_SIGNATURE, blockBody);
    }

    public static GenesisBlock of(InputStream is) throws IOException {
        Branch branch = Branch.of(is);
        return new GenesisBlock(branch);
    }

    public static GenesisBlock of(Branch branch, Block block) {
        return new GenesisBlock(branch, block);
    }

}
