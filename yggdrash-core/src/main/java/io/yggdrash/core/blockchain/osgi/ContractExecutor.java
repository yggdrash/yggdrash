package io.yggdrash.core.blockchain.osgi;

import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.ReceiptAdapter;
import io.yggdrash.contract.core.ReceiptImpl;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractChannelField;
import io.yggdrash.contract.core.annotation.ContractReceipt;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.channel.ContractMethodType;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.LogIndexer;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.errorcode.SystemError;
import io.yggdrash.core.runtime.result.BlockRuntimeResult;
import io.yggdrash.core.runtime.result.TransactionRuntimeResult;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.ReceiptStore;
import io.yggdrash.core.store.StoreAdapter;
import io.yggdrash.core.store.TempStateStore;
import org.apache.commons.codec.binary.Base64;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class ContractExecutor {
    private static final Logger log = LoggerFactory.getLogger(ContractExecutor.class);

    private static final String CONTACT_VERSION = "contractVersion";

    private final ContractStore contractStore;

    private final ContractCacheImpl contractCache;
    private ReceiptAdapter trAdapter;
    private List<StoreAdapter> storeAdapterList;

    private TempStateStore tmpStateStore;
    private TempStateStore pendingStateStore;

    private final LogIndexer logIndexer;
    private ContractChannelCoupler coupler;

    private final ReentrantLock locker = new ReentrantLock();
    private final Condition isBlockExecuting = locker.newCondition();
    private boolean isTx = false;

    ContractExecutor(ContractStore contractStore, LogIndexer logIndexer) {
        this.contractStore = contractStore;
        this.logIndexer = logIndexer;
        this.contractCache = new ContractCacheImpl();
        this.trAdapter = new ReceiptAdapter();
        this.storeAdapterList = new ArrayList<>();
        this.tmpStateStore = contractStore.getTmpStateStore();
        this.pendingStateStore = contractStore.getPendingStateStore();
        this.coupler = new ContractChannelCoupler();
    }

    void injectNodeContract(Object service) {
        inject(service, namespace(service.getClass().getName()));
    }

    void injectBundleContract(Bundle bundle, Object service) {
        inject(service, namespace(bundle.getSymbolicName()));
    }

    private void inject(Object service, String namespace) {
        Field[] fields = service.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);

            for (Annotation annotation : field.getDeclaredAnnotations()) {
                try {
                    if (annotation.annotationType().equals(ContractStateStore.class)) {
                        log.trace("service name : {} \t namespace : {}", service.getClass().getName(), namespace);
                        StoreAdapter storeAdapter = new StoreAdapter(tmpStateStore, namespace);
                        storeAdapterList.add(storeAdapter);
                        field.set(service, storeAdapter); //default => tmpStateStore
                    }

                    if (annotation.annotationType().equals(ContractBranchStateStore.class)) {
                        field.set(service, contractStore.getBranchStore());
                    }

                    if (annotation.annotationType().equals(ContractReceipt.class)) {
                        field.set(service, trAdapter);
                    }

                    if (annotation.annotationType().equals(ContractChannelField.class)) {
                        field.set(service, coupler);
                    }
                    // todo : Implements event store policy. 190814 - lucas

                } catch (IllegalAccessException e) {
                    log.warn(e.getMessage());
                }
            }
        }
    }

    private String namespace(String name) {
        byte[] bundleSymbolicSha3 = HashUtil.sha3omit12(name.getBytes());
        return new String(Base64.encodeBase64(bundleSymbolicSha3));
    }

    Object query(Map<String, Object> serviceMap, String contractVersion, String methodName, JsonObject params) {
        try {
            Object service = getService(serviceMap, contractVersion);
            Method method = getMethod(service, contractVersion, ContractMethodType.QUERY, methodName);

            return invokeMethod(service, method, params);
        } catch (Exception e) {
            log.error("Query failed. {}", e.getMessage());
        }

        return null;
    }

    private TransactionRuntimeResult getTransactionRuntimeResult(Map<String, Object> serviceMap, Transaction tx, TempStateStore curTmpStateStore) {
        locker.lock();
        while (!isTx) {
            try {
                isBlockExecuting.await();
            } catch (InterruptedException e) {
                log.warn("executeTx err : {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        isTx = true;

        setStoreAdapter(curTmpStateStore);

        TransactionRuntimeResult txRuntimeResult = new TransactionRuntimeResult(tx);
        Receipt receipt = createReceipt(tx, null);
        Set<Map.Entry<String, JsonObject>> result = null;
        try {
            result = invokeTx(serviceMap, tx, receipt);
        } catch (ExecutorException e) {
            exceptionHandler(e, receipt);
        }

        if (result != null) {
            txRuntimeResult.setChangeValues(result);
        }

        txRuntimeResult.setReceipt(receipt);

        // PendingStateStore keeps running without closing. It is only reset when a block is added.
        if (curTmpStateStore.equals(tmpStateStore)) {
            curTmpStateStore.close();
        }

        locker.unlock();
        return txRuntimeResult;
    }

    boolean executePendingTx(Map<String, Object> serviceMap, Transaction tx) {
        TransactionRuntimeResult transactionRuntimeResult = getTransactionRuntimeResult(serviceMap, tx, pendingStateStore);
        return !transactionRuntimeResult.getReceipt().getStatus().equals(ExecuteStatus.ERROR);
    }

    TransactionRuntimeResult executeTx(Map<String, Object> serviceMap, Transaction tx) {
        return getTransactionRuntimeResult(serviceMap, tx, tmpStateStore);
    }

    Set<Sha3Hash> executePendingTxs(Map<String, Object> serviceMap, List<Transaction> txs) { //executeTxs 처럼 executeTx 하는동안 피해야함
        BlockRuntimeResult blockRuntimeResult = getBlockRuntimeResult(new BlockRuntimeResult(txs), serviceMap);
        Set<Sha3Hash> errPendingTxs = blockRuntimeResult.getReceipts().stream()
                .filter(receipt -> receipt.getStatus().equals(ExecuteStatus.ERROR))
                .map(receipt -> new Sha3Hash(receipt.getTxId()))
                .collect(Collectors.toSet());
        return errPendingTxs;
    }

    BlockRuntimeResult executeTxs(Map<String, Object> serviceMap, ConsensusBlock nextBlock) {
        if (nextBlock.getIndex() == 0) {
            //TODO first transaction is genesis
            //TODO init method don't call any more
            //@Genesis check
        }
        return getBlockRuntimeResult(new BlockRuntimeResult(nextBlock), serviceMap);
    }

    private BlockRuntimeResult getBlockRuntimeResult(BlockRuntimeResult blockRuntimeResult, Map<String, Object> serviceMap) {
        locker.lock();
        isTx = false;
        // Set Coupler Contract and contractCache
        coupler.setContract(serviceMap, contractCache);

        ConsensusBlock nextBlock = blockRuntimeResult.getOriginBlock();
        List<Transaction> txList = nextBlock != null ? nextBlock.getBody().getTransactionList() : blockRuntimeResult.getTxList();
        TempStateStore curTmpStateStore = nextBlock != null ? tmpStateStore : pendingStateStore;

        setStoreAdapter(curTmpStateStore);

        for (Transaction tx : txList) {
            // get all exceptions
            Receipt receipt = createReceipt(tx, nextBlock);

            Set<Map.Entry<String, JsonObject>> result = null;
            try {
                result = invokeTx(serviceMap, tx, receipt);
            } catch (ExecutorException e) {
                exceptionHandler(e, receipt);
            }

            blockRuntimeResult.addReceipt(receipt);
            if (!receipt.getStatus().equals(ExecuteStatus.ERROR)) {
                blockRuntimeResult.setBlockResult(result);
            } else {
                log.warn("Error TxId={}, TxLog={}", receipt.getTxId(), receipt.getLog());
            }
        }

        // PendingStateStore keeps running without closing. It is only reset when a block is added.
        if (curTmpStateStore.equals(tmpStateStore)) {
            curTmpStateStore.close();
        } else {
            // CommitBlockResult will not run after executing pending txs, so isTx has to be set manually.
            isTx = true;
        }

        locker.unlock();
        return blockRuntimeResult;
    }

    BlockRuntimeResult endBlock(Map<String, Object> serviceMap, ConsensusBlock addedBlock) {
        BlockRuntimeResult result = new BlockRuntimeResult(addedBlock);
        int i = 0;
        Set<Map.Entry<String, JsonObject>> changedValues;
        for (String contractVersion : serviceMap.keySet()) {
            Receipt receipt = createReceipt(addedBlock, i);
            Object service = serviceMap.get(contractVersion);
            List<Method> values = new ArrayList<>(contractCache
                    .getContractMethodMap(contractVersion, ContractMethodType.END_BLOCK, service)
                    .values());
            if (!values.isEmpty()) {
                // Each contract has only one endBlock method
                Method method = values.get(0);
                receipt.setContractVersion(contractVersion);
                changedValues = invokeMethod(receipt, service, method, new JsonObject());

                if (!receipt.getStatus().equals(ExecuteStatus.ERROR) && changedValues != null) {
                    result.setBlockResult(changedValues);
                }

                result.addReceipt(receipt);
                i++;
            }
        }
        contractStore.getTmpStateStore().close(); // clear(revert) tmpStateStore
        return result;
    }

    private Set<Map.Entry<String, JsonObject>> invokeTx(Map<String, Object> serviceMap, Transaction tx, Receipt receipt) throws ExecutorException {
        JsonObject txBody = tx.getBody().getBody();

        String contractVersion = txBody.get(CONTACT_VERSION).getAsString();

        String methodName = txBody.get("method").getAsString();
        JsonObject params = txBody.getAsJsonObject("params");

        Object service = getService(serviceMap, contractVersion);
        Method method = getMethod(service, contractVersion, ContractMethodType.INVOKE, methodName);
        return invokeMethod(receipt, service, method, params);
    }

    private Method getMethod(Object service, String contractVersion, ContractMethodType methodType, String methodName) throws ExecutorException {
        Method method = contractCache.getContractMethodMap(contractVersion, methodType, service).get(methodName);

        if (method == null) {
            log.error("Not found contract method: {}", methodName);
            throw new ExecutorException(SystemError.CONTRACT_METHOD_NOT_FOUND);
        }

        return method;
    }

    private Object getService(Map<String, Object> serviceMap, String contractVersion) throws ExecutorException {
        Object service = serviceMap.get(contractVersion);

        if (service == null) {
            log.error("This service that contract version {} is not registered", contractVersion);
            throw new ExecutorException(SystemError.CONTRACT_VERSION_NOT_FOUND);
        }

        return service;
    }

    private Object invokeMethod(Object service, Method method, JsonObject params) throws Exception {
        return method.getParameterCount() == 0 ? method.invoke(service) : method.invoke(service, params);
    }

    private Set<Map.Entry<String, JsonObject>> invokeMethod(Receipt receipt, Object service, Method method, JsonObject params) { //=> getRuntimeResult
        trAdapter.setReceipt(receipt);

        try {
            invokeMethod(service, method, params);
        } catch (InvocationTargetException e) {
            log.error("Invoke method error in tx id : {} caused by {}", receipt.getTxId(), e.getCause());
            trAdapter.addLog(e.getCause().getMessage());
        } catch (Exception e) {
            log.error("Invoke failed. {}", e.getMessage());
        }

        return contractStore.getTmpStateStore().changeValues();
    }

    void commitBlockResult(BlockRuntimeResult result) {
        locker.lock();
        ReceiptStore receiptStore = contractStore.getReceiptStore();

        String versioningContractVersion = "0000000000000001";
        for (Receipt receipt : result.getReceipts()) {
            if (receipt.getContractVersion().equals(versioningContractVersion)) { //VersioningContract
                // Store receipt and logs
                receiptStore.put(receipt.getBlockId(), receipt); // endBlock
                logIndexer.put(receipt.getBlockId(), receipt.getLog().size());

                // TODO event 발생은 blockChainImpl 에서 !! -> blockChainImpl
            } else {
                // Store receipt and logs
                receiptStore.put(receipt.getTxId(), receipt);
                logIndexer.put(receipt.getTxId(), receipt.getLog().size());
            }

            // Reflect changed values
            Map<String, JsonObject> changes = result.getBlockResult();
            if (!changes.isEmpty()) {
                StateStore stateStore = contractStore.getStateStore();
                changes.forEach(stateStore::put);
            }

        }
        isTx = true;
        isBlockExecuting.signal();
        locker.unlock();
    }

    private void setStoreAdapter(TempStateStore curTmpStateStore) {
        storeAdapterList.forEach(storeAdapter -> storeAdapter.setStateStore(curTmpStateStore));
    }

    private static Receipt createReceipt(ConsensusBlock consensusBlock, int index) { //for endBlock
        Block block = consensusBlock.getBlock();
        String issuer = block.getAddress().toString();
        String branchId = block.getBranchId().toString();
        String blockId = String.format("%s%d", block.getHash().toString(), index);
        long blockSize = block.getLength();
        long blockHeight = block.getIndex();

        return new ReceiptImpl(issuer, branchId, blockId, blockSize, blockHeight);
    }

    private Receipt createReceipt(Transaction tx, ConsensusBlock block) {
        String txId = tx.getHash().toString();
        long txSize = tx.getBody().getLength();
        String issuer = tx.getAddress().toString();

        Receipt receipt;
        if (tx.getBody().getBody().get(CONTACT_VERSION) == null) {
            receipt = new ReceiptImpl(txId, txSize, issuer);
        } else {
            receipt = new ReceiptImpl(txId, txSize, issuer,
                    tx.getBody().getBody().get(CONTACT_VERSION).getAsString());
        }

        if (block != null) {
            receipt.setBlockId(block.getHash().toString());
            receipt.setBlockHeight(block.getIndex());
        } else {
            receipt.setBlockHeight(contractStore.getBranchStore().getLastExecuteBlockIndex());
        }

        receipt.setBranchId(tx.getBranchId().toString());

        return receipt;
    }

    private void exceptionHandler(ExecutorException e, Receipt receipt) {
        SystemError error = e.getCode();
        receipt.setStatus(ExecuteStatus.ERROR);
        switch (error) {
            case CONTRACT_VERSION_NOT_FOUND:
                receipt.addLog(SystemError.CONTRACT_VERSION_NOT_FOUND.toString());
                break;
            case CONTRACT_METHOD_NOT_FOUND:
                receipt.addLog(SystemError.CONTRACT_METHOD_NOT_FOUND.toString());
                break;
            default:
                log.error(e.getMessage());
                break;
        }
    }

}
