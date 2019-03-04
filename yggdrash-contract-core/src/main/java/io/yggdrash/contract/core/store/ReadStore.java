package io.yggdrash.contract.core.store;

public interface ReadStore<K> {
    <V> V get(K key);
}
