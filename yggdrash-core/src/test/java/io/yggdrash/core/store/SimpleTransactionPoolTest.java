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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(classes = StoreConfiguration.class)
public class SimpleTransactionPoolTest {
    @Autowired
    SimpleTransactionPool stp;

    @Test
    public void shouldClearPool() {
        byte[] key = TestUtils.randomBytes(32);
        byte[] value = TestUtils.randomBytes(32);
        stp.put(key, value);
        stp.clear();
        byte[] foundValue = stp.get(key);
        assertThat(foundValue).isNull();
    }

    @Test
    public void shouldGetObject() {
        byte[] key = TestUtils.randomBytes(32);
        byte[] value = TestUtils.randomBytes(32);
        stp.put(key, value);
        byte[] foundValue = stp.get(key);
        assertThat(foundValue).isEqualTo(value);
    }

    @Test
    public void shouldPutObject() {
        byte[] key = TestUtils.randomBytes(32);
        byte[] value = TestUtils.randomBytes(32);
        stp.put(key, value);
    }
}
