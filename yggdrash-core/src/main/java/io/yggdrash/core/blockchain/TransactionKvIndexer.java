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

package io.yggdrash.core.blockchain;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.store.TransactionIndexStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransactionKvIndexer implements TransactionIndexer {
    private static final Logger log = LoggerFactory.getLogger(TransactionKvIndexer.class);
    private Map<BranchId, Map<String, TransactionIndexStore>> txIndexStoreMap; // <BranchId, <Tag, TxIndexStore>>
    private Set<String> tagsToIndex;
    private boolean indexAllTags;


    public TransactionKvIndexer() {
        // TODO Get tags from nodeProperties.
        //  The tags to be indexed by each branch will be different.
        //  Need to consider how to index which tags to branch.
        //   i.e. txMethod, txOwner, txStatus, blockId, blockNum, account ... etc
        //this.indexAllTags = false;
        //buildTxIndexStoreMap(branchGroup, storeBuilder);
    }

    // IndexTags is an option for setting which tags to index.
    public TransactionKvIndexer setIndexTag(String tag) {
        this.tagsToIndex = new HashSet<>();
        tagsToIndex.add(tag);
        return this;
    }

    public TransactionKvIndexer setIndexTags(Set<String> tags) {
        tagsToIndex = tags;
        return this;
    }

    // IndexAllTags is an option for indexing all tags. (not currently used)
    public TransactionKvIndexer setIndexAllTags(boolean on) {
        indexAllTags = on;
        return this;
    }

    // BuildTxIndexStoreMap creates new KV indexer for each branch and builds the store map
    public TransactionKvIndexer buildTxIndexStoreMap(BranchGroup branchGroup, StoreBuilder storeBuilder) {
        txIndexStoreMap = new HashMap<>();

        for (BranchId branchId : branchGroup.getAllBranchId()) {
            for (String tag : tagsToIndex) {
                TransactionIndexStore txIndexStore = storeBuilder.buildTransactionIndexStore(branchId, tag);

                if (!txIndexStoreMap.containsKey(branchId)) {
                    Map<String, TransactionIndexStore> tagMap = new HashMap<>();
                    tagMap.put(tag, txIndexStore);
                    txIndexStoreMap.put(branchId, tagMap);
                } else {
                    txIndexStoreMap.get(branchId).put(tag, txIndexStore);
                }
            }
        }
        return this;
    }

    Map<BranchId, Map<String, TransactionIndexStore>> getTxIndexStoreMap() { // for test!
        return txIndexStoreMap;
    }

    // AddBatch indexes a batch of transactions using the given list of tags.
    @Override
    public void addBatch(List<TransactionHusk> txs) {
        for (TransactionHusk tx : txs) {
            index(tx);
        }
    }

    // Index indexes a single transaction using the given list of tags.
    @Override
    public void index(TransactionHusk tx) {
        indexByTags(tx);
        indexTxByHash(tx);
    }

    // Get gets transaction from the TxIndex storage(db) and returns it or null if the transaction is not found
    @Override
    public TransactionHusk getTxByHash(BranchId branchId, byte[] txHash) {
        if (txIndexStoreMap.containsKey(branchId) && txIndexStoreMap.get(branchId).get("txHash").contains(txHash)) {
            return new TransactionHusk(txIndexStoreMap.get(branchId).get("txHash").get(txHash));
        }
        return null;
    }

    // SearchTxHash and SearchTxHashes are only for searching txHash
    public TransactionHusk getTxByHash(BranchId branchId, Sha3Hash txHash) {
        return getTxByHash(branchId, txHash.getBytes());
    }

    // Search performs a search using the given query.
    // TODO We provide the conditions of the range queries (like 'tx.height > 5").
    // For each condition, it queries the DB index.
    // One special use cases here :
    // if "tx.hash" is found, it returns tx result for it ==> we'll just return txs(don't have spec yet)
    // In the case of range query, it is better for the client to display in the browser
    // but it's possible to always perform a full scan if there is no boundary.
    // Results from querying indexes are then intersected and returned to the caller.
    @Override
    public Set<TransactionHusk> search(TransactionQuery txQuery) {
        BranchId branchId = txQuery.getBranchId();
        Set<byte[]> hashes = new HashSet<>();

        // if there is a hash condition, return the result immediately
        if (txQuery.containTag("txHash")) {
            for (TransactionQuery.Condition condition : txQuery.getConditions()) {
                hashes.add(new Sha3Hash(condition.getValue()).getBytes());
            }
            return searchTxHashes(branchId, hashes);
        }

        // get a list of conditions (like 'tx.height > 5") except for txHash tag
        if (!txQuery.containTag("txHash") || txQuery.tagCount() > 1) {
            for (TransactionQuery.Condition condition : txQuery.getConditions()) {
                if (!condition.getTag().equals("txHash")) {
                    String tag = condition.getTag();
                    byte[] value = condition.getValue().getBytes();
                    hashes = new HashSet<>(txIndexStoreMap.get(branchId).get(tag).getByTag(value));
                }
            }
        }

        return searchTxHashes(branchId, hashes);
    }

    @Override
    public void chainedBlock(BlockHusk block) {
        // Extracts txs from block and indexes them by tags
        addBatch(block.getBody());
    }

    @Override
    public void receivedTransaction(TransactionHusk tx) {

    }

    Set<TransactionHusk> searchTxHashes(BranchId branchId, Set<byte[]> txHashes) {
        Set<TransactionHusk> res = new HashSet<>();

        for (byte[] txHash : txHashes) {
            res.add(getTxByHash(branchId, txHash));
        }
        return res;
    }

    Set<TransactionHusk> searchTxHashesBySha3(BranchId branchId, Set<Sha3Hash> txHashes) {
        Set<TransactionHusk> res = new HashSet<>();

        for (Sha3Hash txHash : txHashes) {
            res.add(getTxByHash(branchId, txHash));
        }
        return res;
    }

    private void indexByTags(TransactionHusk tx) {
        for (String tag : tagsToIndex) {
            if (!tag.equals("txHash") && (indexAllTags || tagsToIndex.size() > 1)) {
                BranchId branchId = tx.getBranchId();
                byte[] txHash = tx.getHashByte();
                byte[] keyForTag = tx.getPropertyByTag(tag).getBytes();

                //txIndexStoreMap.get(branchId).get(tag).put(keyForTag, txHash); // <txProperty, txHash>
                // TODO Considering create an unique key by a separator like slash or a character.
                txIndexStoreMap.get(branchId).get(tag).put(txHash, keyForTag); // <txHash, txProperty>

                log.debug("[TxIndexer] indexByTags :: Size of ({})store => {}",
                        tag, txIndexStoreMap.get(branchId).get(tag).size());
                log.debug("[TxIndexer] indexByTags :: ({})Store contains txHash({}) => {}",
                        tag, tx.getHash(), txIndexStoreMap.get(branchId).get(tag).contains(txHash));
            }
        }
    }

    private void indexTxByHash(TransactionHusk tx) {
        BranchId branchId = tx.getBranchId();
        byte[] txHash = tx.getHashByte();
        byte[] txData = tx.getData();

        txIndexStoreMap.get(branchId).get("txHash").put(txHash, txData); // <txHash, tx>

        log.debug("[TxIndexer] IndexTxByHash :: Size of (TxHash)store => {}",
                txIndexStoreMap.get(branchId).get("txHash").size());
        log.debug("[TxIndexer] IndexTxByHash :: (TxHash])Store contains txHash({}) => {}",
                tx.getHash(), txIndexStoreMap.get(branchId).get("txHash").contains(txHash));
    }
}
