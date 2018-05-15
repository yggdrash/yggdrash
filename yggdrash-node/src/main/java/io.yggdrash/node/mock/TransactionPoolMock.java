package io.yggdrash.node.mock;

import io.yggdrash.core.TransactionPool;

import java.util.HashMap;
import java.util.Map;

public class TransactionPoolMock implements TransactionPool {
    private Map<String, TransactionPool> txs = new HashMap<>();
}
