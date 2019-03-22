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

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.store.TransactionIndexStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransactionKVIndexerTest {

    private final BranchId branchId = BlockChainTestUtils.createTransferTxHusk().getBranchId();
    private TransactionKVIndexer txIndexer;
    private Set<TransactionHusk> txs;

    @Before
    public void setUp() {
        Set<String> tagsToIndex = new HashSet<>(Arrays.asList("txHash", "method"));
        BranchGroup branchGroup = BlockChainTestUtils.createBranchGroup();
        StoreBuilder storeBuilder = new StoreBuilder(new DefaultConfig());
        txIndexer = new TransactionKVIndexer()
                .setIndexTags(tagsToIndex)
                .setIndexAllTags(false)
                .buildTxIndexStoreMap(branchGroup, storeBuilder);
    }

    @Test
    public void buildTxIndexStoreMap() {
        Assert.assertEquals(1, txIndexer.getTxIndexStoreMap().size());
    }

    @Test
    public void index() {
        TransactionHusk tx = BlockChainTestUtils.createTransferTxHusk();
        txIndexer.index(tx);

        Assert.assertEquals(2, txIndexer.getTxIndexStoreMap().get(branchId).size());
    }

    private void add15TxsBatch() {
        txs = new HashSet<>();

        for (int amount = 0; amount < 15; amount++) {
            txs.add(BlockChainTestUtils.createTransferTxHusk(amount));
        }
        txIndexer.addBatch(txs);
    }

    @Test
    public void addBatch() {
        add15TxsBatch();

        Map<String, TransactionIndexStore> txIndexStoreMap = txIndexer.getTxIndexStoreMap().get(branchId);

        Assert.assertEquals(15, txIndexStoreMap.get("txHash").size());
        Assert.assertEquals(15, txIndexStoreMap.get("method").size());

        List<byte[]> foundTxHashes = txIndexStoreMap.get("method").getByTag("transfer".getBytes());

        Assert.assertEquals(15, foundTxHashes.size());

        for (byte[] foundTxHash : foundTxHashes) {
            TransactionHusk foundTx = new TransactionHusk(txIndexStoreMap.get("txHash").get(foundTxHash));
            Assert.assertTrue(txs.contains(foundTx));
        }

        for (TransactionHusk tx : txs) {
            byte[] txHash = tx.getHash().getBytes();
            Assert.assertTrue(txIndexStoreMap.get("txHash").contains(txHash));
            Assert.assertTrue(txIndexStoreMap.get("method").contains(txHash));
        }
    }

    @Test
    public void getTxByHash() {
        add15TxsBatch();

        for (TransactionHusk tx : txs) {
            Assert.assertTrue(txs.contains(txIndexer.getTxByHash(branchId, tx.getHash())));
            Assert.assertEquals(tx, txIndexer.getTxByHash(branchId, tx.getHash()));
            Assert.assertEquals(tx, txIndexer.getTxByHash(branchId, tx.getHash().getBytes()));
        }
    }

    @Test
    public void search() {
        add15TxsBatch();

        TransactionQuery txQuery = new TransactionQuery()
                .setBranchId(branchId)
                .setTagValue("method", "transfer")
                .setOpCode(TransactionQuery.Operator.OpEqaul);

        Set<TransactionHusk> searchedTxs = txIndexer.search(txQuery);

        Assert.assertEquals(15, searchedTxs.size());

        for (TransactionHusk tx : searchedTxs) {
            Assert.assertTrue(txs.contains(tx));
        }
    }

}