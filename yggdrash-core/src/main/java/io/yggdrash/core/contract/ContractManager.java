package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.util.ContractUtils;
import io.yggdrash.core.runtime.annotation.ContractQuery;
import io.yggdrash.core.runtime.annotation.InvokeTransction;
import io.yggdrash.core.store.StoreBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContractManager<T> {
    private static final Logger log = LoggerFactory.getLogger(ContractManager.class);
    private final Map<ContractId, JsonObject> contracts = new ConcurrentHashMap<>();

    private final byte[] contractBinary;
    private final ContractId contractId;
    private final String contractClassName;

    private String contractPath;
    private Map<Number, Map<String, JsonObject>> contractState;
    private Map<String, Method> invokeMethod;
    private Map<String, Method> queryMethod;

    public ContractManager(byte[] contractBinary, Class<? extends Contract> contractClass) {
        this.contractBinary = contractBinary;
        this.contractId = ContractId.of(contractBinary);
        this.contractClassName = contractClass.getName();

        contractState = new Hashtable<>();
        contractState = getContractState();

        invokeMethod = getInvokeMethods();
        queryMethod = getQueryMethods();

    }

    private Map<Number, Map<String, JsonObject>> getContractState() {
        return null;
    }

    /**
     * Invoke Method filter
     * @return Method map (method nams is lower case)
     */
    private Map<String,Method> getInvokeMethods() {
//        return ContractUtils.contractMethods(contract, InvokeTransction.class);
        return null;
    }

    /**
     * Query Method filter
     *
     * @return Method map (method name is lower case)
     */
    private Map<String, Method> getQueryMethods() {
//        return ContractUtils.contractMethods(contract, ContractQuery.class);
        return null;
    }
}
