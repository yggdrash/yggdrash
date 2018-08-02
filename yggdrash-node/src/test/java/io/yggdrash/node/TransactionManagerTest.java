/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node;

import com.google.gson.JsonObject;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
//(properties = "yggdrash.node.grpc.port=0")
@IfProfileValue(name = "spring.profiles.active", value = "ci")
public class TransactionManagerTest {

    @Autowired
    TransactionManager tm;

    @Autowired
    NodeManager nodeManager;

    @Before
    public void setUp() {
        tm.flush();
    }

    @Test
    public void shouldGetFromDb() {
        Transaction dummyTx = TestUtils.createDummyTx();
        tm.put(dummyTx);
        tm.batchAll();
        assertThat(tm.count()).isZero();
        Transaction foundValue = tm.get(dummyTx.getHashString());
        assertThat(foundValue).isNotNull();
    }

    @Test
    public void shouldBatch() {
        byte[] key = TestUtils.createDummyTx().getHash();
        tm.batchAll();
        assertThat(tm.count()).isZero();
    }

    @Test
    public void shouldGetFromPool() {
        Transaction dummyTx = TestUtils.createDummyTx();
        byte[] key = dummyTx.getHash().clone();
        tm.put(dummyTx);
        Transaction foundValue = tm.get(dummyTx.getHashString());
        assertThat(foundValue).isNotNull();
    }

    @Test
    public void shouldPutByTxObject() {
        Transaction tx = new Transaction(new JsonObject());
        nodeManager.signByNode(tx);
        tm.put(tx);
    }

    @Test
    public void shouldLoadTestObject() {
        assertThat(tm).isNotNull();
    }
}
