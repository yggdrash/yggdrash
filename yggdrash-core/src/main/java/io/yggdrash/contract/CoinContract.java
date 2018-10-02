package io.yggdrash.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.core.TransactionReceipt;

public class CoinContract extends BaseContract<Long> {

    /**
     * Returns the balance of the account (query)
     *
     * @param params   account address
     */
    public Long balanceof(JsonArray params) {
        String address = params.get(0).getAsJsonObject().get("address").getAsString().toLowerCase();
        if (state.get(address) != null) {
            return state.get(address);
        }
        return 0L;
    }

    /**
     * Returns TransactionReceipt (invoke)
     */
    public TransactionReceipt genesis(JsonArray params) {
        TransactionReceipt txReceipt = new TransactionReceipt();
        if (state.getState().size() == 0) {
            log.info("\n genesis :: params => " + params);
            for (int i = 0; i < params.size(); i++) {
                JsonObject jsonObject = params.get(i).getAsJsonObject();
                String frontier = jsonObject.get("frontier").getAsString();
                long balance = jsonObject.get("balance").getAsLong();
                txReceipt.putLog(String.format("frontier[%d]", i), frontier);
                txReceipt.putLog(String.format("balance[%d]", i), balance);
                state.put(frontier, balance);
                log.info("\nAddress of Frontier : " + frontier
                        + "\nBalance of Frontier : " + balance);
            }
        }
        return txReceipt;
    }

    /**
     * Returns TransactionReceipt (invoke)
     */
    public TransactionReceipt transfer(JsonArray params) {
        log.info("\n transfer :: params => " + params);
        String to = params.get(0).getAsJsonObject().get("address").getAsString().toLowerCase();
        long amount = params.get(0).getAsJsonObject().get("amount").getAsLong();

        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.putLog("from", sender);
        txReceipt.putLog("to", to);
        txReceipt.putLog("amount", String.valueOf(amount));

        if (state.get(sender) != null) {
            long balanceOfFrom = state.get(sender);

            if (balanceOfFrom - amount < 0) {
                txReceipt.setStatus(0);
                log.info("\n[ERR] " + sender + " has no enough balance!");
            } else {
                balanceOfFrom -= amount;
                state.replace(sender, balanceOfFrom);
                if (state.get(to) != null) {
                    long balanceOfTo = state.get(to);
                    balanceOfTo += amount;
                    state.replace(to, balanceOfTo);
                } else {
                    state.put(to, amount);
                }
                log.info(
                        "\nBalance of From : " + state.get(sender)
                                + "\nBalance of To   : " + state.get(to));
            }
        } else {
            txReceipt.setStatus(0);
            log.info("\n[ERR] " + sender + " has no balance!");
        }
        return txReceipt;
    }
}
