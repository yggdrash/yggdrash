package io.yggdrash.core.blockchain.osgi;

import com.google.gson.JsonObject;
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

public class ContractExecutor {
    private static final Logger log = LoggerFactory.getLogger(ContractExecutor.class);

    private static final String CONTACT_VERSION = "contractVersion";

    private final ContractStore contractStore;

    private final ContractCacheImpl contractCache;
    private ReceiptAdapter trAdapter;
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
                        StoreAdapter adapterStore = new StoreAdapter(contractStore.getTmpStateStore(), namespace);
                        field.set(service, adapterStore); //default => tmpStateStore
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

    TransactionRuntimeResult executeTx(Map<String, Object> serviceMap, Transaction tx) {
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

        Receipt receipt = createReceipt(tx);
        receipt.setBlockHeight(contractStore.getBranchStore().getLastExecuteBlockIndex());

        TransactionRuntimeResult txRuntimeResult = new TransactionRuntimeResult(tx);

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
        contractStore.getTmpStateStore().close(); // clear(revert) tmpStateStore
        locker.unlock();
        return txRuntimeResult;
    }

    BlockRuntimeResult executeTxs(Map<String, Object> serviceMap, ConsensusBlock nextBlock) {
        locker.lock();
        isTx = false;
        // Set Coupler Contract and contractCache
        coupler.setContract(serviceMap, contractCache);

        List<Transaction> txList = nextBlock.getBody().getTransactionList();

        if (nextBlock.getIndex() == 0) {
            //TODO first transaction is genesis
            //TODO init method don't call any more
            //@Genesis check
        }

        BlockRuntimeResult blockRuntimeResult = new BlockRuntimeResult(nextBlock);

        for (Transaction tx : txList) {

            // get all exceptions
            Receipt receipt = createReceipt(tx);
            receipt.setBlockId(nextBlock.getHash().toString());
            receipt.setBlockHeight(nextBlock.getIndex());
            receipt.setBranchId(nextBlock.getBranchId().toString());

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

        contractStore.getTmpStateStore().close(); // clear(revert) tmpStateStore
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
            List<Method> methods = new ArrayList<>(contractCache
                    .getContractMethodMap(contractVersion, ContractMethodType.END_BLOCK, service)
                    .values());
            if (!methods.isEmpty()) {
                // Each contract has only one endBlock method
                Method method = methods.get(0);
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

        for (Receipt receipt : result.getReceipts()) {
            /* todo : Implements better way how to distinguish receipt between blocks and transaction. @lucase, 190904 */
            if (receipt.getTxId() == null) {
                // case 1. store block receipt
                receiptStore.put(receipt.getBlockId(), receipt); // endBlock
                logIndexer.put(receipt.getBlockId(), receipt.getLog().size());
            } else {
                // case 2. store tx receipt
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

    private static Receipt createReceipt(ConsensusBlock consensusBlock, int index) { //for endBlock
        Block block = consensusBlock.getBlock();
        String issuer = block.getAddress().toString();
        String branchId = block.getBranchId().toString();
        String blockId = String.format("%s%d", block.getHash().toString(), index);
        long blockSize = block.getLength();
        long blockHeight = block.getIndex();

        return new ReceiptImpl(issuer, branchId, blockId, blockSize, blockHeight);
    }

    private static Receipt createReceipt(Transaction tx) {
        String txId = tx.getHash().toString();
        long txSize = tx.getBody().getLength();
        String issuer = tx.getAddress().toString();

        if (tx.getBody().getBody().get(CONTACT_VERSION) == null) {
            return new ReceiptImpl(txId, txSize, issuer);
        }
        return new ReceiptImpl(txId, txSize, issuer,
                tx.getBody().getBody().get(CONTACT_VERSION).getAsString());
    }

    private void exceptionHandler(ExecutorException e, Receipt receipt) {
        SystemError error = e.getCode();
        switch (error) {
            case CONTRACT_VERSION_NOT_FOUND:
                receipt.setStatus(ExecuteStatus.ERROR);
                receipt.addLog(SystemError.CONTRACT_VERSION_NOT_FOUND.toString());
                break;
            case CONTRACT_METHOD_NOT_FOUND:
                receipt.setStatus(ExecuteStatus.ERROR);
                receipt.addLog(SystemError.CONTRACT_METHOD_NOT_FOUND.toString());
                break;
            default:
                log.error(e.getMessage());
                break;
        }
    }

}
