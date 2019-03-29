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

import com.google.gson.JsonParser;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.store.TransactionKvIndexerStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TransactionKvIndexerTest {

    private final BranchId branchId = BlockChainTestUtils.createTransferTxHusk().getBranchId();
    private TransactionKvIndexer txIndexer;
    private List<TransactionHusk> txs;
    private int txBodyLength = 10;

    @Before
    public void setUp() {
        Set<String> tagsToIndex = new HashSet<>(Arrays.asList("txHash", "method", "contractVersion"));
        BranchGroup branchGroup = BlockChainTestUtils.createBranchGroup();
        StoreBuilder storeBuilder = new StoreBuilder(new DefaultConfig());
        txIndexer = new TransactionKvIndexer()
                .setIndexTags(tagsToIndex)
                .setIndexAllTags(false)
                .buildTxKvIndexerStoreMap(branchGroup, storeBuilder);
    }

    @Test
    public void buildTxIndexStoreMap() {
        Assert.assertEquals(1, txIndexer.getTxKvIndexerStoreMap().size());
        Assert.assertEquals(1, txIndexer.getTxHashStoreMap().size());
        Assert.assertEquals(3, txIndexer.getTagsToIndex().size());
        Assert.assertNull(txIndexer.getTxHeightStoreMap());
    }

    @Test
    public void getTxByHash() {
        add15TxsBatch();

        for (TransactionHusk tx : txs) {
            Assert.assertTrue(txs.contains(txIndexer.getTxByHash(branchId, tx.getHash()))); // byte len : 32
            Assert.assertEquals(tx, txIndexer.getTxByHash(branchId, tx.getHash()));
            Assert.assertEquals(tx, txIndexer.getTxByHash(branchId, tx.getHashByte()));

            byte[] testTxHash = String.format("%s/%d", tx.getHash().toString(), 0).getBytes(); // byte len : 66
            Assert.assertTrue(txs.contains(txIndexer.getTxByHash(branchId, testTxHash)));
        }
    }

    private TransactionHusk create1Tx() {
        txs = new ArrayList<>();
        TransactionHusk tx = BlockChainTestUtils.createMultiTransferTxHusk(txBodyLength);
        txs.add(tx);

        new JsonParser().parse(tx.getBody()).getAsJsonArray().forEach(ele -> {
            System.out.println(ele.getAsJsonObject());
        });

        return tx;
    }

    @Test
    public void index() {
        txIndexer.index(create1Tx());

        Assert.assertNotNull(txIndexer.getTxKvIndexerStoreMap().get(branchId));
        Assert.assertNotNull(txIndexer.getTxHashStoreMap().get(branchId));
        Assert.assertNull(txIndexer.getTxHeightStoreMap());

        check_found_txHash_by_feyForTag();
    }

    private void check_found_txHash_by_feyForTag() {
        TransactionKvIndexerStore txKvIndexerStore = txIndexer.getTxKvIndexerStoreMap().get(branchId);

        Set<String> txHashes = new HashSet<>();
        Set<String> txHashesWithoutHeight = new HashSet<>();

        // 1개의 TxBody 길이가 10인 Tx 15개 처리된 경우. 총 150개의 key 가 존재함
        int lastHeight = txBodyLength * txs.size(); // 10 * 15

        for (int height = 0; height < lastHeight; height++) {
            /*
            txBody 는 다수 tx를 처리할 수 있도록 여러 tx 을 포함하고 있다.
            같은 txHash 아래 각기 다른 tx 를 구분하기 위해 height 를 붙여주었다.
            아래 foundTxHasStr 이 DB의 Value 값으로 사용된다.
            즉, {"method/transfer/9" : "6ba2e5b8706c7d2454bf14b9c3734089cb34a7262b6d0f99d4fae10652ced686/9"} 으로 저장한다.

            methodKey => "method/transfer/0", "contractVersion"/d79..d93/0"
            foundTxHashStr => 6ba2e5b8706c7d2454bf14b9c3734089cb34a7262b6d0f99d4fae10652ced686/0 ... /9
            foundTxHashWithoutHeight => 6ba2e5b8706c7d2454bf14b9c3734089cb34a7262b6d0f99d4fae10652ced686
             */
            byte[] methodKey = getKeyForTag("method", "transfer", height);
            byte[] foundMethodTxHash = txKvIndexerStore.get(methodKey);

            String foundMethodTxHashStr = new String(foundMethodTxHash);
            String foundMethodTxHashStrWithoutHeight
                    = foundMethodTxHashStr.substring(0, foundMethodTxHashStr.indexOf("/"));

            txHashes.add(foundMethodTxHashStr);
            txHashesWithoutHeight.add(foundMethodTxHashStrWithoutHeight);

            byte[] contractVersionKey
                    = getKeyForTag("contractVersion", "d79ab8e1d735090d2a7ef4f16d13a910457c0d93", height);
            byte[] foundContractVersionTxHash = txKvIndexerStore.get(contractVersionKey);
            String foundContractVersionTxHashStr = new String(foundContractVersionTxHash);
            String foundContractVersionTxHashStrWithoutHeight
                    = foundMethodTxHashStr.substring(0, foundContractVersionTxHashStr.indexOf("/"));

            txHashes.add(foundContractVersionTxHashStr);
            txHashesWithoutHeight.add(foundContractVersionTxHashStrWithoutHeight);
        }

        /*
        txHash 를 구분하는 height 를 제거하지 않으면 총 key 의 개수와 같고
        txHash 를 구분하는 height 제거하고 나면 같은 hash 가 Tx 개수만큼 존재한다
        */
        Assert.assertEquals(lastHeight, txHashes.size());
        Assert.assertEquals(txs.size(), txHashesWithoutHeight.size());

        for (String foundTx : txHashesWithoutHeight) {
            check_txHash_contained_in_txHashStore(foundTx);
        }
    }

    private void check_txHash_contained_in_txHashStore(String foundTxHash) {
        TransactionHusk foundTx = txIndexer.getTxByHash(branchId, new Sha3Hash(foundTxHash));
        Assert.assertTrue(txs.contains(foundTx));
    }

    private byte[] getKeyForTag(String tag, String value, int height) {
        return String.format("%s/%s/%d", tag, value, height).getBytes();
    }

    private void create15Txs() {
        txs = new ArrayList<>();

        for (int i = 0; i < 15; i++) {
            TransactionHusk tx = BlockChainTestUtils.createMultiTransferTxHusk(txBodyLength);
            txs.add(tx);
        }
    }

    private void add15TxsBatch() {
        create15Txs();
        txIndexer.addBatch(txs);
    }

    @Test
    public void addBatch() {
        add15TxsBatch();
        check_found_txHash_by_feyForTag();
    }

    @Test
    public void chainedBlock() {
        create15Txs();
        BlockHusk chainedBlock = BlockChainTestUtils.createNextBlock(BlockChainTestUtils.genesisBlock(), txs);
        txIndexer.chainedBlock(chainedBlock);

        for (TransactionHusk tx : txs) {
            Assert.assertTrue(txs.contains(txIndexer.getTxByHash(branchId, tx.getHash())));
            Assert.assertEquals(tx, txIndexer.getTxByHash(branchId, tx.getHash()));
            Assert.assertEquals(tx, txIndexer.getTxByHash(branchId, tx.getHashByte()));
        }
    }

    @Test
    public void search() {
        add15TxsBatch();

        TransactionQuery txQuery1 = new TransactionQuery()
                .setBranchId(branchId)
                .setTagValue("method", "transfer")
                .setOpCode(TransactionQuery.Operator.OpEqaul);

        List<TransactionHusk> searchedTxs1 = new ArrayList<>(txIndexer.search(txQuery1));

        Assert.assertEquals(15, searchedTxs1.size());
        check_found_Tx_contain_in_txs(searchedTxs1);

        TransactionQuery txQuery2 = new TransactionQuery()
                .setBranchId(branchId)
                .setTagValue("txHash", txs.get(0).getHash().toString())
                .setOpCode(TransactionQuery.Operator.OpEqaul);

        List<TransactionHusk> searchedTxs2 = new ArrayList<>(txIndexer.search(txQuery2));
        Assert.assertEquals(1, searchedTxs2.size());
        Assert.assertTrue(txs.contains(searchedTxs2.get(0)));

        TransactionQuery txQuery3 = new TransactionQuery()
                .setBranchId(branchId)
                .setTagValue("contractVersion", "d79ab8e1d735090d2a7ef4f16d13a910457c0d93")
                .setOpCode(TransactionQuery.Operator.OpEqaul);

        List<TransactionHusk> searchedTxs3 = new ArrayList<>(txIndexer.search(txQuery3));

        Assert.assertEquals(15, searchedTxs3.size());
        check_found_Tx_contain_in_txs(searchedTxs3);

        // Intersection test
        // {condition => method/transfer && contractVersion/d79ab8e1d735090d2a7ef4f16d13a910457c0d93}
        List<TransactionHusk> searchedTxs4 = new ArrayList<>(txIndexer.intersection(txQuery1, txQuery2));
        Assert.assertEquals(0, searchedTxs4.size());

        List<TransactionHusk> searchedTxs5 = new ArrayList<>(txIndexer.intersection(txQuery1, txQuery3));
        Assert.assertEquals(15, searchedTxs5.size());
        check_found_Tx_contain_in_txs(searchedTxs5);
    }

    private void check_found_Tx_contain_in_txs(List<TransactionHusk> result) {
        for (TransactionHusk tx : result) {
            Assert.assertTrue(txs.contains(tx));
        }
    }
}