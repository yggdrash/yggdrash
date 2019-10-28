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

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.core.blockchain.Transaction;
import org.ehcache.Cache;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionStoreTest {

    private TransactionStore ts;
    private Transaction tx;

    @Before
    public void setUp() {
        ts = new TransactionStore(new HashMapDbSource());
        assertThat(ts).isNotNull();
        tx = BlockChainTestUtils.createTransferTx();
    }

    @Test
    public void TEST() {
        Cache<String, Integer> pendingPool = CacheManagerBuilder
                .newCacheManagerBuilder().build(true)
                .createCache("txPool", CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(String.class, Integer.class,
                                ResourcePoolsBuilder.heap(Long.MAX_VALUE)));
        Set<String> pendingKeys = new LinkedHashSet<>();

        pendingPool.put("1", 1);
        pendingKeys.add("1");

        pendingPool.put("2", 2);
        pendingKeys.add("2");

        pendingPool.put("3", 3);
        pendingKeys.add("3");

        ArrayList<Integer> haha = new ArrayList<>(pendingPool.getAll(pendingKeys).values());
    }

    @Test
    public void shouldBeCachedByCacheSize() {
        int cacheSize = 5;
        ts = new TransactionStore(new HashMapDbSource(), cacheSize);

        // 캐시 사이즈보다 하나 더 입력
        for (int i = 0; i < cacheSize + 1; i++) {
            tx = BlockChainTestUtils.createTransferTx();
            ts.put(tx.getHash(), tx);
        }

        assertThat(ts.getUnconfirmedTxs().size()).isEqualTo(cacheSize + 1);
        batch();
        assertThat(ts.getUnconfirmedTxs()).isEmpty();
        assertThat(ts.getRecentTxs().size()).isEqualTo(cacheSize);
    }

    @Test
    public void shouldBeGotRecentTxs() {
        ts.put(tx.getHash(), tx);
        batch();
        Collection<Transaction> unconfirmedTxs = ts.getUnconfirmedTxs();
        assertThat(unconfirmedTxs.size()).isEqualTo(0);
        Collection<Transaction> recentTxs = ts.getRecentTxs();
        assertThat(recentTxs.size()).isEqualTo(1);
        assertThat(recentTxs.contains(tx)).isTrue();
    }

    /* 배치가 돌기 전에는 최근 트랜잭션에 들어가지 않고 언컨펌트랜잭션에서만 조회 가능 */
    @Test
    public void shouldNotGetRecentTxsWhenNotBatched() {
        ts.put(tx.getHash(), tx);
        Collection<Transaction> recentTxs = ts.getRecentTxs();
        assertThat(recentTxs).isEmpty();
        Collection<Transaction> unconfirmedTxs = ts.getUnconfirmedTxs();
        assertThat(unconfirmedTxs.size()).isEqualTo(1);
    }

    @Test
    public void shouldGetFromDb() {
        Sha3Hash key = tx.getHash();
        ts.put(tx.getHash(), tx);
        batch();
        Transaction foundTx = ts.get(key);
        assertThat(foundTx).isEqualTo(tx);
    }

    @Test
    public void shouldBeBatched() {
        ts.put(tx.getHash(), tx);
        batch();
        assertThat(ts.getUnconfirmedTxs()).isEmpty();
    }

    @Test
    public void shouldBeGotTxFromCache() {
        Sha3Hash key = tx.getHash();
        ts.put(tx.getHash(), tx);
        Transaction foundTx = ts.get(key);
        assertThat(foundTx).isNotNull();
        assertThat(ts.getUnconfirmedTxs()).isNotEmpty();
    }


    private void batch() {
        Set<Sha3Hash> keys = ts.getUnconfirmedTxs().stream().map(Transaction::getHash).collect(Collectors.toSet());
        ts.batch(keys);
    }
}
