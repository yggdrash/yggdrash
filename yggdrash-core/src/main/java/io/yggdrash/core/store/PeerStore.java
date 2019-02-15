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

import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerId;
import io.yggdrash.core.store.datasource.DbSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PeerStore implements Store<PeerId, Peer> {

    private static final Logger log = LoggerFactory.getLogger(PeerStore.class);
    private final DbSource<byte[], byte[]> db;
    private final transient Map<PeerId, Peer> peers = new HashMap<>();

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
        byte[] foundedValue = db.get(key.getBytes());
        if (foundedValue != null) {
            return Peer.valueOf(foundedValue);
        }
        return peers.get(key);
    }

    @Override
    public boolean contains(PeerId key) {
        if (db.get(key.getBytes()) != null) {
            return true;
        }
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
        try {
            if (!db.getAll().isEmpty()) {
                return db.getAll().stream().map(Peer::valueOf)
                        .map(Peer::toString).collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return peers.values().stream().map(Peer::toString).collect(Collectors.toList());
    }

    public int size() {
        try {
            if (!db.getAll().isEmpty()) {
                return db.getAll().size();
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return peers.size();
    }
}
