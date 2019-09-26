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

import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.common.utils.SerializationUtil;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PeerStore implements ReadWriterStore<PeerId, Peer> {
    private static final Logger log = LoggerFactory.getLogger(PeerStore.class);
    private static final byte[] TOTAL_SIZE = SerializationUtil.serializeString("TOTAL_SIZE");
    private static final Lock lock = new ReentrantLock();

    private final DbSource<byte[], byte[]> db;
    private long peerSize;

    PeerStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
        this.peerSize = loadPeerSize();
    }

    @Override
    public void put(PeerId key, Peer value) { //TODO invalid data validation
        if (key == null || key.getBytes() == null || value == null) {
            log.debug("put() is failed. key or value are null.");
            return;
        }

        lock.lock();
        try {
            if (!contains(key)) {
                peerSize++;
                byte[] indexKey = getIndexKey(peerSize);
                // store peer index
                db.put(indexKey, key.getBytes());
                db.put(TOTAL_SIZE, ByteUtil.longToBytes(peerSize));
            }
            db.put(key.getBytes(), value.toString().getBytes());
        } finally {
            lock.unlock();
        }
        log.trace("put() {} {}", key.toString(), value.toString());
    }

    @Override
    public Peer get(PeerId key) {
        return get(key.getBytes());
    }

    private Peer get(byte[] key) {
        byte[] foundedValue = db.get(key);
        if (foundedValue != null) {
            return Peer.valueOf(foundedValue);
        }
        return null;
    }

    @Override
    public boolean contains(PeerId key) {
        return db.get(key.getBytes()) != null;
    }

    public void overwrite(List<Peer> peerList) {
        lock.lock();
        try {
            // remove all
            for (int i = 1; i <= peerSize; i++) {
                byte[] indexKey = getIndexKey(i);
                byte[] key = db.get(indexKey);
                if (key != null) {
                    db.delete(key);
                }
            }
            peerSize = 0L;
            // put all
            peerList.forEach(peer -> put(peer.getPeerId(), peer));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        this.db.close();
    }

    public void remove(PeerId key) {
        lock.lock();
        try {
            byte[] foundedValue = db.get(key.getBytes());
            if (foundedValue == null) {
                return;
            }
            byte[] indexKey = getIndexKey(peerSize);
            db.delete(indexKey);
            db.delete(key.getBytes());
            peerSize--;
            db.put(TOTAL_SIZE, ByteUtil.longToBytes(peerSize));
        } finally {
            lock.unlock();
        }
        log.trace("remove() {} {}", key.toString());
    }

    public List<String> getAll() {
        List<String> list = new ArrayList<>();
        if (peerSize == 0) {
            return list;
        }

        for (int i = 1; i <= peerSize; i++) {
            byte[] indexKey = getIndexKey(i);
            byte[] key = db.get(indexKey);
            Peer peer = key != null ? get(key) : null;
            if (peer != null) {
                list.add(peer.getYnodeUri());
            } else {
                // {PeerId:Peer} has been removed, but {IndexKey:PeerId} still remains
                db.delete(indexKey);
            }
        }

        log.trace("getAll(): {}", list.toString());

        return list;
    }

    public long size() {
        return peerSize;
    }

    private long loadPeerSize() {
        if (peerSize > 0L) {
            return peerSize;
        }
        // loading db is just first
        byte[] sizeByte = db.get(TOTAL_SIZE);
        if (sizeByte != null) {
            return ByteUtil.byteArrayToLong(sizeByte);
        }
        return peerSize;
    }

    private byte[] getIndexKey(long index) {
        String blockIndexKey = "PEER_INDEX_" + index;
        return HashUtil.sha3(blockIndexKey.getBytes());
    }
}
