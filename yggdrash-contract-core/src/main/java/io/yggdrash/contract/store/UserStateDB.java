package io.yggdrash.contract.store;

public interface UserStateDB {
    byte[] get(byte[] key);
}
