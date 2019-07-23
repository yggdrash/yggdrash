package io.yggdrash.core.blockchain;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.common.util.VerifierUtils;
import io.yggdrash.contract.core.ExecuteStatus;
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class BlockChainImpl<T, V> implements BlockChain<T, V> {
    private static final Logger log = LoggerFactory.getLogger(BlockChainImpl.class);

    private final List<BranchEventListener> listenerList = new ArrayList<>();

    private final Branch branch;
    private final ConsensusBlock<T> genesisBlock;
    private final Map<String, V> unConfirmedBlockMap = new ConcurrentHashMap<>();

    private final BlockChainManager<T> blockChainManager;
    private final ContractManager contractManager;
    private final BranchStore branchStore;

    private final Consensus consensus;
    private final ReentrantLock lock = new ReentrantLock();

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

    private void init() {
        // step1: branch contract check
        if (this.getBranchContracts().isEmpty()) {
            log.error("This branch {} has no any contract information.", getBranch().getBranchId());
        }

//        initContract();

        // getGenesis Block by Store
        Sha3Hash blockHash = branchStore.getGenesisBlockHash();
        if (blockHash == null || !blockChainManager.containsBlockHash(blockHash)) {
            log.debug("BlockChain init Genesis");
            initGenesis();
        } else {
            log.debug("BlockChain Load in Storage");
            // Load Block Chain Information
            loadTransaction();
            // load contract
        }
    }

    private void initContract() {

        for (BranchContract branchContract : this.getBranchContracts()) {
            ContractVersion contractVersion = branchContract.getContractVersion();
            // step2: file check
            if (!contractManager.isContractFileExist(contractVersion)) {
                log.info("{} contract does not exist. ", branchContract.getName());
                // step3. download file that does not exist.
                boolean isDownloaded = contractManager.downloader(contractVersion);
                if (!isDownloaded) {
                    log.error("Downloading contract version {} has an error occurred.", contractVersion);
                    continue;
                }
            }

            File contractFile = new File(contractManager.getContractPath() + File.separator + contractVersion + ".jar");

            // step3. verifying contract File
            boolean isVerified = contractManager.verifyContractFile(contractFile, contractVersion);
            if (!isVerified) {
                log.error("Verifying contract version {} has an error occurred.", contractVersion);
                contractManager.deleteContractFile(contractFile);
                continue;
            }

            // step4: install contract
            long result = contractManager.installContract(contractVersion, contractFile, branchContract.isSystem());
            if (result == -1) {
                log.error("Installing contract version {} has an error occurred", contractVersion);
            }
        }

        // step5. inject contract
        try {
            contractManager.reloadInject();
        } catch (IllegalAccessException e) {
            log.error(e.getMessage());
            throw new FailedOperationException("contract Inject Fail");
        }

    }

    private void initGenesis() {
        blockChainManager.initGenesis(genesisBlock);
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
            ConsensusBlock<T> block = blockChainManager.getBlockByIndex(i);
            // TODO node can be shutdown before blockStore.addBlock()
            // addBlock(): branchStore.setBestBlock() -> executeTransactions() -> blockStore.addBlock()
            if (block == null) {
                long prevIdx = i - 1;
                branchStore.setBestBlock(blockChainManager.getBlockByIndex(prevIdx));
                log.warn("reset branchStore bestBlock: {} -> {}", bestBlock, prevIdx);
                break;
            }
            blockChainManager.updateTxCache(block);
            // set Last Best Block
            if (i == bestBlock) {
                blockChainManager.setLastConfirmedBlock(block);
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
    public ConsensusBlock<T> getGenesisBlock() {
        return genesisBlock;
    }

    @Override
    public Map<String, V> getUnConfirmedData() {
        return unConfirmedBlockMap;
    }

    @Override
    public ConsensusBlock<T> addBlock(ConsensusBlock<T> nextBlock, boolean broadcast) {
        try {
            lock.lock();

            if (nextBlock.getIndex() != 0
                    && blockChainManager.getLastIndex() != 0
                    && nextBlock.getIndex() != blockChainManager.getLastIndex() + 1) {
                log.trace("Addblock() failed. LastIndex {}, nextBlockIndex {}",
                        blockChainManager.getLastIndex(),
                        nextBlock.getBlock().toJsonObject().toString());
                return null;
            }

            if (blockChainManager.verify(nextBlock) != BusinessError.VALID.toValue()) {
                log.trace("Addblock() failed. {}", nextBlock.getBlock().toJsonObject().toString());
                return null;
            }
            // add best Block
            branchStore.setBestBlock(nextBlock);

            // run Block Transactions
            // TODO run block execute move to other process (or thread)
            // TODO last execute block will invoke
            if (nextBlock.getIndex() > branchStore.getLastExecuteBlockIndex()) {
                BlockRuntimeResult result = contractManager.executeTxs(nextBlock); //TODO Exception
                // Save Result
                contractManager.commitBlockResult(result);
                branchStore.setLastExecuteBlock(nextBlock);
            }

            // BlockChainManager add nextBlock to the blockStore, set the lastConfirmedBlock to nextBlock,
            // and then batch the transactions.
            blockChainManager.addBlock(nextBlock);
            if (!listenerList.isEmpty() && broadcast) {
                listenerList.forEach(listener -> listener.chainedBlock(nextBlock));
            }
            nextBlock.loggingBlock();
        } finally {
            lock.unlock();
        }

        return nextBlock;
    }

    @Override
    public ConsensusBlock<T> addBlock(ConsensusBlock<T> block) {
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
            if (txResult.getReceipt().getStatus() != ExecuteStatus.ERROR) {
                blockChainManager.addTransaction(tx);

                if (!listenerList.isEmpty() && broadcast) {
                    listenerList.forEach(listener -> listener.receivedTransaction(tx));
                }

                return new HashMap<>();
            } else {
                Map<String, List<String>> applicationError = new HashMap<>();
                applicationError.put("SystemError", txResult.getReceipt().getTxLog());
                return applicationError;
            }
        } else {
            return BusinessError.getErrorLogsMap(verifyResult);
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
//            branchStore.setBranchContracts(branch.getBranchContracts());
            return branch.getBranchContracts();
        } else {
            return this.branchStore.getBranchContacts();
        }
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
}
