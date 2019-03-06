package io.yggdrash.core.blockchain.osgi;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.contract.TransactionReceiptImpl;
import io.yggdrash.core.runtime.result.BlockRuntimeResult;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ContractManager {
    private static final Logger log = LoggerFactory.getLogger(ContractManager.class);

    private Framework framework;
    private String systemContractPath;
    private String userContractPath;
    private String branchId;
    private StateStore stateStore;
    private TransactionReceiptStore transactionReceiptStore;

    private ContractCache contractCache;
    private List<String> systemContracts;

    ContractManager(Framework framework, String systemContractPath, String userContractPath, String branchId, StateStore stateStore, TransactionReceiptStore transactionReceiptStore) {
        this.framework = framework;
        this.systemContractPath = systemContractPath;
        this.userContractPath = userContractPath;
        this.branchId = branchId;
        this.stateStore = stateStore;
        this.transactionReceiptStore = transactionReceiptStore;
        contractCache = new ContractCache();
    }

    public void setSystemContracts(List<String> systemContracts) {
        this.systemContracts = systemContracts;
    }

    public String makeContractPath(String contractName, boolean isSystemContract) {
        return String.format("%s/%s", isSystemContract ? systemContractPath : userContractPath, contractName);
    }

    public String makeContractFullPath(String contractName, boolean isSystemContract) {
        return String.format("%s%s/%s", ContractContainer.PREFIX_BUNDLE_PATH, isSystemContract ? systemContractPath : userContractPath, contractName);
    }

    public boolean checkSystemContract(String contractName) {
        return contractName.matches("[0-9]*[-]*system-.*");
    }

    void inject(Bundle bundle) throws IllegalAccessException {
        ServiceReference<?>[] serviceRefs = bundle.getRegisteredServices();
        if (serviceRefs == null) {
            return;
        }

        boolean isSystemContract = bundle.getLocation().startsWith(String.format("%s%s", ContractContainer.PREFIX_BUNDLE_PATH, systemContractPath)) ? true : false;

        for (ServiceReference serviceRef : serviceRefs) {
            Object service = framework.getBundleContext().getService(serviceRef);
            injectField(service, service.getClass().getDeclaredFields(), isSystemContract);
        }
    }

    private void injectField(Object o, Field[] fields, boolean isSystemContract) throws IllegalAccessException {
        for (Field field : fields) {
            field.setAccessible(true);
            for (Annotation annotation : field.getDeclaredAnnotations()) {
                if (isSystemContract) {
                    if (annotation.annotationType().equals(ContractStateStore.class)) {
                        field.set(o, stateStore);
                    }
                }
            }
        }
    }

    private Bundle getBundle(Object identifier) {
        Bundle bundle = null;
        if (identifier instanceof String) {
            bundle = framework.getBundleContext().getBundle((String) identifier);
        } else if (identifier instanceof Long) {
            bundle = framework.getBundleContext().getBundle((long) identifier);
        }
        return bundle;
    }

    private enum ActionType {
        UNINSTALL,
        START,
        STOP
    }

    private enum MethodType {
        EndBlock,
        Query,
        InvokeTx
    }

    private boolean action(Object identifier, ActionType action) {
        Bundle bundle = getBundle(identifier);
        if (bundle == null) {
            return false;
        }

        try {
            switch (action) {
                case UNINSTALL:
                    bundle.uninstall();
                    break;
                case START:
                    bundle.start();
                    inject(bundle);
                    break;
                case STOP:
                    bundle.stop();
                    break;
            }
        } catch (Exception e) {
            log.error("Execute bundle exception: contractId:{}, path:{}, stack:{}", bundle.getBundleId(), bundle.getLocation(), ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
        }
        return true;
    }

    private boolean verifyManifest(Bundle bundle) {
        String bundleManifestVersion = bundle.getHeaders().get("Bundle-ManifestVersion");
        String bundleSymbolicName = bundle.getHeaders().get("Bundle-SymbolicName");
        String bundleVersion = bundle.getHeaders().get("Bundle-Version");
        if (!"2".equals(bundleManifestVersion)) {
            log.error("Must set Bundle-ManifestVersion to 2");
            return false;
        }
        if (bundleSymbolicName == null || "".equals(bundleSymbolicName)) {
            log.error("Must set Bundle-SymbolicName");
            return false;
        }

        if (bundleVersion == null || "".equals(bundleVersion)) {
            log.error("Must set Bundle-Version");
            return false;
        }

        return true;
    }

    public long install(String contractFileName, boolean isSystemContract) {
        Bundle bundle;
        try {
            bundle = framework.getBundleContext().installBundle(makeContractFullPath(contractFileName, isSystemContract));
            boolean isPass = verifyManifest(bundle);
            if (!isPass) {
                uninstall(bundle.getBundleId());
            }

            start(bundle.getBundleId());
        } catch (Exception e) {
            log.error("Install bundle exception: branchID - {}, msg - {}", branchId, e.getMessage());
            throw new RuntimeException(e);
        }
        return bundle.getBundleId();
    }

    public boolean uninstall(long contractId) {
        return action(contractId, ActionType.UNINSTALL);
    }

    public boolean start(long contractId) {
        return action(contractId, ActionType.START);
    }

    public boolean stop(long contractId) {
        return action(contractId, ActionType.STOP);
    }

    private Object callContractMethod(Bundle bundle, String methodName, JsonObject params, MethodType methodType, TransactionReceipt txReceipt) {
        if (bundle.getRegisteredServices() == null) {
            return null;
        }
        // Assume one service
        ServiceReference serviceRef = bundle.getRegisteredServices()[0];
        Object service = framework.getBundleContext().getService(serviceRef);

        Map<String, Method> methodMap = null;
        contractCache.cacheContract(bundle, framework);
        switch (methodType) {
            case InvokeTx:
                methodMap = contractCache.getInvokeTransactionMethods().get(bundle.getLocation());
                break;
            case Query:
                methodMap = contractCache.getQueryMethods().get(bundle.getLocation());
                break;
            case EndBlock:
                methodMap = contractCache.getEndBlockMethods().get(bundle.getLocation());
                break;
        }

        if (methodMap == null || methodMap.get(methodName) == null) {
            return null;
        }

        Method method = methodMap.get(methodName);
        try {
            switch (methodType) {
                case InvokeTx:
                    // Inject field
                    Map<Field, List<Annotation>> fields = contractCache.getInjectingFields().get(bundle.getLocation());
                    Iterator<Field> fieldIter = fields.keySet().iterator();
                    while (fieldIter.hasNext()) {
                        Field field = fieldIter.next();
                        field.setAccessible(true);
                        for (Annotation a : field.getDeclaredAnnotations()) {
                            if (a.annotationType().equals(ContractTransactionReceipt.class)) {
                                field.set(service, txReceipt);
                            }
                        }
                    }
                    break;
            }

            if (method.getParameterCount() == 0) {
                return method.invoke(service);
            } else {
                return method.invoke(service, params);
            }
        } catch (Exception e) {
            log.error("Call contract method : {}", bundle.getBundleId());
        }

        return null;
    }

    public Object query(String contractVersion, String methodName, JsonObject params) {
        String contractFileName = makeContractFullPath(contractVersion, checkSystemContract(contractVersion));
        Bundle bundle = getBundle(contractFileName);
        if (bundle == null) {
            return null;
        }
        return callContractMethod(bundle, methodName, params, MethodType.Query, null);
    }

    public Object invoke(String contractFileName, JsonObject txBody, TransactionReceipt txReceipt) {
        Bundle bundle = getBundle(contractFileName);
        if (bundle == null) {
            return null;
        }
        return invoke(bundle.getBundleId(), txBody, txReceipt);
    }

    public Object invoke(long contractId, JsonObject txBody, TransactionReceipt txReceipt) {
        Bundle bundle = getBundle(contractId);
        if (bundle == null) {
            return null;
        }
        return callContractMethod(bundle, txBody.get("method").getAsString(), txBody.getAsJsonObject("params"), MethodType.InvokeTx, txReceipt);
    }

    private List<Object> endBlock() {
        List<Object> results = new ArrayList<>();
        for (Bundle bundle : framework.getBundleContext().getBundles()) {
            ServiceReference serviceRef = bundle.getRegisteredServices()[0];
            Object service = framework.getBundleContext().getService(serviceRef);

            contractCache.cacheContract(bundle, framework);
            Map<String, Method> endBlockMethods = contractCache.getEndBlockMethods().get(bundle.getLocation());
            if (endBlockMethods != null) {
                endBlockMethods.forEach((k, m) -> {
                    Object result = callContractMethod(bundle, k, null, MethodType.EndBlock, null);
                    if (result != null) {
                        results.add(result);
                    }
                });
            }
        }

        return results;
    }

    public BlockRuntimeResult executeTransactions(BlockHusk nextBlock) {
        if (nextBlock.getIndex() == 0) {
            // TODO first transaction is genesis
            // TODO init method don't call any more
        }

        BlockRuntimeResult result = new BlockRuntimeResult(nextBlock);
//        TempStateStore blockState = new TempStateStore(stateStore);
        for (TransactionHusk tx : nextBlock.getBody()) {
            TransactionReceipt txReceipt = new TransactionReceiptImpl(tx);
            // set Block ID
            txReceipt.setBlockId(nextBlock.getHash().toString());
            txReceipt.setBlockHeight(nextBlock.getIndex());
            txReceipt.setBranchId(nextBlock.getBranchId().toString());

            for (JsonElement transactionElement : JsonUtil.parseJsonArray(tx.getBody())) {
                JsonObject txBody = transactionElement.getAsJsonObject();
                String contractVersion = txBody.get("contractVersion").getAsString();
                Object contractResult = invoke(
                        makeContractFullPath(contractVersion, checkSystemContract(contractVersion))
                        , txBody
                        , txReceipt
                );
                log.debug("{} is {}", txReceipt.getTxId(), txReceipt.isSuccess());
            }
            result.addTxReceipt(txReceipt);
            // Save TxReceipt
        }
        List<Object> endBlockResult = endBlock();
        // Save BlockStates
//        result.setBlockResult(blockState.changeValues());

        return result;
    }

    public void commitBlockResult(BlockRuntimeResult result) {
        // TODO store transaction bybatch
        Map<String, JsonObject> changes = result.getBlockResult();
        result.getTxReceipts().stream().forEach(txr -> {
            transactionReceiptStore.put(txr);
        });
        if (!changes.isEmpty()) {
            changes.entrySet().stream().forEach(r -> {
                stateStore.put(r.getKey(), r.getValue());
            });

        }
        // TODO make transaction Receipt Event

    }

    public List<ContractStatus> searchContracts() {
        List<ContractStatus> result = new ArrayList<>();
        for (Bundle bundle : framework.getBundleContext().getBundles()) {
            Dictionary<String, String> header = bundle.getHeaders();
            int serviceCnt = bundle.getRegisteredServices() == null ? 0 : bundle.getRegisteredServices().length;

            Version v = bundle.getVersion();
            result.add(new ContractStatus(
                    bundle.getSymbolicName(),
                    String.format("%s.%s.%s", v.getMajor(), v.getMinor(), v.getMicro()),
                    header.get("Bundle-Vendor"),
                    header.get("Bundle-Description"),
                    bundle.getBundleId(),
                    bundle.getLocation(),
                    bundle.getState(),
                    serviceCnt
            ));
        }
        return result;
    }
}
