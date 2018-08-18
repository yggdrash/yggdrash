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

package io.yggdrash;

import com.google.protobuf.ByteString;
import io.yggdrash.core.Address;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.Proto;
import io.yggdrash.util.FileUtil;

import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Random;

public class TestUtils {
    public static final String YGG_HOME = "testOutput";

    public static void clearOutput() {
        FileUtil.recursiveDelete(Paths.get(YGG_HOME));
    }

    public static byte[] randomBytes(int length) {
        byte[] result = new byte[length];
        new Random().nextBytes(result);
        return result;
    }

    public static Transaction createDummyTx() {
        TransactionHeader transactionHeader = new TransactionHeader(
                TestUtils.randomBytes(4),
                TestUtils.randomBytes(4),
                TestUtils.randomBytes(32),
                8L,
                8L,
                TestUtils.randomBytes(65));
        return new Transaction(transactionHeader, "dummy");
    }

    public static Address getTestAddress() {
        return new Address(new ECKey().getAddress());
    }

    public static Proto.BlockV2 getBlockFixture() {
        return Proto.BlockV2.newBuilder()
                .setHeader(
                        Proto.BlockV2.Header.newBuilder()
                        .setRawData(Proto.BlockV2.Header.Raw.newBuilder()
                            .setType(ByteString.copyFrom(
                                    ByteBuffer.allocate(4).putInt(1).array()))
                            .setVersion(ByteString.copyFrom(
                                    ByteBuffer.allocate(4).putInt(1).array()))
                            .build()
                        ).build()
                )
                .addBody(getTransactionFixture())
                .addBody(getTransactionFixture())
                .addBody(getTransactionFixture())
                .build();
    }

    public static Proto.TransactionV2 getTransactionFixture() {
        String body = "{\n" +
                "\"func\":\"transfer\",\n"+
                "\"params\":{\n" +
                "\"to\":\"0x407d73d8a49eeb85d32cf465507dd71d507100c1\",\n" +
                "\"value\":\"1000\"}\n" +
                "}";
        return Proto.TransactionV2.newBuilder()
                .setHeader(Proto.TransactionV2.Header.newBuilder()
                        .setRawData(Proto.TransactionV2.Header.Raw.newBuilder()
                                .setType(ByteString.copyFrom(
                                        ByteBuffer.allocate(4).putInt(1).array()))
                                .setVersion(ByteString.copyFrom(
                                        ByteBuffer.allocate(4).putInt(1).array()))
                                .setDataHash(ByteString.copyFrom(
                                        HashUtil.sha3(body.getBytes())))
                                .setDataSize(body.getBytes().length)
                        )
                )
                .setBody(body)
                .build();
    }
}
