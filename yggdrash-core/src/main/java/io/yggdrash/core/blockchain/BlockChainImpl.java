package io.yggdrash.core.blockchain;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.common.util.VerifierUtils;
import io.yggdrash.contract.core.ContractEvent;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.core.blockchain.osgi.ContractEventListener;
import io.yggdrash.core.blockchain.osgi.ContractManager;
import io.yggdrash.core.consensus.Consensus;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.exception.errorcode.BusinessError;
import io.yggdrash.core.runtime.result.BlockRuntimeResult;
import io.yggdrash.core.runtime.result.TransactionRuntimeResult;
import io.yggdrash.core.store.BlockKeyStore;
import io.yggdrash.core.store.BranchStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class BlockChainImpl<T, V> implements BlockChain<T, V> {
    private static final Logger log = LoggerFactory.getLogger(BlockChainImpl.class);

    private final List<BranchEventListener> listenerList = new ArrayList<>();
    private final List<ContractEventListener> contractEventListenerList = new ArrayList<>();

    private final Branch branch;
    private final ConsensusBlock<T> genesisBlock;
    private final Map<String, V> unConfirmedBlockMap = new ConcurrentHashMap<>();

    private final BlockChainManager<T> blockChainManager;
    private final ContractManager contractManager;
    private final BranchStore branchStore;

    private final Consensus consensus;
    private final ReentrantLock lock = new ReentrantLock();

    private boolean isFullSynced = false;

    public ReentrantLock lock2 = new ReentrantLock();

    public BlockChainImpl(Branch branch,
                          ConsensusBlock<T> genesisBlock,
                          BranchStore branchStore,
                          BlockChainManager<T> blockChainManager,
                          ContractManager contractManager) {
        this.branch = branch;
        this.consensus = new Consensus(branch.getConsensus());
        this.branchStore = branchStore;
        this.genesisBlock = genesisBlock;
        this.blockChainManager = blockChainManager;
        this.contractManager = contractManager;

        if (!VerifierUtils.verifyGenesisHash(genesisBlock)) {
            log.error("GenesisBlock is not valid.");
            throw new NotValidateException();
        }

        init();
    }

    public boolean isFullSynced() {
        return isFullSynced;
    }

    public void setFullSynced(boolean fullSynced) {
        isFullSynced = fullSynced;
    }

    private void init() {
        // step1: branch contract check
        if (this.getBranchContracts().isEmpty()) {
            log.error("This branch {} has no any contract information.", getBranch().getBranchId());
        }

        // getGenesis Block by Store
        Sha3Hash blockHash = branchStore.getGenesisBlockHash();
        if (blockHash == null || !blockChainManager.containsBlockHash(blockHash)) {
            log.debug("BlockChain init Genesis");
            initGenesis();
        } else {
            log.debug("BlockChain Load in Storage");
            // Load Block Chain Information
            blockChainManager.loadTransaction(); // load stateRoot when updateTxCache!
            // Load StateRoot
            //blockChainManager.setPendingStateRoot(contractManager.getOriginStateRoot());
        }
    }

    private void initGenesis() {
        // After executing the transactions of GenesisBlock,
        // put them in the txStore with the stateRootHash of pendingStateStore.
        blockChainManager.initGenesis(genesisBlock);
        addBlock(genesisBlock, false);

        // Add Meta Information
        branchStore.setBranch(branch);
        branchStore.setGenesisBlockHash(genesisBlock.getHash());
        // TODO new Validators
        //branchStore.setValidators(branch.getValidators());
        branchStore.setBranchContracts(branch.getBranchContracts());
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
    public ConsensusBlock<T> getGenesisBlock() {
        return genesisBlock;
    }

    @Override
    public Map<String, V> getUnConfirmedData() {
        return unConfirmedBlockMap;
    }

    @Override
    public Map<String, List<String>> addBlock(ConsensusBlock<T> nextBlock, boolean broadcast) {
        try {
            lock.lock();
            int verificationCode = blockChainManager.verify(nextBlock);
            if (verificationCode != BusinessError.VALID.toValue()) {
                log.trace("addBlock is failed. Index({}) {}",
                        nextBlock.getIndex(), BusinessError.getErrorLogsMap(verificationCode).values());
                return BusinessError.getErrorLogsMap(verificationCode);
            }
            // Add best Block
            branchStore.setBestBlock(nextBlock);

            // Run Block Transactions
            // TODO run block execute move to other process (or thread)
            // TODO last execute block will invoke
            if (nextBlock.getIndex() > branchStore.getLastExecuteBlockIndex()) {
                // Execute block and commit the result of block.
                BlockRuntimeResult blockResult = contractManager.executeTxs(nextBlock); //TODO Exception
                // Validate StateRoot
                Sha3Hash blockResultStateRoot = blockResult.getBlockResult().size() > 0
                        ? new Sha3Hash(blockResult.getBlockResult().get("stateRoot").get("stateHash").getAsString())
                        : contractManager.getOriginStateRoot();
                Sha3Hash nextBlockStateRoot = new Sha3Hash(nextBlock.getHeader().getStateRoot(), true);
                if (!nextBlockStateRoot.equals(blockResultStateRoot)) {
                    log.warn("Add block failed. Invalid stateRoot. BlockStateRoot : {}, CurStateRoot : {}"
                            , nextBlockStateRoot, blockResultStateRoot);
                    return BusinessError.getErrorLogsMap(BusinessError.INVALID_STATE_ROOT_HASH.toValue());
                }

                branchStore.setLastExecuteBlock(nextBlock);
            }

            BlockRuntimeResult endBlockResult = contractManager.endBlock(nextBlock);
            if (endBlockResult.getBlockResult().size() > 0) {
                String endBlockStateRoot = endBlockResult.getBlockResult().get("stateRoot").get("stateHash").getAsString();
                log.debug("endBlockStateRoot : {} ", endBlockStateRoot);
            }

            // Fire contract event
            getContractEventList(endBlockResult).stream()
                    .filter(event -> !contractEventListenerList.isEmpty())
                    .forEach(event -> contractEventListenerList.forEach(l -> l.endBlock(event)));

            lock2.lock();
            blockChainManager.batchTxs(nextBlock, contractManager.getOriginStateRoot());

            // PendingStateStore is still running without reset.
            Sha3Hash curStateRoot = reExecuteAndRemoveFromPendingPool(blockChainManager.getUnconfirmedTxs());

            // BlockChainManager add nextBlock to the blockStore, set the lastConfirmedBlock to nextBlock,
            // and then batch the transactions.
            blockChainManager.addBlock(nextBlock);
            blockChainManager.batchTxs(nextBlock, curStateRoot);
            lock2.unlock();

            if (!listenerList.isEmpty() && broadcast) {
                listenerList.forEach(listener -> listener.chainedBlock(nextBlock));
            }
            nextBlock.loggingBlock(this.blockChainManager.getUnconfirmedTxsSize());
        } catch (Exception e) {
            log.debug("Add block failed. {}", e.getMessage()); //TODO Exception handling
        } finally {
            lock.unlock();
        }

        return new HashMap<>();
    }

    private List<ContractEvent> getContractEventList(BlockRuntimeResult result) {
        List<ContractEvent> contractEventList = new ArrayList<>();
        result.getReceipts().stream()
                .filter(receipt -> !receipt.getEvents().isEmpty())
                .map(Receipt::getEvents)
                .forEach(contractEventList::addAll);
        return contractEventList;
    }

    private Sha3Hash reExecuteAndRemoveFromPendingPool(List<Transaction> txs) {
        // PendingStateStore is still running without reset.
        contractManager.resetPendingStateStore();
        Sha3Hash curStateRoot = contractManager.getOriginStateRoot();
        for (Transaction tx : txs) {
            curStateRoot = addPendingTxs(tx);
            if (curStateRoot == null) { // Err tx in pendingPool
                blockChainManager.flushUnconfirmedTx(tx.getHash());
            }
        }

        return curStateRoot;
    }

    private Sha3Hash addPendingTxs(Transaction tx) {
        lock2.lock();
        try {
            Sha3Hash curStateRootHash = contractManager.executePendingTxWithStateRoot(tx);
            log.trace("executeAndAddToPendingPool : curStateRootHash {}", curStateRootHash);
            if (curStateRootHash != null) {
                blockChainManager.addTransaction(tx, curStateRootHash);
            }
            return curStateRootHash;
        } finally {
            lock2.unlock();
        }
    }

    @Override
    public Map<String, List<String>> addBlock(ConsensusBlock<T> block) {
        return addBlock(block, true);
    }

    @Override
    public boolean isValidator(String addr) {
        return branchStore.isValidator(addr);
    }

    @Override
    public ValidatorSet getValidators() {
        return branchStore.getValidators();
    }

    @Override
    public Map<String, List<String>> addTransaction(Transaction tx) {
        return addTransaction(tx, true);
    }

    public Map<String, List<String>> addTransaction(Transaction tx, boolean broadcast) {
        int verifyResult = blockChainManager.verify(tx);
        if (verifyResult == BusinessError.VALID.toValue()) {
            TransactionRuntimeResult txResult = contractManager.executeTx(tx); //checkTx
            log.debug("executeTx() tx={} {}", tx, txResult.toString());
            if (txResult.getReceipt().getStatus() != ExecuteStatus.ERROR) {
                // Execute tx before adding tx to pending pool. Err tx would not be added.
                executeAndAddToPendingPool(tx);

                if (!listenerList.isEmpty() && broadcast) {
                    listenerList.forEach(listener -> listener.receivedTransaction(tx));
                }

                return new HashMap<>();
            } else {
                log.debug("addTransaction() is failed. tx={} {}",
                        tx, txResult.getReceipt().getLog());
                Map<String, List<String>> applicationError = new HashMap<>();
                applicationError.put("SystemError", txResult.getReceipt().getLog());
                return applicationError;
            }
        } else {
            log.debug("addTransaction() is failed. tx={} {}",
                    tx, BusinessError.getErrorLogsMap(verifyResult));
            return BusinessError.getErrorLogsMap(verifyResult);
        }
    }

    @Override
    public Sha3Hash executeAndAddToPendingPool(Transaction tx) {
        lock2.lock();
        try {
            // return pendingTx stateRootHash
            if (!blockChainManager.contains(tx)) {
                Sha3Hash stateRoot = addPendingTxs(tx);
                log.debug("executeAndAddToPendingPool() tx={} stateRoot={}", tx, stateRoot);
                return stateRoot;
            }

            log.debug("executeAndAddToPendingPool() is failed. tx={} ", tx);
            return null;
        } finally {
            lock2.unlock();
        }
    }

    @Override
    public ContractManager getContractManager() {
        return contractManager;
    }

    @Override
    public BlockChainManager<T> getBlockChainManager() {
        return blockChainManager;
    }

    @Override
    public List<BranchContract> getBranchContracts() {
        if (this.branchStore.getBranchContacts().isEmpty()) {
            return branch.getBranchContracts();
        } else {
            return this.branchStore.getBranchContacts();
        }
    }

    @Override
    public ReentrantLock getLock() {
        return lock;
    }

    @Override
    public boolean containBranchContract(ContractVersion contractVersion) {
        return getBranchContracts().stream().anyMatch(bc -> bc.getContractVersion().equals(contractVersion));
    }

    @Override
    public void close() {
        this.branchStore.close();
        this.blockChainManager.close();
        this.contractManager.close();
    }

    @Override
    public void addListener(BranchEventListener listener) {
        listenerList.add(listener);
    }

    @Override
    public void addListener(ContractEventListener listener) {
        contractEventListenerList.add(listener);
    }
}
