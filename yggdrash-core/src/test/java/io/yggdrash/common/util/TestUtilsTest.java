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

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.blockchain.Transaction;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestUtilsTest {
    @Test
    public void shouldBeCreatedDifferentKeyTx() {
        Transaction transferTx = BlockChainTestUtils.createTransferTx();
        Transaction transferTx1 = BlockChainTestUtils.createTransferTx();
        Transaction transferTx2 = BlockChainTestUtils.createTransferTx();
        Assertions.assertThat(transferTx.getHash()).isNotEqualTo(transferTx1.getHash());
        Assertions.assertThat(transferTx.getHash()).isNotEqualTo(transferTx2.getHash());
        Assertions.assertThat(transferTx1.getHash()).isNotEqualTo(transferTx2.getHash());
    }
}
