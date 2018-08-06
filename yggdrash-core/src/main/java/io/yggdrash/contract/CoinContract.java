package io.yggdrash.contract;

import io.yggdrash.core.TransactionReceipt;

public class CoinContract implements Contract {
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
        return txRecipt;
    }
}
