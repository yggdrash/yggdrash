/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.store;

import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerId;
import io.yggdrash.core.store.datasource.DbSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PeerStore implements Store<PeerId, Peer> {

    private final DbSource<byte[], byte[]> db;
    private transient Map<PeerId, Peer> peers = new HashMap<>();

    PeerStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
    }

    @Override
    public void put(PeerId key, Peer value) {
        peers.put(key, value);
        db.put(key.getBytes(), value.toString().getBytes());
    }

    @Override
    public Peer get(PeerId key) {
        return peers.get(key);
    }

    @Override
    public boolean contains(PeerId key) {
        return peers.containsKey(key);
    }

    public void close() {
        this.db.close();
    }

    public void remove(PeerId key) {
        peers.remove(key);
        db.delete(key.getBytes());
    }

    public List<String> getAll() {
        return peers.values().stream().map(Peer::toString).collect(Collectors.toList());
    }

    public int size() {
        return peers.size();
    }
}
