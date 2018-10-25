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

import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerId;
import io.yggdrash.core.store.datasource.DbSource;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class PeerStore implements Store<PeerId, Peer> {

    private final DbSource<byte[], byte[]> db;

    PeerStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
    }

    @Override
    public void put(PeerId key, Peer value) {
        db.put(key.getBytes(), value.toString().getBytes());
    }

    @Override
    public Peer get(PeerId key) {
        byte[] foundValue = db.get(key.getBytes());

        if (foundValue != null) {
            return Peer.valueOf(foundValue);
        }

        throw new NonExistObjectException("Not Found [" + key + "]");
    }

    @Override
    public boolean contains(PeerId key) {
        return db.get(key.getBytes()) != null;
    }

    public void close() {
        this.db.close();
    }

    public List<String> getAll() throws IOException {
        return db.getAll().stream().map(Hex::encodeHexString).collect(Collectors.toList());
    }

    public void remove(PeerId key) throws IOException {
        db.getAll().remove(key.getBytes());
    }

    public boolean isEmpty() throws IOException {
        return db.getAll().isEmpty();
    }
}
