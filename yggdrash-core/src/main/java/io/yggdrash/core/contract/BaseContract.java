package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseContract<T> implements Contract<T> {
    protected static final Logger log = LoggerFactory.getLogger(BaseContract.class);
    protected TransactionReceiptStore txReceiptStore;
    protected StateStore<T> state;
    protected String sender;

    @Override
    public void init(StateStore<T> store, TransactionReceiptStore txReceiptStore) {
        this.state = store;
        this.txReceiptStore = txReceiptStore;
    }

    @Override
    public boolean invoke(TransactionHusk txHusk) {
        TransactionReceipt txReceipt;
        String txId = txHusk.getHash().toString();
        try {
            this.sender = txHusk.getAddress().toString();
            JsonObject txBody = JsonUtil.parseJsonArray(txHusk.getBody()).get(0).getAsJsonObject();

            dataFormatValidation(txBody);

            String method = txBody.get("method").getAsString().toLowerCase();
            if (txBody.has("params")) {
                JsonObject params = txBody.getAsJsonObject("params");
                txReceipt = (TransactionReceipt) this.getClass().getMethod(method, JsonObject.class)
                        .invoke(this, params);
            } else {
                txReceipt = (TransactionReceipt) this.getClass().getMethod(method)
                        .invoke(this);
            }
            txReceipt.putLog("method", method);
            txReceipt.setTxId(txId);
            txReceiptStore.put(txReceipt.getTxId(), txReceipt);
        } catch (Throwable e) {
            txReceipt = TransactionReceipt.errorReceipt(txId, e);
            txReceiptStore.put(txHusk.getHash().toString(), txReceipt);
        }
        return txReceipt.isSuccess();
    }

    @Override
    public Object query(String method, JsonObject params) {
        try {
            if (params != null) {
                return getClass().getMethod(method.toLowerCase(), JsonObject.class)
                        .invoke(this, params);
            } else {
                return getClass().getMethod(method.toLowerCase()).invoke(this);
            }
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
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
