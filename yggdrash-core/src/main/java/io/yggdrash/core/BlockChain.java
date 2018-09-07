package io.yggdrash.core;

import com.google.common.annotations.VisibleForTesting;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.contract.Contract;
import io.yggdrash.contract.NoneContract;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class BlockChain {

    private static final Logger log = LoggerFactory.getLogger(BlockChain.class);

    // <Variable>
    private final BlockHusk genesisBlock;
    private BlockHusk prevBlock;
    private BlockStore blockStore;
    private TransactionStore transactionStore;
    private Contract contract;
    private Runtime<?> runtime;

    @VisibleForTesting
    public BlockChain(File infoFile) {
        try {
            this.genesisBlock = new BlockChainLoader(infoFile).getGenesis();
            this.blockStore = new BlockStore(getBranchId());
            this.transactionStore = new TransactionStore(new HashMapDbSource());
            this.contract = new NoneContract();
            this.runtime = new Runtime<>(new StateStore<>(), new TransactionReceiptStore());
            loadBlockChain();
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    public BlockChain(BlockHusk genesisBlock, BlockStore blockStore,
                      TransactionStore transactionStore, Contract contract, Runtime runtime) {
        this.genesisBlock = genesisBlock;
        this.blockStore = blockStore;
        this.transactionStore = transactionStore;
        this.contract = contract;
        this.runtime = runtime;
        loadBlockChain();
    }

    private void loadBlockChain() {
        try {
            prevBlock = blockStore.get(genesisBlock.getHash());
        } catch (NonExistObjectException e) {
            addBlock(genesisBlock);
        }
    }

    public BlockHusk generateBlock(Wallet wallet) {
        BlockHusk block = BlockHuskBuilder.buildSigned(wallet,
                new ArrayList<>(transactionStore.getUnconfirmedTxs()), getPrevBlock());
        return addBlock(block);
    }

    public List<TransactionHusk> getTransactionList() {
        return new ArrayList<>(transactionStore.getUnconfirmedTxs());
    }

    public BranchId getBranchId() {
        return new BranchId(genesisBlock.getHash());
    }

    public BlockHusk getGenesisBlock() {
        return this.genesisBlock;
    }

    public BlockHusk getPrevBlock() {
        return this.prevBlock;
    }

    public Set<BlockHusk> getBlocks() {
        return blockStore.getAll();
    }

    /**
     * Gets last block index.
     *
     * @return the last block index
     */
    public long getLastIndex() {
        if (isGenesisBlockChain()) {
            return 0;
        }
        return prevBlock.nextIndex();
    }

    /**
     * Add block.
     *
     * @param nextBlock the next block
     * @throws NotValidateException the not validate exception
     */
    public BlockHusk addBlock(BlockHusk nextBlock) {
        if (blockStore.contains(nextBlock.getHash())) {
            return null;
        }
        if (!isValidNewBlock(prevBlock, nextBlock)) {
            throw new NotValidateException("Invalid to chain");
        }
        executeAllTx(new TreeSet<>(nextBlock.getBody()));
        log.debug("Added block index=[{}], blockHash={}", nextBlock.getIndex(),
                nextBlock.getHash());
        this.blockStore.put(nextBlock.getHash(), nextBlock);
        this.prevBlock = nextBlock;
        removeTxByBlock(nextBlock);
        return nextBlock;
    }

    private boolean isValidNewBlock(BlockHusk prevBlock, BlockHusk nextBlock) {
        if (prevBlock == null) {
            return true;
        }
        log.trace(" prev : " + prevBlock.getHash());
        log.trace(" new : " + nextBlock.getHash());

        if (prevBlock.getIndex() + 1 != nextBlock.getIndex()) {
            log.warn("invalid index: prev:{} / new:{}", prevBlock.getIndex(), nextBlock.getIndex());
            return false;
        } else if (!prevBlock.getHash().equals(nextBlock.getPrevHash())) {
            log.warn("invalid previous hash");
            return false;
        }

        return true;
    }

    public TransactionHusk addTransaction(TransactionHusk tx) {
        if (transactionStore.contains(tx.getHash())) {
            throw new FailedOperationException("Duplicated " + tx.getHash().toString()
                    + " Transaction");
        } else if (!tx.verify()) {
            throw new InvalidSignatureException();
        }

        try {
            transactionStore.put(tx.getHash(), tx);
            return tx;
        } catch (Exception e) {
            throw new FailedOperationException("Transaction");
        }
    }

    public long size() {
        return blockStore.size();
    }

    /**
     * Is valid chain boolean.
     *
     * @return the boolean
     */
    public boolean isValidChain() {
        return isValidChain(this);
    }

    /**
     * Is valid chain boolean.
     *
     * @param blockChain the block chain
     * @return the boolean
     */
    public boolean isValidChain(BlockChain blockChain) {
        if (blockChain.getPrevBlock() != null) {
            BlockHusk block = blockChain.getPrevBlock(); // Get Last Block
            while (block.getIndex() != 0L) {
                block = blockChain.getBlockByHash(block.getPrevHash());
            }
            return block.getIndex() == 0L;
        }
        return true;
    }

    public BlockHusk getBlockByIndex(long index) {
        for (BlockHusk block : this.getBlocks()) {
            if (block.getIndex() == index) {
                return block;
            }
        }
        throw new NonExistObjectException("Block index=" + index);
    }

    /**
     * Gets block by hash.
     *
     * @param hash the hash
     * @return the block by hash
     */
    public BlockHusk getBlockByHash(String hash) {
        return getBlockByHash(new Sha3Hash(hash));
    }

    /**
     * Gets block by hash.
     *
     * @param hash the hash
     * @return the block by hash
     */
    public BlockHusk getBlockByHash(Sha3Hash hash) {
        return blockStore.get(hash);
    }

    /**
     * Gets transaction by hash.
     *
     * @param hash the hash
     * @return the transaction by hash
     */
    public TransactionHusk getTxByHash(String hash) {
        return getTxByHash(new Sha3Hash(hash));
    }

    /**
     * Gets transaction by hash.
     *
     * @param hash the hash
     * @return the transaction by hash
     */
    public TransactionHusk getTxByHash(Sha3Hash hash) {
        return transactionStore.get(hash);
    }

    /**
     * Is genesis block chain boolean.
     *
     * @return the boolean
     */
    public boolean isGenesisBlockChain() {
        return (this.prevBlock == null);
    }


    private void executeAllTx(Set<TransactionHusk> txList) {
        try {
            for (TransactionHusk tx : txList) {
                if (!runtime.invoke(contract, tx)) {
                    break;
                }
            }
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }

    private void removeTxByBlock(BlockHusk block) {
        if (block == null || block.getBody() == null) {
            return;
        }
        Set<Sha3Hash> keys = new HashSet<>();

        for (TransactionHusk tx : block.getBody()) {
            keys.add(tx.getHash());
        }
        transactionStore.batch(keys);
    }

    @Override
    public String toString() {
        return "BlockChain{"
                + "genesisBlock=" + genesisBlock
                + ", prevBlock=" + prevBlock
                + ", height=" + this.getLastIndex()
                + '}';
    }

    public void close() {
        this.blockStore.close();
    }

    public String toStringStatus() {
        StringBuilder builder = new StringBuilder();

        builder.append("[BlockChain Status]\n")
                .append("genesisBlock=")
                .append(genesisBlock.getHash()).append("\n").append("currentBlock=" + "[")
                .append(prevBlock.getIndex()).append("]").append(prevBlock.getHash()).append("\n");

        String prevBlockHash = this.prevBlock.getPrevHash().toString();
        if (prevBlockHash == null) {
            prevBlockHash = "";
        }

        do {
            builder.append("<-- " + "[")
                    .append(blockStore.get(new Sha3Hash(prevBlockHash)).getIndex())
                    .append("]").append(prevBlockHash).append("\n");

            prevBlockHash = blockStore.get(new Sha3Hash(prevBlockHash)).getPrevHash().toString();

        } while (prevBlockHash != null
                && !prevBlockHash.equals(
                    "0000000000000000000000000000000000000000000000000000000000000000"));

        return builder.toString();

    }
}
