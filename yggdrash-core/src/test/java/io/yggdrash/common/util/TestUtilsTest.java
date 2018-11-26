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

package io.yggdrash.common.util;

import io.yggdrash.TestUtils;
import io.yggdrash.core.blockchain.TransactionHusk;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestUtilsTest {
    @Test
    public void shouldBeCreatedDifferentKeyTx() {
        TransactionHusk transferTxHusk = TestUtils.createTransferTxHusk();
        TransactionHusk transferTxHusk1 = TestUtils.createTransferTxHusk();
        TransactionHusk transferTxHusk2 = TestUtils.createTransferTxHusk();
        Assertions.assertThat(transferTxHusk.getHash()).isNotEqualTo(transferTxHusk1.getHash());
        Assertions.assertThat(transferTxHusk.getHash()).isNotEqualTo(transferTxHusk2.getHash());
        Assertions.assertThat(transferTxHusk1.getHash()).isNotEqualTo(transferTxHusk2.getHash());
    }
}
