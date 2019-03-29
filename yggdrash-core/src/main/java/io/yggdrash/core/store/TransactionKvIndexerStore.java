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

import io.yggdrash.common.store.datasource.DbSource;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.blockchain.TransactionQuery;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public class TransactionKvIndexerStore implements ReadWriterStore<byte[], byte[]> {
    private final String tagKeySeparator = "/";
    private final String tagKeyFormat = "%s/%s/%d";
    private final DbSource<byte[], byte[]> db;
    private Map<String, Integer> heightMap;

    TransactionKvIndexerStore(DbSource<byte[], byte[]> db) {
        this.db = db.init();
        this.heightMap = new HashMap<>();
    }

    @Override
    public void put(byte[] keyForTag, byte[] txHash) {
        db.put(keyForTag, txHash);
    }

    public void put(String tag, String value, byte[] txHash) { // i.e. ("method", "transfer", "0x0...")
        put(getKeyForTag(tag, value), txHash);
        heightMap.put(tag, getNextHeight(tag));
    }

    @Override
    public boolean contains(byte[] key) {
        return db.get(key) != null;
    }

    boolean containsTag(String tag) {
        return heightMap.keySet().contains(tag);
    }

    boolean containsValue(String tag, String value) {
        if (containsTag(tag)) {
            for (int i = 0; i < getCurHeight(tag) + 1; i++) {
                if (contains(createKeyForTag(tag, value, i))) {
                    return true;
                }
            }
        }
        return false;
    }

    Set<String> getTags() {
        return heightMap.keySet();
    }

    @Override
    public void close() {
        this.db.close();
    }

    @Override
    public byte[] get(byte[] key) {
        return Optional.ofNullable(db.get(key)).orElseThrow(NoSuchElementException::new);
    }

    // Search returns txHashes found by tag and value.
    public Set<byte[]> search(int opCode, String tag, String value) {
        if (opCode == TransactionQuery.Operator.OpEqaul) { //TODO Implement a search function for other operators
            return search(tag, value);
        }
        return new HashSet<>();
    }

    Set<byte[]> search(String tag, String value) { // opEquals
        Set<byte[]> txHashes = new HashSet<>();
        if (containsTag(tag)) {
            for (int i = 0; i < getCurHeight(tag) + 1; i++) {
                byte[] keyForTag = createKeyForTag(tag, value, i);
                if (contains(keyForTag)) {
                    txHashes.add(get(keyForTag));
                }
            }
        }
        return txHashes;
    }

    // i.e. method == transfer && account == 0x0
    public Set<byte[]> intersection(String tag1, String value1, String tag2, String value2) {
        Set<byte[]> list1 = search(tag1, value1);
        Set<byte[]> list2 = search(tag2, value2);
        Set<byte[]> result = new HashSet<>();

        for (byte[] txHash1 : list1) {
            for (byte[] txHash2 : list2) {
                if (Arrays.equals(txHash1, txHash2)) {
                    result.add(txHash1);
                }
            }
        }

        return result;
    }

    // For transaction index
    private int getCurHeight(String tag) {
        return Optional.ofNullable(heightMap.get(tag)).orElse(0);
    }

    private int getNextHeight(String tag) {
        return containsTag(tag) ? getCurHeight(tag) + 1 : 0;
    }

    private byte[] getKeyForTag(String tag, String value) { // i.e. "method/transfer/0", "method/transfer/1" ...
        return String.format(tagKeyFormat, tag, value, getNextHeight(tag)).getBytes();
    }

    private byte[] createKeyForTag(String tag, String value, int height) {
        return String.format(tagKeyFormat, tag, value, height).getBytes();
    }

}
