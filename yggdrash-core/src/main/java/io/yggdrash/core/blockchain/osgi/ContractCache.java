package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.contract.core.annotation.ContractChannelMethod;
import io.yggdrash.contract.core.annotation.ContractEndBlock;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.InvokeTransaction;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class ContractCache {
    //Map<contractVersion, Map<field, List<Annotation>>>
    private final Map<String, Map<Field, List<Annotation>>> injectingFields = new HashMap<>();
    //Map<contractVersion, Map<methodName, method>>
    private final Map<String, Map<String, Method>> invokeTransactionMethods = new HashMap<>();
    //Map<contractVersion, Map<methodName, method>>
    private final Map<String, Map<String, Method>> queryMethods = new HashMap<>();

    private final Map<String, Map<String, Method>> contractChannelMethods = new HashMap<>();

    private final Map<String, Map<String, Method>> endBlockMethods = new HashMap<>();

    Map<String, Map<Field, List<Annotation>>> getInjectingFields() {
        return injectingFields;
    }

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

    void cacheContract(String contractName, Object service) {
        // Assume one service
        if (injectingFields.get(contractName) == null) {
            Map<Field, List<Annotation>> fields = Arrays.stream(service.getClass().getDeclaredFields())
                    .filter(field ->
                            field.getDeclaredAnnotations() != null && field.getDeclaredAnnotations().length > 0)
                    .collect(Collectors.toMap(field -> field, field -> Arrays.asList(field.getDeclaredAnnotations())));
            injectingFields.put(contractName, fields);
        }

        if (invokeTransactionMethods.get(contractName) == null) {
            Map<String, Method> methods = Arrays.stream(service.getClass().getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(InvokeTransaction.class))
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .collect(Collectors.toMap(Method::getName, m -> m));
            invokeTransactionMethods.put(contractName, methods);
        }

        if (contractChannelMethods.get(contractName) == null) {
            Map<String, Method> methods = Arrays.stream(service.getClass().getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(ContractChannelMethod.class))
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .collect(Collectors.toMap(Method::getName, m -> m));
            contractChannelMethods.put(contractName, methods);
        }

        if (queryMethods.get(contractName) == null) {
            Map<String, Method> methods = Arrays.stream(service.getClass().getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(ContractQuery.class))
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .collect(Collectors.toMap(Method::getName, m -> m));
            queryMethods.put(contractName, methods);
        }

        if (endBlockMethods.get(contractName) == null) {
            Map<String, Method> methods = Arrays.stream(service.getClass().getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(ContractEndBlock.class))
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .collect(Collectors.toMap(Method::getName, m -> m));
            endBlockMethods.put(contractName, methods);
        }
    }

}
