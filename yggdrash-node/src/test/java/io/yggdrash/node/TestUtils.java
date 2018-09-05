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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.Proto;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.TimeUtils;

import java.sql.Time;
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
        String body = "[\"dummy\"]";
        return Proto.Transaction.newBuilder()
                .setHeader(Proto.Transaction.Header.newBuilder()
                                .setChain(ByteString.copyFrom(randomBytes(20)))
                                .setVersion(ByteString.copyFrom(randomBytes(8)))
                                .setType(ByteString.copyFrom(randomBytes(8)))
                                .setTimestamp(ByteString.copyFrom(ByteUtil.longToBytes(TimeUtils.time())))
                                .setBodyHash(ByteString.copyFrom(HashUtil.sha3(body.getBytes())))
                                .setBodyLength(ByteString.copyFrom(randomBytes(8)))
                )
                .setSignature(ByteString.copyFrom(wallet.sign(body.getBytes())))
                .setBody(ByteString.copyFrom(body.getBytes()))
                .build();
    }

    public static TransactionHusk createInvalidTxHusk() {
        return new TransactionHusk(createDummyTx());
    }

    public static TransactionHusk createUnsignedTxHusk() {
        return new TransactionHusk(getTransfer());
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

    public static JsonObject getTransfer() {
        JsonArray params = new JsonArray();
        JsonObject param1 = new JsonObject();
        param1.addProperty("address", "aaa2aaab0fb041c5cb2a60a12291cbc3097352bb");
        JsonObject param2 = new JsonObject();
        param2.addProperty("amount", 100);
        params.add(param1);
        params.add(param2);
        JsonObject txObj = new JsonObject();
        txObj.addProperty("method", "transfer");
        txObj.add("params", params);

        return txObj;
    }
}
