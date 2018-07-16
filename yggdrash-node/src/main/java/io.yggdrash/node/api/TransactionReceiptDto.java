package io.yggdrash.node.api;

import io.yggdrash.core.cache.TransactionPool;

public class TransactionReceiptDto {

    private final TransactionPool txPool;

    public TransactionReceiptDto(TransactionPool txPool) {
        this.txPool = txPool;
    }

}
