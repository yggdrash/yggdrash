package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.contract.core.annotation.ContractEndBlock;
import io.yggdrash.contract.core.annotation.ContractQuery;
import io.yggdrash.contract.core.annotation.InvokeTransaction;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ContractCache {
    //Map<contractName, Map<field, List<Annotation>>>
    private final Map<String, Map<Field, List<Annotation>>> injectingFields = new HashMap<>();
    //Map<contractName, Map<methodName, method>>
    private final Map<String, Map<String, Method>> invokeTransactionMethods = new HashMap<>();
    //Map<contractName, Map<methodName, method>>
    private final Map<String, Map<String, Method>> queryMethods = new HashMap<>();

    private final Map<String, Map<String, Method>> endBlockMethods = new HashMap<>();

    ContractCache() {
        injectingFields = new HashMap<>();
        invokeTransactionMethods = new HashMap<>();
        queryMethods = new HashMap<>();
    }

    public Map<String, Map<Field, List<Annotation>>> getInjectingFields() {
        return injectingFields;
    }


    public Map<String, Map<String, Method>> getInvokeTransactionMethods() {
        return invokeTransactionMethods;
    }

    public Map<String, Map<String, Method>> getQueryMethods() {
        return queryMethods;
    }

    public Map<String, Map<String, Method>> getEndBlockMethods() {
        return endBlockMethods;
    }

    void cacheContract(Bundle bundle, Framework framework) {
        if (bundle.getRegisteredServices() == null) {
            return;
        }

        // Assume one service
        ServiceReference serviceRef = bundle.getRegisteredServices()[0];
        Object service = framework.getBundleContext().getService(serviceRef);

        if (injectingFields.get(bundle.getLocation()) == null) {
            Map<Field, List<Annotation>> fields = Arrays.stream(service.getClass().getDeclaredFields())
                    .filter(field -> {
                        if (field.getDeclaredAnnotations() != null && field.getDeclaredAnnotations().length > 0) {
                            return true;
                        }
                        return false;
                    })
                    .collect(Collectors.toMap(field -> field, field -> Arrays.asList(field.getDeclaredAnnotations())));
            injectingFields.put(bundle.getLocation(), fields);
        }

        if (invokeTransactionMethods.get(bundle.getLocation()) == null) {
            Map<String, Method> methods = Arrays.stream(service.getClass().getDeclaredMethods())
                    .filter(method -> {
                        if (method.isAnnotationPresent(InvokeTransaction.class)) {
                            return true;
                        }
                        return false;
                    })
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .collect(Collectors.toMap(m -> m.getName(), m -> m));
            invokeTransactionMethods.put(bundle.getLocation(), methods);
        }

        if (queryMethods.get(bundle.getLocation()) == null) {
            Map<String, Method> methods = Arrays.stream(service.getClass().getDeclaredMethods())
                    .filter(method -> {
                        if (method.isAnnotationPresent(ContractQuery.class)) {
                            return true;
                        }
                        return false;
                    })
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .collect(Collectors.toMap(m -> m.getName(), m -> m));
            queryMethods.put(bundle.getLocation(), methods);
        }

        if (endBlockMethods.get(bundle.getLocation()) == null) {
            Map<String, Method> methods = Arrays.stream(service.getClass().getDeclaredMethods())
                    .filter(method -> {
                        if (method.isAnnotationPresent(ContractEndBlock.class)) {
                            return true;
                        }
                        return false;
                    })
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .collect(Collectors.toMap(m -> m.getName(), m -> m));
            endBlockMethods.put(bundle.getLocation(), methods);
        }
    }
}
