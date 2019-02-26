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
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BlockchainMetaInfo;
import io.yggdrash.core.store.datasource.DbSource;

public class MetaStore implements Store<String, String> {
    private final DbSource<byte[], byte[]> db;

    MetaStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
    }

    @Override
    public void put(String key, String value) {
        db.put(key.getBytes(), value.getBytes());
    }

    @Override
    public String get(String key) {
        return new String(db.get(key.getBytes()));
    }

    @Override
    public boolean contains(String key) {
        return db.get(key.getBytes()) != null;
    }

    @Override
    public void close() {
        db.close();
    }

    public Long getBestBlock() {
        return reStoreToLong(BlockchainMetaInfo.BEST_BLOCK_INDEX.toString(), -1);
    }

    public void setBestBlock(Long index) {
        storeLongValue(BlockchainMetaInfo.BEST_BLOCK_INDEX.toString(), index);
    }

    public void setBestBlock(BlockHusk block) {
        setBestBlockHash(block.getHash());
        setBestBlock(block.getIndex());
    }

    public Sha3Hash getBestBlockHash() {
        byte[] bestBlockHashArray = db.get(BlockchainMetaInfo.BEST_BLOCK.toString().getBytes());
        Sha3Hash bestBlockHash = null;
        if (bestBlockHashArray != null) {
            bestBlockHash = Sha3Hash.createByHashed(bestBlockHashArray);
        }
        return bestBlockHash;
    }

    public void setBestBlockHash(Sha3Hash hash) {
        byte[] bestBlockHash = hash.getBytes();
        db.put(BlockchainMetaInfo.BEST_BLOCK.toString().getBytes(), bestBlockHash);
    }

    public Long getLastExecuteBlockIndex() {
        return reStoreToLong(BlockchainMetaInfo.LAST_EXECUTE_BLOCK_INDEX.toString(), -1);
    }

    public Sha3Hash getLastExecuteBlockHash() {
        byte[] lastBlockBytes = db.get(BlockchainMetaInfo.LAST_EXECUTE_BLOCK.toString().getBytes());
        Sha3Hash lastBlockHash = null;
        if (lastBlockBytes != null) {
            lastBlockHash = Sha3Hash.createByHashed(lastBlockBytes);
        }
        return lastBlockHash;
    }

    public  void setLastExecuteBlock(BlockHusk block) {
        storeLongValue(BlockchainMetaInfo.LAST_EXECUTE_BLOCK_INDEX.toString(), block.getIndex());
        byte[] executeBlockHash = block.getHash().getBytes();
        db.put(BlockchainMetaInfo.LAST_EXECUTE_BLOCK.toString().getBytes(), executeBlockHash);
    }

    private Long reStoreToLong(String key, long defaultValue) {
        byte[] longByteArray = db.get(key.getBytes());
        if (longByteArray == null) {
            return defaultValue;
        } else {
            return Longs.fromByteArray(longByteArray);
        }
    }

    private void storeLongValue(String key, long value) {
        byte[] longValue = Longs.toByteArray(value);
        db.put(key.getBytes(), longValue);
    }



}
