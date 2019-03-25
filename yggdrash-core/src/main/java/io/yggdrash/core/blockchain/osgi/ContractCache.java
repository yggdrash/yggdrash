package io.yggdrash.core.blockchain.osgi;

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
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;

class ContractCache {
    //Map<contractName, Map<field, List<Annotation>>>
    private final Map<String, Map<Field, List<Annotation>>> injectingFields = new HashMap<>();
    //Map<contractName, Map<methodName, method>>
    private final Map<String, Map<String, Method>> invokeTransactionMethods = new HashMap<>();
    //Map<contractName, Map<methodName, method>>
    private final Map<String, Map<String, Method>> queryMethods = new HashMap<>();

    private final Map<String, Map<String, Method>> endBlockMethods = new HashMap<>();

    private final Map<String, String> fullLocation = new HashMap<>();

    Map<String, Map<Field, List<Annotation>>> getInjectingFields() {
        return injectingFields;
    }

    Map<String, Map<String, Method>> getInvokeTransactionMethods() {
        return invokeTransactionMethods;
    }

    Map<String, Map<String, Method>> getQueryMethods() {
        return queryMethods;
    }

    Map<String, Map<String, Method>> getEndBlockMethods() {
        return endBlockMethods;
    }

    String getFullLocation(String contractName) {
        return fullLocation.get(contractName);
    }


    void cacheContract(Bundle bundle, Framework framework) {
        if (bundle.getRegisteredServices() == null) {
            return;
        }
        // Store Full contract location
        String location = bundle.getLocation();
        if (!fullLocation.containsValue(location)) {
            String contractName = location.substring(location.lastIndexOf("/")+1);
            fullLocation.put(contractName, location);
        }

        // Assume one service
        ServiceReference serviceRef = bundle.getRegisteredServices()[0];
        Object service = framework.getBundleContext().getService(serviceRef);

        if (injectingFields.get(bundle.getLocation()) == null) {
            Map<Field, List<Annotation>> fields = Arrays.stream(service.getClass().getDeclaredFields())
                    .filter(field -> field.getDeclaredAnnotations() != null && field.getDeclaredAnnotations().length > 0)
                    .collect(Collectors.toMap(field -> field, field -> Arrays.asList(field.getDeclaredAnnotations())));
            injectingFields.put(bundle.getLocation(), fields);
        }

        if (invokeTransactionMethods.get(bundle.getLocation()) == null) {
            Map<String, Method> methods = Arrays.stream(service.getClass().getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(InvokeTransaction.class))
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .collect(Collectors.toMap(Method::getName, m -> m));
            invokeTransactionMethods.put(bundle.getLocation(), methods);
        }

        if (queryMethods.get(bundle.getLocation()) == null) {
            Map<String, Method> methods = Arrays.stream(service.getClass().getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(ContractQuery.class))
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .collect(Collectors.toMap(Method::getName, m -> m));
            queryMethods.put(bundle.getLocation(), methods);
        }

        if (endBlockMethods.get(bundle.getLocation()) == null) {
            Map<String, Method> methods = Arrays.stream(service.getClass().getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(ContractEndBlock.class))
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .collect(Collectors.toMap(Method::getName, m -> m));
            endBlockMethods.put(bundle.getLocation(), methods);
        }
    }
}
