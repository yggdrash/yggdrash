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

import io.yggdrash.proto.BlockChainProto;
import org.junit.Test;

import java.math.BigInteger;

public class TransactionHuskTest {
    private final String privString = "c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4";
    private final BigInteger privateKey = new BigInteger(privString, 16);

    @Test
    public void name() {
        BlockChainProto.TransactionHeader transactionHeader =
                BlockChainProto.TransactionHeader.newBuilder().build();
        BlockChainProto.Transaction transaction =
                BlockChainProto.Transaction.newBuilder()
                        .setHeader(transactionHeader).build();
        TransactionHusk transactionHusk = new TransactionHusk(transaction);
        transactionHusk.sign(privateKey.toByteArray());
    }
}
