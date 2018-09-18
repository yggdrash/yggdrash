package io.yggdrash.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.TransactionReceipt;
import io.yggdrash.core.event.ContractEventListener;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseContract<T> implements Contract<T> {
    private String branchName;
    protected static final Logger log = LoggerFactory.getLogger(BaseContract.class);
    protected StateStore<T> state;
    protected TransactionReceiptStore txReceiptStore;
    protected String sender;
    protected ContractEventListener listener;

    @Override
    public void init(StateStore<T> store, TransactionReceiptStore txReceiptStore) {
        this.state = store;
        this.txReceiptStore = txReceiptStore;
    }

    @Override
    public boolean invoke(TransactionHusk txHusk) throws Exception {
        String data = txHusk.getBody();
        JsonParser jsonParser = new JsonParser();
        JsonArray txBodyArray = (JsonArray) jsonParser.parse(data);
        JsonObject txBody = txBodyArray.get(0).getAsJsonObject();
        String method = txBody.get("method").getAsString().toLowerCase();
        this.sender = txHusk.getAddress().toString();
        JsonArray params = txBody.get("params").getAsJsonArray();

        if (!method.isEmpty()) {
            TransactionReceipt txReceipt = (TransactionReceipt) this.getClass()
                    .getMethod(method, JsonArray.class)
                    .invoke(this, params);
            txReceipt.put("paramSize", params.size());
            txReceipt.put("method", method);
            txReceipt.put("branchName", branchName);
            txReceipt.setTransactionHash(txHusk.getHash().toString());
            if (listener != null) {
                listener.onContractEvent(ContractEvent.of(txReceipt, txHusk));
            }
            txReceiptStore.put(txReceipt.getTransactionHash(), txReceipt);
            return true;
        }
        return false;
    }

    @Override
    public JsonObject query(JsonObject query) throws Exception {
        //this.sender = query.get("address").getAsString();
        String method = query.get("method").getAsString().toLowerCase();
        JsonArray params = query.get("params").getAsJsonArray();

        JsonObject result = new JsonObject();
        if (!method.isEmpty()) {
            Object res = this.getClass().getMethod(method, JsonArray.class)
                    .invoke(this, params);
            result.addProperty("result", res.toString());
            return result;
        }
        return null;
    }

    @Override
    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    @Override
    public void setListener(ContractEventListener listener) {
        this.listener = listener;
    }
}
