package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.runtime.annotation.ContractTransactionReceipt;
import io.yggdrash.core.store.StateStore;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseContract<T> implements Contract<T> {
    protected static final Logger log = LoggerFactory.getLogger(BaseContract.class);
    protected StateStore<T> state;

    // TODO REMOVE sender ASSP
    protected String sender;

    @ContractTransactionReceipt
    TransactionReceipt txReceipt;

    @Override
    public void init(StateStore<T> store) {
        this.state = store;
    }

    public List<String> specification() {
        List<String> methods = new ArrayList<>();
        getMethods(getClass(), methods);

        return methods;
    }

    private List<String> getMethods(Class<?> currentClass, List<String> methods) {
        if (!currentClass.equals(BaseContract.class)) {
            for (Method method : currentClass.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers())) {
                    methods.add(method.toString());
                }
            }
            getMethods(currentClass.getSuperclass(), methods);
        }
        return methods;
    }

    private void dataFormatValidation(JsonObject data) {
        if (data.get("method").getAsString().length() < 0) {
            throw new FailedOperationException("Empty method");
        }
        if (!data.get("params").isJsonObject()) {
            throw new FailedOperationException("Param must be JsonObject");
        }
    }
}
