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

import io.yggdrash.TestUtils;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionStoreTest {

    private TransactionStore ts;
    private TransactionHusk tx;

    @Before
    public void setUp() {
        ts = new TransactionStore(new HashMapDbSource());
        assertThat(ts).isNotNull();
        tx = TestUtils.createTransferTxHusk();
    }

    @Test
    public void shouldGetFromDb() {
        Sha3Hash key = tx.getHash();
        ts.put(tx.getHash(), tx);
        batch();
        TransactionHusk transactionHusk = ts.get(key);
        assertThat(transactionHusk).isEqualTo(tx);
    }

    @Test
    public void shouldBeBatched() {
        ts.put(tx.getHash(), tx);
        batch();
        assertThat(ts.getUnconfirmedTxs()).isEmpty();
        assertThat(ts.getAll().size()).isEqualTo(1L);
    }

    @Test
    public void shouldBeGotTxFromCache() {
        Sha3Hash key = tx.getHash();
        ts.put(tx.getHash(), tx);
        TransactionHusk foundTx = ts.get(key);
        assertThat(foundTx).isNotNull();
        assertThat(ts.getUnconfirmedTxs()).isNotEmpty();
    }

    private void batch() {
        Set<Sha3Hash> keys = ts.getUnconfirmedTxs().stream().map(TransactionHusk::getHash)
                .collect(Collectors.toSet());
        ts.batch(keys);
    }
}
