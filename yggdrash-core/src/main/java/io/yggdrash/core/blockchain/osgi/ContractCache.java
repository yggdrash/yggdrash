package io.yggdrash.core.blockchain.osgi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContractCache {
    //Map<contractName, Map<field, List<Annotation>>>
    private Map<String, Map<Field, List<Annotation>>> injectingFields;
    //Map<contractName, Map<methodName, method>>
    private Map<String, Map<String, Method>> invokeTransactionMethods;

    ContractCache() {
        injectingFields = new HashMap<>();
        invokeTransactionMethods = new HashMap<>();
    }

    public Map<String, Map<Field, List<Annotation>>> getInjectingFields() {
        return injectingFields;
    }


    public Map<String, Map<String, Method>> getInvokeTransactionMethods() {
        return invokeTransactionMethods;
    }
}
