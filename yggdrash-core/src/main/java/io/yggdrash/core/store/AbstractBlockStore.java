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

package io.yggdrash.core.store;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.NonExistObjectException;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

import static io.yggdrash.common.config.Constants.LEVELDB_SIZE_KEY;

public abstract class AbstractBlockStore<T> implements ConsensusBlockStore<T> {
    private static final Logger log = LoggerFactory.getLogger(AbstractBlockStore.class);

    protected final DbSource<byte[], byte[]> db;
    private long size;

    protected final ReentrantLock lock = new ReentrantLock();

    protected AbstractBlockStore(DbSource<byte[], byte[]> dbSource) {
        // TODO: config params to configfile
        Options options = new Options();
        options.createIfMissing(true);
        options.compressionType(CompressionType.NONE);
        options.blockSize(10 * 1024 * 1024);
        options.writeBufferSize(10 * 1024 * 1024);
        options.cacheSize(0);
        options.paranoidChecks(true);
        options.verifyChecksums(true);
        options.maxOpenFiles(32);
        this.db = dbSource.init(options);
        this.size = loadSize();
    }

    @Override
    public void put(Sha3Hash key, ConsensusBlock<T> value) {
        if (key == null || value == null) {
            log.debug("put() is failed.");
            return;
        }

        byte[] bytes = value.toBinary();
        if (bytes.length > Constants.MAX_MEMORY) {
            log.debug("block binary {} > {}", bytes.length, Constants.MAX_MEMORY);
            return;
        }

        lock.lock();
        try {
            if (contains(key)) {
                log.debug("put(): Key is duplicated. (key: {})(blockHash {})", key, value.getHash());
            } else {
                log.trace("put (key: {})(blockHash {})", key, value.getHash());
                db.put(key.getBytes(), bytes);
                size++;
                db.put(LEVELDB_SIZE_KEY, ByteUtil.longToBytes(size));
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean contains(Sha3Hash key) {
        try {
            return db.get(key.getBytes()) != null;
        } catch (Exception e) {
            log.debug(e.getMessage());
            return false;
        }
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public void close() {
        db.close();
    }

    @Override
    public void addBlock(ConsensusBlock<T> block) {
        lock.lock();
        try {
            if (block == null) {
                return;
            }
            // Add BlockIndex and Add Block Data
            long index = block.getIndex();
            byte[] indexKey = blockIndexKey(index);
            // store block index
            db.put(indexKey, block.getHash().getBytes());
            // store block data
            put(block.getHash(), block);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ConsensusBlock<T> getBlockByIndex(long index) {
        byte[] indexKey = blockIndexKey(index);
        byte[] blockHash = db.get(indexKey);
        if (blockHash == null) {
            throw new NonExistObjectException(String.valueOf(index));
        }
        return get(Sha3Hash.createByHashed(blockHash));
    }

    private long loadSize() {
        // loading db is just first
        lock.lock();
        byte[] sizeByte = db.get(LEVELDB_SIZE_KEY);
        lock.unlock();
        if (sizeByte != null) {
            return ByteUtil.byteArrayToLong(sizeByte);
        } else {
            return 0L;
        }
    }

    private byte[] blockIndexKey(long index) {
        String blockIndexKey = "BLOCK_INDEX_" + index;
        return HashUtil.sha3(blockIndexKey.getBytes());
    }

}
