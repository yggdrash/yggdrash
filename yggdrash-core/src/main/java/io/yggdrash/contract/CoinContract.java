package io.yggdrash.contract;

import io.yggdrash.core.TransactionReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CoinContract implements Contract {

    private static final Logger log = LoggerFactory.getLogger(CoinContract.class);

    private Map<String, Long> state;

    public CoinContract(StateStore stateStore) {
        state = stateStore.getState();
    }

    /**
     * Returns the balance of the account
     *
     * @param address   account address
     */
    public Long balance(String address) {
        if (state.get(address) != null) {
            log.debug("\nstate :: " + this.state);
            return state.get(address);
        }
        return 0L;
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
            long balanceOfFrom = state.get(from);

            if (balanceOfFrom - Long.parseLong(amount) < 0) {
                txRecipt.setStatus(0);
            } else {
                balanceOfFrom -= Long.parseLong(amount);
                state.replace(from, balanceOfFrom);
                if (state.get(to) != null) {
                    long balanceOfTo = state.get(to);
                    balanceOfTo += Integer.parseInt(amount);
                    state.replace(to, balanceOfTo);
                } else {
                    state.put(to, Long.parseLong(amount));
                }
            }
        } else {
            txRecipt.setStatus(0);
        }
        return txRecipt;
    }
}
