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

import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.ChainId;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.datasource.DbSource;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class BlockStore implements Store<Sha3Hash, BlockHusk> {
    private static final Logger logger = LoggerFactory.getLogger(BlockStore.class);

    private DbSource<byte[], byte[]> db;

    public BlockStore(DbSource<byte[], byte[]> dbSource) {
        this.db = dbSource.init();
    }

    public BlockStore(ChainId chainId) {
        this(chainId.toString());
    }

    public BlockStore(String chainId) {
        this.db = new LevelDbDataSource(chainId + "/blocks").init();
    }

    @Override
    public void put(Sha3Hash key, BlockHusk value) {
        this.db.put(key.getBytes(), value.getData());
    }

    @Override
    public BlockHusk get(Sha3Hash key) {
        byte[] foundValue = db.get(key.getBytes());
        try {
            if (foundValue != null) {
                return new BlockHusk(foundValue);
            }
        } catch (InvalidProtocolBufferException e) {
            logger.warn("InvalidProtocolBufferException: {}", e);
        }

        throw new NonExistObjectException("Not Found [" + key + "]");
    }

    @Override
    public Set<BlockHusk> getAll() {
        try {
            List<byte[]> dataList = db.getAll();
            TreeSet<BlockHusk> blockSet = new TreeSet<>();
            for (byte[] data : dataList) {
                blockSet.add(new BlockHusk(data));
            }
            return blockSet;
        } catch (IOException e) {
            throw new NotValidateException(e);
        }
    }

    @Override
    public boolean contains(Sha3Hash key) {
        return db.get(key.getBytes()) != null;
    }

    public long size() {
        return this.db.count();
    }

    public void close() {
        this.db.close();
    }
}
