package io.yggdrash.core.blockchain.osgi;

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

    Map<String, Map<Field, List<Annotation>>> getInjectingFields() {
        return injectingFields;
    }

    Map<String, Map<String, Method>> getInvokeTransactionMethods() {
        return invokeTransactionMethods;
    }

    Map<String, Map<String, Method>> getQueryMethods() {
        return queryMethods;
    }
}
