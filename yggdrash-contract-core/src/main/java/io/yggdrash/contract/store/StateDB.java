package io.yggdrash.contract.store;

public interface StateDB extends UserStateDB {
    void put(byte[] key, byte[] value);
}
