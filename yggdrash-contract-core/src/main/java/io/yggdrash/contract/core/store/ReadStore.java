package io.yggdrash.contract.core.store;

public interface ReadStore<K, V> {
    <V> V get(K key);
}
