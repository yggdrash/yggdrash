package io.yggdrash.core.contract;

import io.yggdrash.core.store.StateStore;

public interface Contract<T> {
    void init(StateStore<T> store);

}
