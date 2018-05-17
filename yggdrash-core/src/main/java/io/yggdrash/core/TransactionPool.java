package io.yggdrash.core;

public interface TransactionPool {
    Transaction getTxByHash(String id);

    Transaction addTx(Transaction tx);
}
