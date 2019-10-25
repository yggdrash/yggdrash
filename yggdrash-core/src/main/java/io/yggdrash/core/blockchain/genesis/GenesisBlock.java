package io.yggdrash.core.blockchain.genesis;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.BlockImpl;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBuilder;
import io.yggdrash.core.consensus.Consensus;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GenesisBlock {
    private final Branch branch;
    private Block block;
    private List<Transaction> contractTxs;

    /**
     * Build genesis for dynamic branch.json
     *
     * @param branch branch info
     */
    private GenesisBlock(Branch branch) {
        this.branch = branch;
        this.contractTxs = contractTransactions();
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

    public BranchId getBranchId() {
        return branch.getBranchId();
    }

    public Consensus getConsensus() {
        return new Consensus(branch.getConsensus());
    }

    public List<Transaction> getContractTxs() {
        return this.contractTxs;
    }

    public void toBlock(Sha3Hash stateRoot) {
        this.block = generatorGenesisBlock(contractTxs, stateRoot);
    }

    // Contract initial value
    private List<Transaction> contractTransactions() {
        // Divided Branch Transaction
        // TODO Save Branch Genesis Transaction
        List<Transaction> txs = new ArrayList<>();
        List<BranchContract> contracts = branch.getBranchContracts();

        for (BranchContract c : contracts) {
            TransactionBuilder builder = new TransactionBuilder();
            // TODO remove consensus in transaction
            builder.setBranchId(branch.getBranchId())
                    .setTimeStamp(branch.getTimestamp())
                    .setTxBody(c.getContractVersion(), "init", c.getInit(), c.isSystem(), branch.getConsensus());

            txs.add(builder.build());
        }
        return txs;
    }

    private Block generatorGenesisBlock(List<Transaction> txs, Sha3Hash stateRoot) {
        BlockBody blockBody = new BlockBody(txs);
        BlockHeader blockHeader = new BlockHeader(
                branch.getBranchId().getBytes(),
                Constants.EMPTY_BYTE8,
                Constants.EMPTY_BYTE8,
                Constants.EMPTY_HASH,
                0L,
                branch.getTimestamp(),
                stateRoot.getBytes(),
                blockBody);

        return new BlockImpl(blockHeader, Constants.EMPTY_SIGNATURE, blockBody);
    }

    public static GenesisBlock of(InputStream is) throws IOException {
        Branch branch = Branch.of(is);
        return new GenesisBlock(branch);
    }

    public static GenesisBlock of(Branch branch) {
        return new GenesisBlock(branch);
    }

    public static GenesisBlock of(Branch branch, Block block) {
        return new GenesisBlock(branch, block);
    }

}
