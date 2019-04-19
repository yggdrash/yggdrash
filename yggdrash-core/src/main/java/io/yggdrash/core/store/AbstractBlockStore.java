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
import io.yggdrash.core.consensus.Block;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.exception.NotValidateException;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractBlockStore<T> implements ConsensusBlockStore<T> {
    private static final Logger log = LoggerFactory.getLogger(AbstractBlockStore.class);

    protected final DbSource<byte[], byte[]> db;
    private long blockSize;
    private long transactionSize;

    protected final ReentrantLock lock = new ReentrantLock();

    protected AbstractBlockStore(DbSource<byte[], byte[]> dbSource) {
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

        try {
            this.blockSize = this.db.getAll().size();
        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new NotValidateException("Store is not valid.");
        }
        // get Transaction Size
        transactionSize = getBlockchainTransactionSize();
    }

    @Override
    public void put(Sha3Hash key, Block<T> value) {
        if (key == null || value == null || value.getData().length > Constants.MAX_MEMORY) {
            log.debug("Key or value are not valild.");
            return;
        }

        lock.lock();
        try {
            if (!contains(key)) {
                log.trace("put (key: {})(blockHash {})", key, value.getHashHex());
                db.put(key.getBytes(), value.getData());
                blockSize++;
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
        } finally {
            lock.unlock();
        }

        db.put(key.getBytes(), value.getData());
    }

    @Override
    public boolean contains(Sha3Hash key) {
        return db.get(key.getBytes()) != null;
    }

    @Override
    public long size() {
        lock.lock();
        try {
            return this.blockSize;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            db.close();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addBlock(Block<T> block) {
        // Add BlockIndex and Add Block Data
        long index = block.getIndex();
        byte[] indexKey = blockIndexKey(index);
        // store block index
        lock.lock();
        db.put(indexKey, block.getHash().getBytes());
        // store block data
        db.put(block.getHash().getBytes(), block.getData());
        // add block Transaction size
        transactionSize += block.getBodyCount();
        db.put("TRANSACTION_SIZE".getBytes(), ByteUtil.longToBytes(transactionSize));
        lock.unlock();
    }

    @Override
    public Block<T> getBlockByIndex(long index) {
        byte[] indexKey = blockIndexKey(index);
        lock.lock();
        byte[] blockHash = db.get(indexKey);
        lock.unlock();

        if (blockHash == null) {
            throw new NonExistObjectException(String.valueOf(index));
        }
        return get(Sha3Hash.createByHashed(blockHash));
    }

    @Override
    public long getBlockchainTransactionSize() {
        // loading db is just first
        if (transactionSize == 0L) {
            lock.lock();
            byte[] txSize = db.get("TRANSACTION_SIZE".getBytes());
            lock.unlock();
            if (txSize != null) {
                transactionSize = ByteUtil.byteArrayToLong(txSize);
            } else {
                return 0L;
            }
        }
        return transactionSize;
    }

    private byte[] blockIndexKey(long index) {
        String blockIndexKey = "BLOCK_INDEX_" + index;
        return HashUtil.sha3(blockIndexKey.getBytes());
    }

}
