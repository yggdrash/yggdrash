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

package io.yggdrash.core.husk;

import io.yggdrash.TestUtils;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;

public class TransactionHuskTest {

    @Test
    public void shouldBeVerifiedBySignature()
            throws IOException, InvalidCipherTextException {
        TransactionHusk transactionHusk = getTransactionHusk();
        Wallet wallet = new Wallet();

        transactionHusk.sign(wallet);
        Assertions.assertThat(transactionHusk.verify()).isTrue();
    }

    @Test
    public void shouldBeSignedTransaction() throws IOException, InvalidCipherTextException {
        TransactionHusk transactionHusk = getTransactionHusk();

        Wallet wallet = new Wallet();
        transactionHusk.sign(wallet);

        Assertions.assertThat(transactionHusk.isSigned()).isTrue();
        Assertions.assertThat(transactionHusk.verify());
    }

    @Test
    public void shouldBeCreatedNonSingedTransaction() {
        /* 외부에서 받는 정보
           - target - 블록체인 ID - String
           - from - 보내는 주소 - String
           - body - JSON - String
         */
        TransactionHusk transactionHusk = getTransactionHusk();
        Assertions.assertThat(transactionHusk).isNotNull();
    }

    private TransactionHusk getTransactionHusk() {
        return new TransactionHusk(TestUtils.sampleTxObject(null));
    }

}
