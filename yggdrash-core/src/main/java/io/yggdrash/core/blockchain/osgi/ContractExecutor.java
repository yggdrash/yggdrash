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
import io.yggdrash.contract.core.channel.ContractMethodType;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Log;
import io.yggdrash.core.blockchain.LogIndexer;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.errorcode.SystemError;
import io.yggdrash.core.runtime.result.BlockRuntimeResult;
import io.yggdrash.core.runtime.result.TransactionRuntimeResult;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.LogStore;
import io.yggdrash.core.store.StoreAdapter;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

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

    private final ContractStore contractStore;

    private final ContractCacheImpl contractCache;
    private TransactionReceiptAdapter trAdapter;
    private final LogIndexer logIndexer;
    private ContractChannelCoupler coupler;

    private final ReentrantLock locker = new ReentrantLock();
    private final Condition isBlockExecuting = locker.newCondition();
    private boolean isTx = false;

    ContractExecutor(ContractStore contractStore, LogStore logStore) {
        this.contractStore = contractStore;
        this.logIndexer = new LogIndexer(logStore, contractStore.getTransactionReceiptStore());
        this.contractCache = new ContractCacheImpl();
        this.trAdapter = new TransactionReceiptAdapter();
        this.coupler = new ContractChannelCoupler();
    }

    // TODO Implements endBlock Executions

    Log getLog(long index) {
        return logIndexer.getLog(index);
    }

    List<Log> getLogs(long start, long offset) {
        return logIndexer.getLogs(start, offset);
    }

    long getCurLogIndex() {
        return logIndexer.curIndex();
    }

    public void injectField(ArrayList<Object> services) throws IllegalAccessException {

        for (Object service : services) {
            Field[] fields = service.getClass().getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);

                for (Annotation annotation : field.getDeclaredAnnotations()) {
                    if (annotation.annotationType().equals(ContractStateStore.class)) {
                        String nameSpace = namespace(service.getClass().getName());
                        log.debug("serviceName {} , nameSpace {}", service.getClass().getName(), nameSpace);
                        StoreAdapter adapterStore = new StoreAdapter(contractStore.getTmpStateStore(), nameSpace);
                        field.set(service, adapterStore); //default => tmpStateStore
                    }

                    if (annotation.annotationType().equals(ContractBranchStateStore.class)) {
                        field.set(service, contractStore.getBranchStore());
                    }

                    if (annotation.annotationType().equals(ContractTransactionReceipt.class)) {
                        field.set(service, trAdapter);
                    }

                    if (annotation.annotationType().equals(ContractChannelField.class)) {
                        field.set(service, coupler);
                    }
                }
            }
        }
    }

    private String namespace(String name) {
        byte[] bundleSymbolicSha3 = HashUtil.sha3omit12(name.getBytes());
        return new String(Base64.encodeBase64(bundleSymbolicSha3));
    }

    public Object query(Map<String, Object> serviceMap, String contractVersion, String methodName, JsonObject params) {
        Object service = serviceMap.get(contractVersion);
        if (service == null) {
            log.error("This service that contract version {} is not registered", contractVersion);
            return null;
        }

        Method method = contractCache.getContractMethodMap(contractVersion, ContractMethodType.QUERY, service).get(methodName);

        if (method == null) {
            log.error("Not found query method: {}", methodName);
            return null;
        }

        try {
            return method.invoke(service, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Query method failed with {}", e.getMessage());
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

        TransactionReceipt txReceipt = createTransactionReceipt(tx);
        TransactionRuntimeResult txRuntimeResult = new TransactionRuntimeResult(tx);

        Set<Map.Entry<String, JsonObject>> result = null;
        try {
            result = getRuntimeResult(serviceMap, tx, txReceipt);
        } catch (ExecutorException e) {
            exceptionHandler(e, txReceipt);
        }

        if (result != null) {
            txRuntimeResult.setChangeValues(result);
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
        // TODO: update coupler logic
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

            Set<Map.Entry<String, JsonObject>> result = null;
            try {
                result = getRuntimeResult(serviceMap, tx, txReceipt);
            } catch (ExecutorException e) {
                exceptionHandler(e, txReceipt);
            }

            if (result != null) {
                blockRuntimeResult.setBlockResult(result);
            }

            blockRuntimeResult.addTxReceipt(txReceipt);
            log.debug("{} : {}", txReceipt.getTxId(), txReceipt.isSuccess());

        }

        contractStore.getTmpStateStore().close(); // clear(revert) tmpStateStore
        locker.unlock();
        return blockRuntimeResult;
    }

    private Set<Map.Entry<String, JsonObject>> getRuntimeResult(Map<String, Object> serviceMap, Transaction tx, TransactionReceipt txReceipt) throws ExecutorException {

        BranchId branchId = tx.getBranchId();
        JsonObject txBody = tx.getBody().getBody();
        // TODO Check if txBody.get("contractVersion") == null

        String contractVersion = null;

        if (txBody.get("contractVersion") == null) {
            contractVersion = Hex.toHexString(tx.getHeader().getType());
        } else {
            contractVersion = txBody.get("contractVersion").getAsString();
        }

        String methodName = txBody.get("method").getAsString();
        JsonObject params = txBody.getAsJsonObject("params");

        Object service = serviceMap.get(contractVersion);

        if (service == null) {
            log.error("This service that contract version {} is not registered", contractVersion);
            throw new ExecutorException(SystemError.CONTRACT_VERSION_NOT_FOUND);

        }

        Method method = contractCache.getContractMethodMap(contractVersion, ContractMethodType.INVOKE, service).get(methodName);

        if (method == null) {
            log.error("Not found contract method: {}", methodName);
            throw new ExecutorException(SystemError.CONTRACT_METHOD_NOT_FOUND);
        }

        trAdapter.setTransactionReceipt(txReceipt);

        try {
            if (method.getParameterCount() == 0) {
                method.invoke(service);
            } else {
                method.invoke(service, params);
            }

        } catch (IllegalAccessException e) {
            log.error("CallContractMethod : {} and bundle {} ", methodName, contractVersion);
        } catch (InvocationTargetException e) {
            log.debug("CallContractMethod ApplicationErrorLog : {}", e.getCause().toString());
            trAdapter.addLog(e.getCause().getMessage());
        } catch (IllegalArgumentException e) {
            e.getStackTrace();
            log.error(e.getMessage());
        }

        return contractStore.getTmpStateStore().changeValues();
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

        if (tx.getBody().getBody().get("contractVersion") == null) {
            return new TransactionReceiptImpl(txId, txSize, issuer);
        }
        return new TransactionReceiptImpl(txId, txSize, issuer,
                tx.getBody().getBody().get("contractVersion").getAsString());
    }

    private void exceptionHandler(ExecutorException e, TransactionReceipt receipt) {
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
