/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.core.store;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.contract.core.store.ReadWriterStore;

import java.util.NoSuchElementException;
import java.util.Optional;

public class TransactionHashStore implements ReadWriterStore<byte[], byte[]> {
    private final DbSource<byte[], byte[]> db;

    TransactionHashStore(DbSource<byte[], byte[]> db) {
        this.db = db.init();
    }

    @Override
    public void put(byte[] txHash, byte[] txData) {
        db.put(txHash, txData);
    }

    @Override
    public boolean contains(byte[] txHash) {
        return db.get(txHash) != null;
    }

    public boolean contains(Sha3Hash txHash) {
        return contains(txHash.getBytes());
    }

    @Override
    public void close() {
        this.db.close();
    }

    @Override
    public byte[] get(byte[] txHash) {
        return Optional.ofNullable(db.get(txHash)).orElseThrow(NoSuchElementException::new);
    }

    public byte[] get(Sha3Hash txHash) {
        return get(txHash.getBytes());
    }
}
