package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.contract.core.annotation.ContractChannelMethod;
import io.yggdrash.contract.core.annotation.ContractEndBlock;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import io.yggdrash.contract.core.channel.ContractMethodType;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContractCacheImpl implements ContractCache {

    //Map<contractVersion, Map<methodName, method>>
    private final Map<String, Map<String, Method>> invokeTransactionMethods = new HashMap<>();
    //Map<contractVersion, Map<methodName, method>>
    private final Map<String, Map<String, Method>> queryMethods = new HashMap<>();

    private final Map<String, Map<String, Method>> contractChannelMethods = new HashMap<>();

    private final Map<String, Map<String, Method>> endBlockMethods = new HashMap<>();

    Map<String, Map<String, Method>> getInvokeTransactionMethods() {
        return invokeTransactionMethods;
    }

    Map<String, Map<String, Method>> getQueryMethods() {
        return queryMethods;
    }

    Map<String, Map<String, Method>> getChannelMethods() {
        return contractChannelMethods;
    }

    Map<String, Map<String, Method>> getEndBlockMethods() {
        return endBlockMethods;
    }

    public void cacheContract(String contractName, Object service) {
        // Assume one service

        List<Method> serviceMethods = Arrays.stream(service.getClass().getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .collect(Collectors.toList());

        if (invokeTransactionMethods.get(contractName) == null) {
            Map<String, Method> methods = serviceMethods.stream()
                    .filter(method -> method.isAnnotationPresent(InvokeTransaction.class))
                    .collect(Collectors.toMap(Method::getName, m -> m));
            invokeTransactionMethods.put(contractName, methods);
        }

        if (contractChannelMethods.get(contractName) == null) {
            Map<String, Method> methods = serviceMethods.stream()
                    .filter(method -> method.isAnnotationPresent(ContractChannelMethod.class))
                    .collect(Collectors.toMap(Method::getName, m -> m));
            contractChannelMethods.put(contractName, methods);
        }

        if (queryMethods.get(contractName) == null) {
            Map<String, Method> methods = serviceMethods.stream()
                    .filter(method -> method.isAnnotationPresent(ContractQuery.class))
                    .collect(Collectors.toMap(Method::getName, m -> m));
            queryMethods.put(contractName, methods);
        }

        if (endBlockMethods.get(contractName) == null) {
            Map<String, Method> methods = serviceMethods.stream()
                    .filter(method -> method.isAnnotationPresent(ContractEndBlock.class))
                    .collect(Collectors.toMap(Method::getName, m -> m));
            endBlockMethods.put(contractName, methods);
        }
    }

    @Override
    public Map<String, Method> getContractMethodMap(String contractVersion, ContractMethodType type) {
        switch (type) {
            case QUERY:
                return this.queryMethods.get(contractVersion);
            case INVOKE:
                return this.invokeTransactionMethods.get(contractVersion);
            case END_BLOCK:
                return this.endBlockMethods.get(contractVersion);
            case CHANNEL_METHOD:
                return this.contractChannelMethods.get(contractVersion);
            default:
                return new HashMap<>();
        }
    }

}
