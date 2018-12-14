package io.yggdrash.core.contract;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
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
        String transactionHash = txHusk.getHash().toString();
        try {
            this.sender = txHusk.getAddress().toString();
            JsonObject txBody = Utils.parseJsonArray(txHusk.getBody()).get(0).getAsJsonObject();

            dataFormatValidation(txBody);

            String method = txBody.get("method").getAsString().toLowerCase();
            JsonObject param = txBody.get("param").getAsJsonObject();

            txReceipt = (TransactionReceipt) this.getClass()
                    .getMethod(method, JsonObject.class)
                    .invoke(this, param);
            txReceipt.putLog("method", method);
            txReceipt.setTransactionHash(transactionHash);
            txReceiptStore.put(txReceipt.getTransactionHash(), txReceipt);
        } catch (Throwable e) {
            txReceipt = TransactionReceipt.errorReceipt(transactionHash, e);
            txReceiptStore.put(txHusk.getHash().toString(), txReceipt);
        }
        return txReceipt.isSuccess();
    }

    @Override
    @SuppressWarnings("unchecked")
    public JsonObject query(JsonObject query) {
        dataFormatValidation(query);

        String method = query.get("method").getAsString().toLowerCase();
        JsonObject param = query.get("param").getAsJsonObject();

        JsonObject result = new JsonObject();
        try {
            Object res = getClass().getMethod(method, JsonObject.class).invoke(this, param);
            if (res instanceof JsonElement) {
                result.add("result", (JsonElement)res);
            } else if (res instanceof Collection<?>) {
                result.addProperty("result", collectionToString((Collection<Object>) res));
            } else {
                result.addProperty("result", res.toString());
            }
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
        return result;
    }

    public List<String> specification(JsonObject param) {
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
        if (!data.get("param").isJsonObject()) {
            throw new FailedOperationException("Param must be JsonObject");
        }
    }

    private String collectionToString(Collection<Object> collection) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : collection) {
            String str = obj.toString();
            if (sb.length() != 0) {
                sb.append(",");
            }
            sb.append(str);
        }

        return sb.toString();
    }
}
