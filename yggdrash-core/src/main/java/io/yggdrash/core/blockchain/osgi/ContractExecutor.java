package io.yggdrash.core.blockchain.osgi;

import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.TransactionReceiptAdapter;
import io.yggdrash.contract.core.TransactionReceiptImpl;
import io.yggdrash.contract.core.annotation.ContractBranchStateStore;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.InjectEvent;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.runtime.result.BlockRuntimeResult;
import io.yggdrash.core.runtime.result.TransactionRuntimeResult;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.StoreAdapter;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.apache.commons.codec.binary.Base64;
import org.osgi.framework.Bundle;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContractExecutor {
    private static final Logger log = LoggerFactory.getLogger(ContractExecutor.class);

    private final Framework framework;
    private final ContractStore contractStore;

    private final SystemProperties systemProperties;
    private final ContractCache contractCache;
    private TransactionReceiptAdapter trAdapter;

    ContractExecutor(Framework framework, ContractStore contractStore, SystemProperties systemProperties) {
        this.framework = framework;
        this.contractStore = contractStore;
        this.systemProperties = systemProperties;
        contractCache = new ContractCache();
        trAdapter = new TransactionReceiptAdapter();
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

                if (systemProperties != null
                        && annotation.annotationType().equals(InjectEvent.class)
                        && field.getType().isAssignableFrom(systemProperties.getEventStore().getClass())) {
                    field.set(service, systemProperties.getEventStore());
                }
            }
        }

        contractCache.cacheContract(bundle.getLocation(), service);
    }

    private enum MethodType {
        EndBlock,
        Query,
        InvokeTx
    }

    private Object callContractMethod(String contractVersion, Object service, String methodName, JsonObject params,
                                      MethodType methodType, TransactionReceipt txReceipt,
                                      JsonObject endBlockParams) {

        contractCache.cacheContract(contractVersion, service);

        Map<String, Method> methodMap = null;
        switch (methodType) {
            case InvokeTx:
                methodMap = contractCache.getInvokeTransactionMethods().get(contractVersion);
                break;
            case Query:
                methodMap = contractCache.getQueryMethods().get(contractVersion);
                break;
            case EndBlock:
                methodMap = contractCache.getEndBlockMethods().get(contractVersion);
                break;
            default:
                log.error("Method Type is not exist");
                return null;
        }

        if (methodMap == null || methodMap.get(methodName) == null) {
            return null;
        }

        Method method = methodMap.get(methodName);
        try {
            if (methodType == MethodType.InvokeTx) {
                //
                trAdapter.setTransactionReceipt(txReceipt);
            }

            if (method.getParameterCount() == 0) {
                return method.invoke(service);
            } else {
                if (methodType == MethodType.EndBlock) {
                    return method.invoke(service, endBlockParams);
                } else {
                    return method.invoke(service, params);
                }
            }
        } catch (Exception e) {
            contractStore.revertTmpStateStore();
            log.error("Call contract method : {} and bundle {} ", methodName, contractVersion);
        }

        return null;
    }

    public Object query(String contractVersion, Object service, String methodName, JsonObject params) {

        return callContractMethod(
                contractVersion, service, methodName, params, MethodType.Query, null, null);
    }

    public Object invoke(String contractVersion, Object service, JsonObject txBody, TransactionReceipt txReceipt) {

        return callContractMethod(contractVersion, service, txBody.get("method").getAsString(),
                txBody.getAsJsonObject("params"), MethodType.InvokeTx, txReceipt, null);
    }

    private List<Object> endBlock(String location, Object service, JsonObject endBlockParams) {
        List<Object> results = new ArrayList<>();
        for (Bundle bundle : framework.getBundleContext().getBundles()) {
            contractCache.cacheContract(location, service);
            Map<String, Method> endBlockMethods = contractCache.getEndBlockMethods().get(bundle.getLocation());
            if (endBlockMethods != null) {
                endBlockMethods.forEach((k, m) -> {
                    Object result = callContractMethod(
                            location, service, k, null, MethodType.EndBlock, null, endBlockParams);
                    if (result != null) {
                        results.add(result);
                    }
                });
            }
        }

        return results;
    }

    TransactionRuntimeResult executeTx(String contractVersion, Object service, Transaction tx) {
        TransactionReceipt txReceipt = createTransactionReceipt(tx);
        TransactionRuntimeResult txRuntimeResult = new TransactionRuntimeResult(tx);
        txRuntimeResult.setTransactionReceipt(txReceipt);

        JsonObject txBody = JsonUtil.parseJsonObject(tx.getBody().toString());
        Object contractResult = invoke(contractVersion, service, txBody, txReceipt);
        //TODO Q. Where will the contractResult be used?

        txRuntimeResult.setChangeValues(contractStore.getTmpStateStore().changeValues());

        return txRuntimeResult;
    }

    BlockRuntimeResult executeTxs(Map<String, Object> serviceMap, ConsensusBlock nextBlock) {
        if (nextBlock.getIndex() == 0) {
            // TODO first transaction is genesis
            // TODO init method don't call any more
        }

        BlockRuntimeResult blockRuntimeResult = new BlockRuntimeResult(nextBlock);

        for (Transaction tx : nextBlock.getBody().getTransactionList()) {
            TransactionReceipt txReceipt = createTransactionReceipt(tx);

            txReceipt.setBlockId(nextBlock.getHash().toString());
            txReceipt.setBlockHeight(nextBlock.getIndex());
            txReceipt.setBranchId(nextBlock.getBranchId().toString());

            JsonObject txBody = JsonUtil.parseJsonObject(tx.getBody().toString());
            String contractVersion = txBody.get("contractVersion").getAsString();
            Object service = serviceMap.get(contractVersion);
            Object contractResult = invoke(contractVersion, service, txBody, txReceipt);
            //TODO Q. Where will the contractResult be used?

            log.debug("{} is {}", txReceipt.getTxId(), txReceipt.isSuccess());

            blockRuntimeResult.addTxReceipt(txReceipt);
        }

        return blockRuntimeResult;
    }

    public void commitBlockResult(BlockRuntimeResult result) {
        // TODO store transaction by batch
        Map<String, JsonObject> changes = result.getBlockResult(); //TODO test required
        TransactionReceiptStore transactionReceiptStore = contractStore.getTransactionReceiptStore();
        result.getTxReceipts().forEach(transactionReceiptStore::put);
        if (!changes.isEmpty()) {
            StateStore stateStore = contractStore.getStateStore();
            changes.forEach(stateStore::put);
        }
        // TODO make transaction Receipt Event
    }

    public static TransactionReceipt createTransactionReceipt(Transaction tx) {
        String txId = tx.getHash().toString();
        long txSize = tx.getBody().getLength();
        String issuer = tx.getAddress().toString();

        return new TransactionReceiptImpl(txId, txSize, issuer);
    }
}
