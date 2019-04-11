package io.yggdrash.core.blockchain;

import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.store.OutputStore;
import io.yggdrash.contract.core.store.OutputType;
import io.yggdrash.core.blockchain.osgi.ContractContainer;
import io.yggdrash.core.consensus.Block;
import io.yggdrash.core.consensus.Consensus;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.runtime.result.BlockRuntimeResult;
import io.yggdrash.core.store.BlockKeyStore;
import io.yggdrash.core.store.BranchStore;
import io.yggdrash.core.store.ConsensusBlockStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.TransactionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BlockChainImpl<T, V> implements BlockChain<T, V> {

    private static final Logger log = LoggerFactory.getLogger(BlockChainImpl.class);

    private final List<BranchEventListener> listenerList = new ArrayList<>();

    private final Branch branch;
    private final ConsensusBlockStore<T> blockStore;
    private final TransactionStore transactionStore;
    private final BranchStore branchStore;
    private final StateStore stateStore;
    private final TransactionReceiptStore transactionReceiptStore;

    private final Block<T> genesisBlock;
    private final Map<String, V> unConfirmedBlockMap = new ConcurrentHashMap<>();

    private Block<T> lastConfirmedBlock;

    private final ContractContainer contractContainer;
    private final Map<OutputType, OutputStore> outputStores;

    private final Consensus consensus;

    public BlockChainImpl(Branch branch, Block<T> genesisBlock,
                          ConsensusBlockStore<T> blockStore,
                          TransactionStore transactionStore,
                          BranchStore branchStore,
                          StateStore stateStore,
                          TransactionReceiptStore transactionReceiptStore,
                          ContractContainer contractContainer,
                          Map<OutputType, OutputStore> outputStores) {
        if (genesisBlock.getIndex() != 0
                || !genesisBlock.getPrevBlockHash().equals(Sha3Hash.createByHashed(Constants.EMPTY_HASH))) {
            log.error("GenesisBlock is not valid.");
            throw new NotValidateException();
        }
        this.branch = branch;
        this.genesisBlock = genesisBlock;
        this.blockStore = blockStore;
        this.transactionStore = transactionStore;
        this.branchStore = branchStore;
        this.stateStore = stateStore;
        this.transactionReceiptStore = transactionReceiptStore;
        this.contractContainer = contractContainer;
        this.outputStores = outputStores;
        this.consensus = new Consensus(branch.getConsensus());

        // Check BlockChain is Ready
        PrepareBlockchain prepareBlockchain = new PrepareBlockchain(contractContainer.getContractPath());
        // check block chain is ready
        if (prepareBlockchain.checkBlockChainIsReady(this)) {
            // install bundles
            // bc.getContractContainer()
            ContractContainer container = getContractContainer();
            for (BranchContract contract : prepareBlockchain.getContractList()) {
                File branchContractFile = prepareBlockchain.loadContractFile(contract.getContractVersion());
                container.installContract(contract.getContractVersion(), branchContractFile, contract.isSystem());
            }

            try {
                container.reloadInject();
            } catch (IllegalAccessException e) {
                log.error(e.getMessage());
                // TODO throw Runtime Exception
            }
        } else {
            // TODO blockchain ready fails
            log.error("Blockchain is not Ready");
        }

        // getGenesis Block by Store
        Sha3Hash blockHash = branchStore.getGenesisBlockHash();
        if (blockHash == null || !blockStore.contains(blockHash)) {
            log.debug("BlockChain init Genesis");
            initGenesis();
        } else {
            log.debug("BlockChain Load in Storage");
            // Load Block Chain Information
            loadTransaction();

            // load contract
        }
    }

    private void initGenesis() {
        for (TransactionHusk tx : genesisBlock.getBody()) {
            if (!transactionStore.contains(tx.getHash())) {
                transactionStore.put(tx.getHash(), tx);
            }
        }
        addBlock(genesisBlock, false);

        // Add Meta Information
        branchStore.setBranch(branch);
        branchStore.setGenesisBlockHash(genesisBlock.getHash());
        // TODO new Validators
        //branchStore.setValidators(branch.getValidators());
        branchStore.setBranchContracts(branch.getBranchContracts());
    }

    private void loadTransaction() {
        // load recent 1000 block
        // Start Block and End Block
        long bestBlock = branchStore.getBestBlock();
        long loadStart = bestBlock > 1000 ? bestBlock - 1000 : 0;
        for (long i = loadStart; i <= bestBlock; i++) {
            // recent block load and update Cache
            Block<T> block = blockStore.getBlockByIndex(i);
            // TODO node can be shutdown before blockStore.addBlock()
            // addBlock(): branchStore.setBestBlock() -> executeTransactions() -> blockStore.addBlock()
            if (block == null) {
                long prevIdx = i - 1;
                branchStore.setBestBlock(blockStore.getBlockByIndex(prevIdx));
                log.warn("reset branchStore bestBlock: {} -> {}", bestBlock, prevIdx);
                break;
            }
            transactionStore.updateCache(block.getBody());
            // set Last Best Block
            if (i == bestBlock) {
                this.lastConfirmedBlock = block;
            }
        }
    }

    @Override
    public BranchId getBranchId() {
        return genesisBlock.getBranchId();
    }

    @Override
    public Branch getBranch() {
        return branch;
    }

    @Override
    public Consensus getConsensus() {
        return consensus;
    }

    @Override
    public BlockKeyStore getBlockKeyStore() {
        throw new FailedOperationException("To be removed");
    }

    @Override
    public ConsensusBlockStore<T> getBlockStore() {
        return blockStore;
    }

    @Override
    public TransactionStore getTransactionStore() {
        return transactionStore;
    }

    @Override
    public Block<T> getGenesisBlock() {
        return genesisBlock;
    }

    @Override
    public Block<T> getLastConfirmedBlock() {
        return lastConfirmedBlock;
    }

    @Override
    public Map<String, V> getUnConfirmedData() {
        return unConfirmedBlockMap;
    }

    @Override
    public Block<T> addBlock(Block<T> nextBlock, boolean broadcast) {
        if (blockStore.contains(nextBlock.getHash())) {
            return null;
        }
        if (!isValidNewBlock(lastConfirmedBlock, nextBlock)) {
            String msg = String.format("Invalid to chain cur=%s, new=%s",
                    lastConfirmedBlock.getIndex(), nextBlock.getIndex());
            throw new NotValidateException(msg);
        }
        // add best Block
        branchStore.setBestBlock(nextBlock);

        // run Block Transactions
        // TODO run block execute move to other process (or thread)
        // TODO last execute block will invoke
        if (nextBlock.getIndex() > branchStore.getLastExecuteBlockIndex()) {
            //BlockRuntimeResult result = runtime.invokeBlock(nextBlock);
            BlockRuntimeResult result = contractContainer.getContractManager().executeTransactions(nextBlock);
            // Save Result
            contractContainer.getContractManager().commitBlockResult(result);
            //runtime.commitBlockResult(result);
            branchStore.setLastExecuteBlock(nextBlock);

            //Store event
            if (outputStores != null && outputStores.size() > 0) {
                Map<String, JsonObject> transactionMap = new HashMap<>();
                List<TransactionHusk> txList = nextBlock.getBody();
                txList.forEach(tx -> {
                    String txHash = tx.getHash().toString();
                    transactionMap.put(txHash, tx.toJsonObjectFromProto());
                });

                outputStores.forEach((storeType, store) -> {
                    store.put(nextBlock.toJsonObjectByProto());
                    store.put(nextBlock.getHash().toString(), transactionMap);
                });
            }
        }

        // Store Block Index and Block Data
        this.blockStore.addBlock(nextBlock);

        this.lastConfirmedBlock = nextBlock;
        batchTxs(nextBlock);
        if (!listenerList.isEmpty() && broadcast) {
            listenerList.forEach(listener -> listener.chainedBlock(nextBlock));
        }
        log.debug("Added idx=[{}], tx={}, branch={}, blockHash={}", nextBlock.getIndex(),
                nextBlock.getBodyCount(), getBranchId(), nextBlock.getHash());
        return nextBlock;
    }

    @Override
    public Block<T> addBlock(Block<T> block) {
        return addBlock(block, true);
    }

    private boolean isValidNewBlock(Block<T> prevBlock, Block<T> nextBlock) {
        if (prevBlock == null) {
            return true;
        }

        if (prevBlock.getIndex() + 1 != nextBlock.getIndex()) {
            log.warn("invalid index: prev:{} / new:{}", prevBlock.getIndex(), nextBlock.getIndex());
            return false;
        } else if (!prevBlock.getHash().equals(nextBlock.getPrevBlockHash())) {
            log.warn("invalid previous hash= {} {}", prevBlock.getHash(), nextBlock.getPrevBlockHash());
            return false;
        }

        return true;
    }

    private void batchTxs(Block<T> block) {
        if (block == null || block.getBody() == null) {
            return;
        }
        Set<Sha3Hash> keys = new HashSet<>();

        for (TransactionHusk tx : block.getBody()) {
            keys.add(tx.getHash());
        }
        transactionStore.batch(keys);
    }

    /**
     * Get BlockList from BlockStore with index, count.
     *
     * @param index index of block (0 <= index)
     * @param count count of blocks (1 < count <= 100)
     * @return list of Block
     */
    @Override
    public List<Block<T>> getBlockList(long index, long count) {
        List<Block<T>> blockList = new ArrayList<>();
        if (index < 0L || count < 1L || count > 100L) {
            log.debug("index or count is not valid");
            return blockList;
        }

        for (long l = index; l < index + count; l++) {
            try {
                blockList.add(blockStore.getBlockByIndex(l));
            } catch (Exception e) {
                break;
            }
        }

        return blockList;
    }

    @Override
    public TransactionHusk addTransaction(TransactionHusk tx) {
        return addTransaction(tx, true);
    }

    public TransactionHusk addTransaction(TransactionHusk tx, boolean broadcast) {
        if (transactionStore.contains(tx.getHash())) {
            return null;
        } else if (!tx.verify()) {
            throw new InvalidSignatureException();
        }

        try {
            transactionStore.put(tx.getHash(), tx);
            if (!listenerList.isEmpty() && broadcast) {
                listenerList.forEach(listener -> listener.receivedTransaction(tx));
            }
            return tx;
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }

    /**
     * Gets last block index.
     *
     * @return the last block index
     */
    @Override
    public long getLastIndex() {
        return lastConfirmedBlock.getIndex();
    }

    @Override
    public Collection<TransactionHusk> getRecentTxs() {
        return transactionStore.getRecentTxs();
    }

    @Override
    public List<TransactionHusk> getUnconfirmedTxs() {
        return new ArrayList<>(transactionStore.getUnconfirmedTxs());
    }

    /**
     * Gets transaction by hash.
     *
     * @param hash the hash
     * @return the transaction by hash
     */
    @Override
    public TransactionHusk getTxByHash(Sha3Hash hash) {
        return transactionStore.get(hash);
    }

    @Override
    public Block<T> getBlockByIndex(long index) {
        try {
            return blockStore.getBlockByIndex(index);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets block by hash.
     *
     * @param hash the hash
     * @return the block by hash
     */
    @Override
    public Block<T> getBlockByHash(Sha3Hash hash) {
        return blockStore.get(hash);
    }

    @Override
    public StateStore getStateStore() {
        return stateStore;
    }

    @Override
    public TransactionReceiptStore getTransactionReceiptStore() {
        return transactionReceiptStore;
    }

    @Override
    public TransactionReceipt getTransactionReceipt(String txId) {
        return transactionReceiptStore.get(txId);
    }

    @Override
    public ContractContainer getContractContainer() {
        return contractContainer;
    }

    @Override
    public long countOfTxs() {
        return transactionStore.countOfTxs();
    }

    @Override
    public List<BranchContract> getBranchContracts() {
        if (this.branchStore.getBranchContacts() == null) {
            return branch.getBranchContracts();
        } else {
            return this.branchStore.getBranchContacts();
        }
    }

    @Override
    public void close() {
        this.blockStore.close();
        this.transactionStore.close();
        this.branchStore.close();
        // TODO refactoring
        this.stateStore.close();
        this.transactionReceiptStore.close();
    }

    @Override
    public void addListener(BranchEventListener listener) {
        listenerList.add(listener);
    }
}
