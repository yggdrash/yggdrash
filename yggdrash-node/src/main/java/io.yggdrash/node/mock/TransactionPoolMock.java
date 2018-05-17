package io.yggdrash.node.mock;

import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionPool;

import java.util.HashMap;
import java.util.Map;

public class TransactionPoolMock implements TransactionPool {
    private Map<String, Transaction> txs = new HashMap<>();

    @Override
    public Transaction getTxByHash(String id) {
        return txs.get(id);
    }

    @Override
    public Transaction addTx(Transaction tx) {
        txs.put(tx.getHashString(), tx);
        return tx;
    }
}
