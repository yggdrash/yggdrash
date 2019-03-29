/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.store;

import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.contract.core.store.ReadWriterStore;

public class ReadOnlyStore<K, V> implements ReadWriterStore<K, V> {
    private final ReadWriterStore<K, V> stateStore;

    public ReadOnlyStore(ReadWriterStore<K, V> originStore) {
        this.stateStore = originStore;
    }

    @Override
    public void put(K key, V value) {
        throw new FailedOperationException("This Store is readOnly");
    }

    @Override
    public V get(K key) {
        return stateStore.get(key);
    }

    @Override
    public boolean contains(K key) {
        return stateStore.contains(key);
    }

    @Override
    public void close() {

    }
}
