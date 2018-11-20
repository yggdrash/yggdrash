package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
        try {
            this.sender = txHusk.getAddress().toString();
            JsonObject txBody = Utils.parseJsonArray(txHusk.getBody()).get(0).getAsJsonObject();

            dataFormatValidation(txBody);

            String method = txBody.get("method").getAsString().toLowerCase();
            JsonArray params = txBody.get("params").getAsJsonArray();

            txReceipt = (TransactionReceipt) this.getClass()
                    .getMethod(method, JsonArray.class)
                    .invoke(this, params);
            txReceipt.putLog("method", method);
            txReceipt.setTransactionHash(txHusk.getHash().toString());
            txReceiptStore.put(txReceipt.getTransactionHash(), txReceipt);
        } catch (Throwable e) {
            txReceipt = new TransactionReceipt();
            txReceipt.setTransactionHash(txHusk.getHash().toString());
            txReceipt.setStatus(0);
            txReceipt.putLog("Error", e);
            txReceiptStore.put(txHusk.getHash().toString(), txReceipt);
        }
        return txReceipt.isSuccess();
    }

    @Override
    public JsonObject query(JsonObject query) {
        dataFormatValidation(query);

        String method = query.get("method").getAsString().toLowerCase();
        JsonArray params = query.get("params").getAsJsonArray();

        JsonObject result = new JsonObject();
        try {
            Object res = getClass().getMethod(method, JsonArray.class).invoke(this, params);
            if (res instanceof Collection) {
                result.addProperty("result", collectionToString((Collection<Object>) res));
            } else {
                result.addProperty("result", res.toString());
            }
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
        return result;
    }


    public List<String> specification(JsonArray params) {
        return Collections.singletonList(Arrays.toString(getClass().getDeclaredMethods()));
    }

    private void dataFormatValidation(JsonObject data) {
        if (data.get("method").getAsString().length() < 0) {
            throw new FailedOperationException("Empty method");
        }
        if (!data.get("params").isJsonArray()) {
            throw new FailedOperationException("Params must be JsonArray");
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
