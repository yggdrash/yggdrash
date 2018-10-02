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
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.datasource.DbSource;
import io.yggdrash.core.store.datasource.LevelDbDataSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockStore implements Store<Sha3Hash, BlockHusk> {
    private DbSource<byte[], byte[]> db;
    private Map<Long, Sha3Hash> index = new HashMap<>();

    public BlockStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
        indexing();
    }

    public BlockStore(BranchId branchId) {
        this(branchId.toString());
    }

    public BlockStore(String branchId) {
        this.db = new LevelDbDataSource(branchId + "/blocks").init();
        indexing();
    }

    private void indexing() {
        try {
            List<byte[]> dataList = db.getAll();
            for (byte[] data : dataList) {
                BlockHusk block = new BlockHusk(data);
                index.put(block.getIndex(), block.getHash());
            }
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    @Override
    public void put(Sha3Hash key, BlockHusk value) {
        db.put(key.getBytes(), value.getData());
        index.put(value.getIndex(), key);
    }

    @Override
    public BlockHusk get(Sha3Hash key) {
        byte[] foundValue = db.get(key.getBytes());
        if (foundValue != null) {
            return new BlockHusk(foundValue);
        }

        throw new NonExistObjectException("Not Found [" + key + "]");
    }

    public BlockHusk get(long idx) {
        if (!index.containsKey(idx)) {
            return null;
        }
        return get(index.get(idx));
    }

    @Override
    public boolean contains(Sha3Hash key) {
        return db.get(key.getBytes()) != null;
    }

    public long size() {
        return index.size();
    }

    public void close() {
        this.db.close();
    }
}
