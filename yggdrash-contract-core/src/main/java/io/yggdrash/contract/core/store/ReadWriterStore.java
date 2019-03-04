package io.yggdrash.contract.core.store;

public interface ReadWriterStore<K, V> extends ReadStore<K> {
    void put(K key, V value);

    boolean contains(K key);

    void close();
}
