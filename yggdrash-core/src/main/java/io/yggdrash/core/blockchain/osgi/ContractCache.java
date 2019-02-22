package io.yggdrash.core.blockchain.osgi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ContractCache {
    private Field[] transactionReceiptFields;
    private Method[] invokeTransctionMethods;

    public Field[] getTransactionReceiptFields() {
        return transactionReceiptFields;
    }

    public void setTransactionReceiptFields(Field[] transactionReceiptFields) {
        this.transactionReceiptFields = transactionReceiptFields;
    }

    public Method[] getInvokeTransctionMethods() {
        return invokeTransctionMethods;
    }

    public void setInvokeTransctionMethods(Method[] invokeTransctionMethods) {
        this.invokeTransctionMethods = invokeTransctionMethods;
    }
}
