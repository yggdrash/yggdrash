package io.yggdrash.core;

import java.io.IOException;

public interface TransactionPool {
    Transaction getTxByHash(String id);

    Transaction addTx(Transaction tx) throws IOException;

    void setListener(TransactionPoolListener listener);
}
