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

import com.google.common.primitives.Longs;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.store.datasource.DbSource;

public class BlockStore implements Store<Sha3Hash, BlockHusk> {
    private final DbSource<byte[], byte[]> db;
    private long transactionSize;


    BlockStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
        // get Transaction Size
        transactionSize = getBlockchainTransactionSize();
    }

    @Override
    public void put(Sha3Hash key, BlockHusk value) {
        db.put(key.getBytes(), value.getData());
    }

    @Override
    public BlockHusk get(Sha3Hash key) {
        byte[] foundValue = db.get(key.getBytes());
        if (foundValue != null) {
            return new BlockHusk(foundValue);
        }
        throw new NonExistObjectException("Not Found [" + key + "]");
    }

    @Override
    public boolean contains(Sha3Hash key) {
        return db.get(key.getBytes()) != null;
    }

    public void close() {
        this.db.close();
    }


    public void addBlock(BlockHusk block) {
        // Add BlockIndex and Add Block Data
        long index = block.getIndex();
        byte[] indexKey = blockIndexKey(index);
        // store block index
        db.put(indexKey, block.getHash().getBytes());
        // store block data
        db.put(block.getHash().getBytes(), block.getData());
        // add block Transaction size
        transactionSize += block.getBodyCount();
        db.put("TRANSACTION_SIZE".getBytes(), Longs.toByteArray(transactionSize));
    }

    public BlockHusk getBlockByIndex(long index) {
        byte[] indexKey = blockIndexKey(index);
        byte[] blockHash = db.get(indexKey);
        if (blockHash == null) {
            return null;
        }
        byte[] blockData = db.get(blockHash);
        if (blockData == null) {
            return null;
        }
        return new BlockHusk(blockData);
    }

    public long getBlockchainTransactionSize() {
        // loading db is just first
        if (transactionSize == 0L) {
            byte[] txSize = db.get("TRANSACTION_SIZE".getBytes());
            if (txSize != null) {
                transactionSize = Longs.fromByteArray(txSize);
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
