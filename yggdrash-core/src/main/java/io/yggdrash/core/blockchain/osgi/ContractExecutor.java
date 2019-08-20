package io.yggdrash.core.blockchain.osgi;

import com.google.gson.JsonObject;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptAdapter;
import io.yggdrash.contract.core.TransactionReceiptImpl;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractChannelField;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.InjectEvent;
import io.yggdrash.contract.core.channel.ContractMethodType;
import io.yggdrash.core.blockchain.Log;
import io.yggdrash.core.blockchain.LogIndexer;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.osgi.service.VersioningContract;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.errorcode.SystemError;
import io.yggdrash.core.runtime.result.BlockRuntimeResult;
import io.yggdrash.core.runtime.result.TransactionRuntimeResult;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.LogStore;
import io.yggdrash.core.store.StoreAdapter;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.apache.commons.codec.binary.Base64;
import org.osgi.framework.Bundle;
import org.osgi.framework.launch.Framework;
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

    private final Framework framework;
    private final ContractStore contractStore;

    private final SystemProperties systemProperties;
    private final ContractCacheImpl contractCache;
    private TransactionReceiptAdapter trAdapter;
    private final LogIndexer logIndexer;
    private ContractChannelCoupler coupler;

    private final ReentrantLock locker = new ReentrantLock();
    private final Condition isBlockExecuting = locker.newCondition();

    private boolean isTx = false;


    ContractExecutor(Framework framework, ContractStore contractStore, SystemProperties systemProperties,
                     LogStore logStore) {
        this.framework = framework;
        this.contractStore = contractStore;
        this.systemProperties = systemProperties;
        this.logIndexer = new LogIndexer(logStore, contractStore.getTransactionReceiptStore());
        contractCache = new ContractCacheImpl();
        trAdapter = new TransactionReceiptAdapter();
        coupler = new ContractChannelCoupler();
    }

    Log getLog(long index) {
        return logIndexer.getLog(index);
    }

    List<Log> getLogs(long start, long offset) {
        return logIndexer.getLogs(start, offset);
    }

    long getCurLogIndex() {
        return logIndexer.curIndex();
    }

    void injectFields(Bundle bundle, Object service, boolean isSystemContract)
            throws IllegalAccessException {

        Field[] fields = service.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);

            for (Annotation annotation : field.getDeclaredAnnotations()) {
                if (annotation.annotationType().equals(ContractStateStore.class)) {
                    String bundleSymbolicName = bundle.getSymbolicName();
                    byte[] bundleSymbolicSha3 = HashUtil.sha3omit12(bundleSymbolicName.getBytes());
                    String nameSpace = new String(Base64.encodeBase64(bundleSymbolicSha3));
                    log.debug("bundleSymbolicName {} , nameSpace {}", bundleSymbolicName, nameSpace);
                    StoreAdapter adapterStore = new StoreAdapter(contractStore.getTmpStateStore(), nameSpace);
                    field.set(service, adapterStore); //default => tmpStateStore
                }

                if (isSystemContract && annotation.annotationType().equals(ContractBranchStateStore.class)) {
                    field.set(service, contractStore.getBranchStore());
                }

                if (annotation.annotationType().equals(ContractTransactionReceipt.class)) {
                    field.set(service, trAdapter);
                }

                if (annotation.annotationType().equals(ContractChannelField.class)) {
                    field.set(service, coupler);
                }

                if (systemProperties != null
                        && annotation.annotationType().equals(InjectEvent.class)
                        && field.getType().isAssignableFrom(systemProperties.getEventStore().getClass())) {
                    field.set(service, systemProperties.getEventStore());
                }
            }
        }

        contractCache.cacheContract(bundle.getLocation(), service);
    }

    private Object callContractMethod(String contractVersion, Object service, String methodName, JsonObject params,
                                      ContractMethodType methodType, TransactionReceipt txReceipt,
                                      JsonObject endBlockParams) {

        //temporary
        Map<String, Method> methodMap = contractCache.getContractMethodMap(
                String.format("%s/%s", ContractConstants.SUFFIX_SYSTEM_CONTRACT, contractVersion), methodType);

        if (methodMap == null || methodMap.get(methodName) == null) {
            txReceipt.setStatus(ExecuteStatus.ERROR);
            txReceipt.addLog("Method Type is not exist");
            return null;
        }

        Method method = methodMap.get(methodName);
        try {
            if (methodType == ContractMethodType.INVOKE) {
                //
                trAdapter.setTransactionReceipt(txReceipt);
            }

            if (method.getParameterCount() == 0) {
                return method.invoke(service);
            } else {
                if (methodType == ContractMethodType.END_BLOCK) {
                    return method.invoke(service, endBlockParams);
                } else {
                    return method.invoke(service, params);
                }
            }
        } catch (IllegalAccessException e) {
            log.error("CallContractMethod : {} and bundle {} ", methodName, contractVersion);
        } catch (InvocationTargetException e) {
            log.debug("CallContractMethod ApplicationErrorLog : {}", e.getCause().toString());
            trAdapter.addLog(e.getCause().getMessage());

            if (log.isDebugEnabled()) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public Object query(String contractVersion, Object service, String methodName, JsonObject params) {

        return callContractMethod(
                contractVersion, service, methodName, params, ContractMethodType.QUERY, null, null);
    }

    private Set<Map.Entry<String, JsonObject>> invoke(
            String contractVersion, Object service, JsonObject txBody, TransactionReceipt txReceipt) {

        callContractMethod(contractVersion, service, txBody.get("method").getAsString(),
                txBody.getAsJsonObject("params"), ContractMethodType.INVOKE, txReceipt, null);
        return contractStore.getTmpStateStore().changeValues();
    }

    // TODO fix End Block call by execution
    private List<Object> endBlock(String location, Object service, JsonObject endBlockParams) {
        List<Object> results = new ArrayList<>();
        for (Bundle bundle : framework.getBundleContext().getBundles()) {
            contractCache.cacheContract(location, service);
            // TODO change contract version
            Map<String, Method> endBlockMethods = contractCache.getEndBlockMethods().get(bundle.getLocation());
            if (endBlockMethods != null) {
                endBlockMethods.forEach((k, m) -> {
                    Object result = callContractMethod(
                            location, service, k, null, ContractMethodType.END_BLOCK, null, endBlockParams);
                    if (result != null) {
                        results.add(result);
                    }
                });
            }
        }

        return results;
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

        TransactionReceipt txReceipt = createTransactionReceipt(tx);
        TransactionRuntimeResult txRuntimeResult = new TransactionRuntimeResult(tx);

        JsonObject txBody = tx.getBody().getBody();
        String contractVersion = txBody.get("contractVersion").getAsString();
        Object service = serviceMap.get(contractVersion);

        if (service != null) {
            txRuntimeResult.setChangeValues(invoke(contractVersion, service, txBody, txReceipt));
        } else {
            txReceipt.setStatus(ExecuteStatus.ERROR);
            txReceipt.addLog(SystemError.CONTRACT_VERSION_NOT_FOUND.toString());
        }
        txRuntimeResult.setTransactionReceipt(txReceipt);
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
            TransactionReceipt txReceipt = createTransactionReceipt(tx);

            txReceipt.setBlockId(nextBlock.getHash().toString());
            txReceipt.setBlockHeight(nextBlock.getIndex());
            txReceipt.setBranchId(nextBlock.getBranchId().toString());

            JsonObject txBody = tx.getBody().getBody();
            String contractVersion = txBody.get("contractVersion").getAsString();
            Object service = serviceMap.get(contractVersion);

            if (service != null) {
                blockRuntimeResult.setBlockResult(invoke(contractVersion, service, txBody, txReceipt));
            } else {
                txReceipt.setStatus(ExecuteStatus.ERROR);
                txReceipt.addLog(SystemError.CONTRACT_VERSION_NOT_FOUND.toString());
            }

            blockRuntimeResult.addTxReceipt(txReceipt);
            log.debug("{} : {}", txReceipt.getTxId(), txReceipt.isSuccess());
        }
        contractStore.getTmpStateStore().close(); // clear(revert) tmpStateStore
        locker.unlock();
        return blockRuntimeResult;
    }

    void commitBlockResult(BlockRuntimeResult result) {
        locker.lock();
        // TODO store transaction by batch
        Map<String, JsonObject> changes = result.getBlockResult();
        TransactionReceiptStore transactionReceiptStore = contractStore.getTransactionReceiptStore();
        result.getTxReceipts().forEach(transactionReceiptStore::put);
        result.getTxReceipts().forEach(receipt -> logIndexer.put(receipt.getTxId(), receipt.getTxLog().size()));
        if (!changes.isEmpty()) {
            StateStore stateStore = contractStore.getStateStore();
            changes.forEach(stateStore::put);
        }
        isTx = true;
        isBlockExecuting.signal();
        locker.unlock();
        // TODO make transaction Receipt Event
    }

    private static TransactionReceipt createTransactionReceipt(Transaction tx) {
        String txId = tx.getHash().toString();
        long txSize = tx.getBody().getLength();
        String issuer = tx.getAddress().toString();
        String contractVersion = tx.getBody().getBody().get("contractVersion").getAsString();

        return new TransactionReceiptImpl(txId, txSize, issuer, contractVersion);
    }


    public TransactionRuntimeResult versioningService(Transaction tx) throws IllegalAccessException {
        VersioningContract service = new VersioningContract();

        locker.lock();
        while (!isTx) {
            try {
                isBlockExecuting.await();
            } catch (InterruptedException e) {
                log.warn("executeTx err : {} ", e.getMessage());
            }
        }
        isTx = true;

        TransactionReceipt txReceipt = createTransactionReceipt(tx);
        TransactionRuntimeResult txRuntimeResult = new TransactionRuntimeResult(tx);

        // inject

        Field[] fields = service.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            for (Annotation annotation : field.getDeclaredAnnotations()) {
                if (annotation.annotationType().equals(ContractStateStore.class)) {
                    StoreAdapter adapterStore = new StoreAdapter(contractStore.getStateStore(), "versioning");
                    field.set(service, adapterStore); //default => tmpStateStore
                }
                if (annotation.annotationType().equals(ContractTransactionReceipt.class)) {
                    field.set(service, trAdapter);
                }

                if (annotation.annotationType().equals(ContractBranchStateStore.class)) {
                    field.set(service, contractStore.getBranchStore());
                }
            }
        }

        JsonObject txBody = tx.getBody().getBody();
        String contractVersion = txBody.get("contractVersion").getAsString();
        String methodName = txBody.get("method").getAsString();

        contractCache.cacheContract(contractVersion, service);

        Method method = contractCache.getContractMethodMap(contractVersion, ContractMethodType.INVOKE).get(methodName);

        trAdapter.setTransactionReceipt(txReceipt);

        try {
            method.invoke(service, txBody);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        txRuntimeResult.setChangeValues(contractStore.getTmpStateStore().changeValues());

        txRuntimeResult.setTransactionReceipt(txReceipt);
        contractStore.getTmpStateStore().close();
        locker.unlock();
        return txRuntimeResult;
    }
}
