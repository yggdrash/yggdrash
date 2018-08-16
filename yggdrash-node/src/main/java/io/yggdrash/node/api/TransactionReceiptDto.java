package io.yggdrash.node.api;

import io.yggdrash.core.store.CachePool;

public class TransactionReceiptDto {

    private final CachePool txPool;

    public TransactionReceiptDto(CachePool txPool) {
        this.txPool = txPool;
    }

}
