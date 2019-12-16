package io.yggdrash.core.blockchain;

import com.google.gson.JsonObject;
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
            blockChainManager.loadTransaction();
            log.debug("Current StateRoot -> ", contractManager.getOriginStateRoot().toString());
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

            if (isLastExecutedBlock(nextBlock)) {
                log.info("Block[{}] has already been executed. Save it directly to blockStore.", nextBlock.getIndex());
                branchStore.setLastExecuteBlock(nextBlock);
                blockChainManager.addBlock(nextBlock);
                return new HashMap<>();
            }

            // Run Block Transactions
            // TODO run block execute move to other process (or thread)
            // TODO last execute block will invoke

            // Execute block and commit the result of block.
            BlockRuntimeResult blockResult = contractManager.executeTxs(nextBlock); //TODO Exception
            Sha3Hash nextBlockStateRoot = new Sha3Hash(nextBlock.getHeader().getStateRoot(), true);
            // Validate StateRoot
            Sha3Hash blockResultStateRoot = blockResult.getBlockResult().size() > 0
                    ? new Sha3Hash(blockResult.getBlockResult().get("stateRoot").get("stateHash").getAsString())
                    : contractManager.getOriginStateRootHash();
            if (!nextBlockStateRoot.equals(blockResultStateRoot)) {
                log.warn("Add block failed. Invalid stateRoot. BlockStateRoot : {}, CurStateRoot : {}",
                        nextBlockStateRoot, blockResultStateRoot);
                return BusinessError.getErrorLogsMap(BusinessError.INVALID_STATE_ROOT_HASH.toValue());
            }

            branchStore.setLastExecuteBlock(nextBlock);
            // Add best Block
            branchStore.setBestBlock(nextBlock); // It can be sure that all Txs in BestBlock have been executed.
            contractManager.commitBlockResult(blockResult);
            // BlockChainManager add nextBlock to the blockStore, set the lastConfirmedBlock to nextBlock,
            // and then batch the transactions.
            blockChainManager.addBlock(nextBlock);

            // Fire contract event
            getContractEventList(blockResult).stream()
                    .filter(event -> !contractEventListenerList.isEmpty())
                    .forEach(event -> contractEventListenerList.forEach(l -> l.endBlock(event)));

            if (!listenerList.isEmpty() && broadcast) {
                listenerList.forEach(listener -> listener.chainedBlock(nextBlock));
            }

            nextBlock.loggingBlock(this.blockChainManager.getUnconfirmedTxSize());
        } catch (Exception e) {
            log.debug("Add block failed. {}", e.getMessage()); //TODO Exception handling
        } finally {
            lock.unlock();
        }

        return new HashMap<>();
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

    @Override
    public Map<String, List<String>> addTransaction(Transaction tx, boolean broadcast) {
        log.trace("addTransaction: tx={}", tx.getHash().toString());
        int verifyResult = blockChainManager.verify(tx);
        if (verifyResult == BusinessError.VALID.toValue()) {
            log.trace("contractManager.executeTx: {}", tx.getHash().toString());
            TransactionRuntimeResult txResult = contractManager.executeTx(tx); //checkTx
            log.trace("contractManager.executeTx Result: {}", txResult.getReceipt().getLog());
            if (txResult.getReceipt().getStatus() == ExecuteStatus.SUCCESS) {
                blockChainManager.addTransaction(tx);
                // TODO Filter broadcast
                if (!listenerList.isEmpty() && broadcast) {
                    listenerList.forEach(listener -> listener.receivedTransaction(tx));
                } else {
                    log.trace("addTransaction(): queuing broadcast is failed. listener={} broadcast={}",
                            listenerList.size(), broadcast);
                }

                return new HashMap<>();
            } else {
                Map<String, List<String>> applicationError = new HashMap<>();
                applicationError.put("SystemError", txResult.getReceipt().getLog());
                log.trace("addTransaction(): executeTx() is failed. {}", txResult.getReceipt().getLog());
                return applicationError;
            }
        } else {
            log.trace("addTransaction(): verify() is failed. tx={} {}",
                    tx.getHash().toString(), BusinessError.getErrorLogsMap(verifyResult));
            return BusinessError.getErrorLogsMap(verifyResult);
        }
    }

    private Map<String, List<String>> addTransactionByTransactionConsumer(Transaction tx, boolean broadcast) {
        if (blockChainManager.contains(tx)) {
            return BusinessError.getErrorLogsMap(BusinessError.DUPLICATED.toValue());
        }

        Receipt receipt = contractManager.checkTx(tx);
        if (receipt.getStatus() != ExecuteStatus.SUCCESS) {
            Map<String, List<String>> applicationError = new HashMap<>();
            applicationError.put("ApplicationError", receipt.getLog());
            return applicationError;
        }

        blockChainManager.addTransaction(tx); //Common process
        if (broadcast) {
            fireReceivedTxEvent(tx);
        }
        return new HashMap<>();
    }

    private void fireReceivedTxEvent(Transaction tx) {
        if (!listenerList.isEmpty()) {
            listenerList.forEach(listener -> listener.receivedTransaction(tx));
        } else {
            log.trace("[ReceivedTransactionEvent] queuing broadcast is failed. listener={} ", listenerList.size());
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

    private boolean isLastExecutedBlock(ConsensusBlock<T> nextBlock) {
        if (branchStore.getLastExecuteBlockIndex() >= nextBlock.getIndex()) {
            log.info("IsLastExecutedBlock? lastExecutedBlock: {} >= nextBlockIndex: {}",
                    branchStore.getLastExecuteBlockIndex(), nextBlock.getIndex());

            Sha3Hash nextBlockStateRoot = new Sha3Hash(nextBlock.getHeader().getStateRoot(), true);
            JsonObject originStateRoot = contractManager.getOriginStateRoot();
            Sha3Hash stateRootHash = new Sha3Hash(originStateRoot.get("stateHash").getAsString());
            long blockHeight = originStateRoot.get("blockHeight").getAsLong();
            return nextBlock.getIndex() == blockHeight && nextBlockStateRoot.equals(stateRootHash);
        }
        return false;
    }

    private List<ContractEvent> getContractEventList(BlockRuntimeResult result) {
        List<ContractEvent> contractEventList = new ArrayList<>();
        result.getReceipts().stream()
                .filter(receipt -> !receipt.getEvents().isEmpty())
                .map(Receipt::getEvents)
                .forEach(contractEventList::addAll);
        return contractEventList;
    }

}
