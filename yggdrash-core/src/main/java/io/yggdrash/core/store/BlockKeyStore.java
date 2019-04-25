package io.yggdrash.core.store;

import io.yggdrash.contract.core.store.ReadWriterStore;

@Deprecated
public interface BlockKeyStore<K, V> extends ReadWriterStore<K, V> {
    long size();
}
