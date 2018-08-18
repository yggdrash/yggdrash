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

package io.yggdrash.node;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.husk.BlockHusk;
import io.yggdrash.core.husk.TransactionHusk;
import io.yggdrash.proto.Proto;

import java.util.List;
import java.util.Random;

public class TestUtils {
    public static Wallet wallet;

    private TestUtils() {}

    static {
        try {
            wallet = new Wallet();
        } catch (Exception e) {
            throw new InvalidSignatureException(e);
        }
    }

    public static Proto.Transaction createDummyTx() {
        String body = "dummy";
        return Proto.Transaction.newBuilder()
                .setHeader(Proto.Transaction.Header.newBuilder()
                        .setRawData(Proto.Transaction.Header.Raw.newBuilder()
                                .setType(ByteString.copyFrom(randomBytes(4)))
                                .setVersion(ByteString.copyFrom(randomBytes(4)))
                                .setDataHash(ByteString.copyFrom(randomBytes(32)))
                                .setDataSize(1)
                                .setTimestamp(System.currentTimeMillis())
                        )
                )
                .setBody(body)
                .build();
    }

    public static TransactionHusk createTxHusk() {
        return createTxHusk(wallet);
    }

    public static TransactionHusk createTxHusk(Wallet wallet) {
        return new TransactionHusk(getTransfer()).sign(wallet);
    }

    public static BlockHusk createGenesisBlockHusk() {
        return createGenesisBlockHusk(wallet);
    }

    public static BlockHusk createGenesisBlockHusk(Wallet wallet) {
        return BlockHusk.genesis(wallet, getTransfer());
    }

    public static BlockHusk createBlockHuskByTxList(Wallet wallet, List<TransactionHusk> txList) {
        return BlockHusk.build(wallet, txList, createGenesisBlockHusk());
    }

    public static ObjectMapper getMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    private static byte[] randomBytes(int length) {
        byte[] result = new byte[length];
        new Random().nextBytes(result);
        return result;
    }

    private static JsonObject getTransfer() {
        JsonObject txObj = new JsonObject();

        txObj.addProperty("operator", "transfer");
        txObj.addProperty("to", "0x9843DC167956A0e5e01b3239a0CE2725c0631392");
        txObj.addProperty("amount", 100);

        return txObj;
    }
}
