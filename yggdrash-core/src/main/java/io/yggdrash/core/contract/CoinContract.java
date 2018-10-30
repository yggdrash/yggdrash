package io.yggdrash.core.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.math.BigDecimal;

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
        if (state.getState().size() == 0) {
            log.info("\n genesis :: params => " + params);
            for (int i = 0; i < params.size(); i++) {
                JsonObject jsonObject = params.get(i).getAsJsonObject();
                String frontier = jsonObject.get("frontier").getAsString();
                BigDecimal balance = jsonObject.get("balance").getAsBigDecimal();
                txReceipt.putLog(String.format("frontier[%d]", i), frontier);
                txReceipt.putLog(String.format("balance[%d]", i), balance);
                state.put(frontier, balance);
                txReceipt.setStatus(TransactionReceipt.SUCCESS);
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
