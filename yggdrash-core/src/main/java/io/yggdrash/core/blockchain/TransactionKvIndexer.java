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

import com.google.common.primitives.Longs;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.store.TransactionHashStore;
import io.yggdrash.core.store.TransactionHeightStore;
import io.yggdrash.core.store.TransactionKvIndexerStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransactionKvIndexer implements TransactionIndexer {
    private static final Logger log = LoggerFactory.getLogger(TransactionKvIndexer.class);
    private static final String tagKeySeparator = "/";

    private Map<BranchId, TransactionKvIndexerStore> txKvIndexerStoreMap; // <BranchId, TxIndexerStore>
    private Map<BranchId, TransactionHashStore> txHashStoreMap; // <BranchId, TxHashStore>
    private Map<BranchId, TransactionHeightStore> txHeightStoreMap; // <BranchId, TxHeightStore>

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
    public TransactionKvIndexer buildTxKvIndexerStoreMap(BranchGroup branchGroup, StoreBuilder storeBuilder) {
        for (BranchId branchId : branchGroup.getAllBranchId()) {
            for (String tag : tagsToIndex) {

                //if (tag.equals("txHash")) { // default option!
                txHashStoreMap = new HashMap<>();
                TransactionHashStore txHashStore = storeBuilder.buildTransactionHashStore(branchId);
                txHashStoreMap.put(branchId, txHashStore);
                //}

                if (tag.equals("txHeight")) {
                    txHeightStoreMap = new HashMap<>();
                    TransactionHeightStore txHeightStore = storeBuilder.buildTransactionHeightStore(branchId);
                    txHeightStoreMap.put(branchId, txHeightStore);
                }

                txKvIndexerStoreMap = new HashMap<>();
                TransactionKvIndexerStore txKvIndexerStore = storeBuilder.buildTransactionKvIndexerStore(branchId);
                txKvIndexerStoreMap.put(branchId, txKvIndexerStore);
            }
        }
        return this;
    }

    Map<BranchId, TransactionKvIndexerStore> getTxKvIndexerStoreMap() { // for debugging!
        return txKvIndexerStoreMap;
    }

    Map<BranchId, TransactionHashStore> getTxHashStoreMap() {   // for debugging!
        return txHashStoreMap;
    }

    Map<BranchId, TransactionHeightStore> getTxHeightStoreMap() {   // for debugging!
        return txHeightStoreMap;
    }

    Set<String> getTagsToIndex() { // for debugging!
        return tagsToIndex;
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
        indexTxByHash(tx); // txHash is default option

        if (indexAllTags || tagsToIndex.size() > 1) {
            indexByTags(tx);
        }

        if (tagsToIndex.contains("txHeight")) {
            indexTxByHeight(tx);
        }
    }

    // Get gets transaction from the TxIndex storage(db) and returns it or null if the transaction is not found
    @Override
    public TransactionHusk getTxByHash(BranchId branchId, byte[] txHash) {
        byte[] checkedTxHash = isTxHash(txHash);
        return txHashStoreMap.containsKey(branchId) && txHashStoreMap.get(branchId).contains(checkedTxHash)
                ? new TransactionHusk(txHashStoreMap.get(branchId).get(checkedTxHash)) : null;
    }

    // SearchTxHash and SearchTxHashes are only for searching txHash
    public TransactionHusk getTxByHash(BranchId branchId, Sha3Hash txHash) {
        return txHashStoreMap.containsKey(branchId) && txHashStoreMap.get(branchId).contains(txHash)
                ? new TransactionHusk(txHashStoreMap.get(branchId).get(txHash)) : null;
    }


    private byte[] isTxHash(byte[] txHash) {
        if (txHash.length > 32) { // The length of general txHash
            String txHashStr = new String(txHash);
            return new Sha3Hash(txHashStr.substring(0, txHashStr.indexOf("/"))).getBytes();
        }
        return txHash;
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
        Set<byte[]> txHashes = new HashSet<>();

        // if there is a txHash condition, return the result immediately
        if (txQuery.containTag("txHash")) {
            for (TransactionQuery.Condition condition : txQuery.getConditions()) {
                txHashes.add(new Sha3Hash(condition.getValue()).getBytes());
            }
            return searchTxHashes(branchId, txHashes);
        }

        // if there is a txHeight condition, return the result immediately
        if (txQuery.containTag("txHeight")) { // TODO Requires specification and testing for txHeightStore
            for (TransactionQuery.Condition condition : txQuery.getConditions()) {
                txHashes.add(txHeightStoreMap.get(branchId).get(condition.getValue().getBytes()));
            }
            return searchTxHashes(branchId, txHashes);
        }

        // get a list of conditions except for txHash tag
        if (txQuery.tagCount() > 0) {
            txHashes.addAll(batchCondition(txQuery));
        }

        return searchTxHashes(branchId, txHashes);
    }

    // Intersection searches txHashes by queries and get txData from txHashStore
    public Set<TransactionHusk> intersection(TransactionQuery q1, TransactionQuery q2) { // && operator
        if (!q1.getBranchId().equals(q2.getBranchId())) {
            return new HashSet<>();
        }

        if (q1.containTag("txHash") || q2.containTag("txHash")) {
            return new HashSet<>();
        }

        return searchTxHashes(q1.getBranchId(), batchCondition(q1, q2));
    }

    private Set<byte[]> batchCondition(TransactionQuery txQuery) {
        Set<byte[]> hashes = new HashSet<>();
        for (TransactionQuery.Condition condition : txQuery.getConditions()) {
            int opCode = condition.getOperator();
            String tag = condition.getTag();
            String value = condition.getValue();

            hashes.addAll(txKvIndexerStoreMap.get(txQuery.getBranchId()).search(opCode, tag, value));
        }
        return hashes;
    }

    private Set<byte[]> batchCondition(TransactionQuery q1, TransactionQuery q2) {
        BranchId branchId = q1.getBranchId();
        Set<byte[]> txHashes = new HashSet<>();

        for (TransactionQuery.Condition con1 : q1.getConditions()) {
            String t1 = con1.getTag();
            String v1 = con1.getValue();
            for (TransactionQuery.Condition con2 : q2.getConditions()) {
                String t2 = con2.getTag();
                String v2 = con2.getValue();

                txHashes.addAll(txKvIndexerStoreMap.get(branchId).intersection(t1, v1, t2, v2));
            }
        }
        return txHashes;
    }

    @Override
    public void chainedBlock(BlockHusk block) {
        // Extracts txs from block and indexes them by tags
        addBatch(block.getBody());
    }

    @Override
    public void receivedTransaction(TransactionHusk tx) {

    }

    private Set<TransactionHusk> searchTxHashes(BranchId branchId, Set<byte[]> txHashes) {
        Set<TransactionHusk> res = new HashSet<>();

        for (byte[] txHash : txHashes) {
            res.add(getTxByHash(branchId, txHash));
        }
        return res;
    }

    private Set<TransactionHusk> searchTxHashesBySha3(BranchId branchId, Set<Sha3Hash> txHashes) {
        Set<TransactionHusk> res = new HashSet<>();

        for (Sha3Hash txHash : txHashes) {
            res.add(getTxByHash(branchId, txHash));
        }
        return res;
    }


    //TODO address == tx Author
    private void indexByTags(TransactionHusk tx) {
        BranchId branchId = tx.getBranchId();

        if (!txKvIndexerStoreMap.containsKey(branchId)) {
            return;
        }

        for (String tag : tagsToIndex) { // TODO if tag.equals("account")
            if (checkTag(tag)) {
                List<String> values = tx.getPropertiesByTag(tag);

                for (int i = 0; i < values.size(); i++) {
                    String value = values.get(i);
                    byte[] txHash = String.format("%s/%d", tx.getHash().toString(), i).getBytes();

                    txKvIndexerStoreMap.get(branchId).put(tag, value, txHash);
                }
            }
        }
    }

    private boolean checkTag(String tag) {
        return !tag.equals("txHash") && !tag.equals("txHeight");
    }

    private void indexTxByHash(TransactionHusk tx) {
        BranchId branchId = tx.getBranchId();
        byte[] txHash = tx.getHashByte();
        byte[] txData = tx.getData();

        txHashStoreMap.get(branchId).put(txHash, txData);
    }

    private void indexTxByHeight(TransactionHusk tx) {
        BranchId branchId = tx.getBranchId();
        byte[] txHash = tx.getHashByte();
        byte[] tmpHeight = Longs.toByteArray(0L); // TODO What height to tag? blockHeight?

        if (txHeightStoreMap.containsKey(branchId)) {
            txHeightStoreMap.get(branchId).put(tmpHeight, txHash);
        }
    }

}
