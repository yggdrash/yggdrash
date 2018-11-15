package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.Map;

public class CoinContract extends BaseContract<BigDecimal> {

    /**
     * Returns the balance of the account (query)
     *
     * @param params   account address
     */
    public BigDecimal balanceof(JsonArray params) {
        String address = params.get(0).getAsJsonObject().get("address").getAsString().toLowerCase();
        if (state.get(address) != null) {
            return state.get(address);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Returns TransactionReceipt (invoke)
     */
    public TransactionReceipt genesis(JsonArray params) {
        TransactionReceipt txReceipt = new TransactionReceipt();
        if (state.getState().size() > 0) {
            return txReceipt;
        }
        log.info("\n genesis :: params => " + params);
        JsonObject json = params.get(0).getAsJsonObject();
        JsonObject alloc = json.get("alloc").getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : alloc.entrySet()) {
            String frontier = entry.getKey();
            JsonObject value = entry.getValue().getAsJsonObject();
            BigDecimal balance = value.get("balance").getAsBigDecimal();
            txReceipt.putLog(frontier, balance);
            state.put(frontier, balance);
            txReceipt.setStatus(TransactionReceipt.SUCCESS);
            log.info("\nAddress of Frontier : " + frontier
                    + "\nBalance of Frontier : " + balance);
        }
        return txReceipt;
    }

    /**
     * Returns TransactionReceipt (invoke)
     */
    public TransactionReceipt transfer(JsonArray params) {
        log.info("\n transfer :: params => " + params);
        String to = params.get(0).getAsJsonObject().get("address").getAsString().toLowerCase();
        BigDecimal amount = params.get(0).getAsJsonObject().get("amount").getAsBigDecimal();

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.putLog("from", sender);
        txReceipt.putLog("to", to);
        txReceipt.putLog("amount", String.valueOf(amount));

        if (state.get(sender) != null) {
            BigDecimal balanceOfFrom = state.get(sender);

            if (balanceOfFrom.subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
                log.info("\n[ERR] " + sender + " has no enough balance!");
            } else {
                state.replace(sender, balanceOfFrom.subtract(amount));
                if (state.get(to) != null) {
                    state.replace(to, state.get(to).add(amount));
                } else {
                    state.put(to, amount);
                }
                txReceipt.setStatus(TransactionReceipt.SUCCESS);
                log.info(
                        "\nBalance of From : " + state.get(sender)
                                + "\nBalance of To   : " + state.get(to));
            }
        } else {
            log.info("\n[ERR] " + sender + " has no balance!");
        }
        return txReceipt;
    }
}
