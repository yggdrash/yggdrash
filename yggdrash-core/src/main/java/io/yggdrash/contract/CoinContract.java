package io.yggdrash.contract;

import io.yggdrash.core.TransactionReceipt;

import java.util.HashMap;

public class CoinContract implements Contract {

    private HashMap<String, Integer> state = new HashMap<>();

    public CoinContract(StateStore stateStore) {
        state = stateStore.getState();
    }

    /**
     * Returns the balance of the account
     *
     * @param address   account address
     */
    public Integer balance(String address) {
        if (state.get(address) != null) {
            System.out.println("\nstate :: " + this.state);
            return state.get(address);
        }
        return 0;
    }

    /**
     * Returns TransactionRecipt
     *
     * @param from   from address
     * @param to     to address
     * @param amount amount of coin
     */
    public TransactionReceipt transfer(String from, String to, String amount) {
        TransactionReceipt txRecipt = new TransactionReceipt();
        txRecipt.txLog.put("from", from);
        txRecipt.txLog.put("to", to);
        txRecipt.txLog.put("amount", amount);

        if (state.get(from) != null) {
            Integer balanceOfFrom = state.get(from);

            if (balanceOfFrom - Integer.parseInt(amount) < 0) {
                txRecipt.setStatus(0);
            } else {
                balanceOfFrom -= Integer.parseInt(amount);
                state.replace(from, balanceOfFrom);
                if (state.get(to) != null) {
                    Integer balanceOfTo = state.get(to);
                    balanceOfTo += Integer.parseInt(amount);
                    state.replace(to, balanceOfTo);
                } else {
                    state.put(to, Integer.parseInt(amount));
                }
            }
        } else {
            txRecipt.setStatus(0);
        }
        return txRecipt;
    }
}
