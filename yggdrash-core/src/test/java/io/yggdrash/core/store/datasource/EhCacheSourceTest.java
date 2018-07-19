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

package io.yggdrash.core.store.datasource;

import io.yggdrash.TestUtils;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.store.StoreConfiguration;
import org.ehcache.Cache;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {StoreConfiguration.class})
public class EhCacheSourceTest {
    private static final Logger log = LoggerFactory.getLogger(EhCacheSourceTest.class);

    @Autowired
    Cache<String, Transaction> cache;

    @Test
    public void shouldGetTx() throws IOException {
        Transaction dummyTx = TestUtils.createDummyTx();
        cache.put(dummyTx.getHashString(), dummyTx);
        Transaction foundTx = cache.get(dummyTx.getHashString());
        assertThat(foundTx).isEqualTo(dummyTx);
    }

    @Test
    public void shouldPutTx() throws IOException {
        Transaction dummyTx = TestUtils.createDummyTx();
        cache.put(dummyTx.getHashString(), dummyTx);
    }

    @Test
    public void shouldBeLoadedBean() {
        assertThat(cache).isNotNull();
    }
}
