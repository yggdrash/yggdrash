/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.common.store.datasource;

import org.iq80.leveldb.Options;

import java.io.IOException;
import java.util.List;

public interface DbSource<K, V> {
    DbSource<K, V> init();

    DbSource<K, V> init(Options options);

    V get(K key);

    void put(K key, V value);

    List<V> getAll() throws IOException;

    void close();

    void delete(K key);
}
