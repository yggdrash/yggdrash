package io.yggdrash.validator.store;

import io.yggdrash.contract.core.store.ReadWriterStore;

public interface BlockStore<K, V> extends ReadWriterStore<K, V> {
    long size();
}
