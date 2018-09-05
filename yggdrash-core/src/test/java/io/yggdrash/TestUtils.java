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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BlockHuskBuilder;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.Proto;
import io.yggdrash.util.TimeUtils;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Random;

public class TestUtils {
    public static final String YGG_HOME = "testOutput";

    private static Wallet wallet;
    private static byte[] type =
            ByteBuffer.allocate(4).putInt(BlockHuskBuilder.DEFAULT_TYPE).array();
    private static byte[] version =
            ByteBuffer.allocate(4).putInt(BlockHuskBuilder.DEFAULT_VERSION).array();

    private TestUtils() {}

    static {
        try {
            wallet = new Wallet();
        } catch (Exception e) {
            throw new InvalidSignatureException(e);
        }
    }

    public static Proto.Transaction getTransactionFixture() {
        String body = getTransfer().toString();
        return Proto.Transaction.newBuilder()
                .setHeader(Proto.Transaction.Header.newBuilder()
                        .setRawData(Proto.Transaction.Header.Raw.newBuilder()
                                .setType(ByteString.copyFrom(
                                        ByteBuffer.allocate(4).putInt(1).array()))
                                .setVersion(ByteString.copyFrom(
                                        ByteBuffer.allocate(4).putInt(1).array()))
                                .setDataHash(ByteString.copyFrom(
                                        HashUtil.sha3(body.getBytes())))
                                .setDataSize(body.getBytes().length)
                                .setTimestamp(TimeUtils.time())
                        )
                )
                .setBody(body)
                .build();
    }

    public static Proto.Transaction[] getTransactionFixtures() {
        return new Proto.Transaction[] {getTransactionFixture(), getTransactionFixture()};
    }

    public static TransactionHusk createTxHusk() {
        return createTxHusk(wallet);
    }

    public static TransactionHusk createTxHusk(Wallet wallet) {
        return createTxHuskByJson(getTransfer()).sign(wallet);
    }

    public static TransactionHusk createTxHuskByJson(JsonObject jsonObject) {
        String body = jsonObject.toString();
        Proto.Transaction.Header transactionHeader = Proto.Transaction.Header.newBuilder()
                .setRawData(Proto.Transaction.Header.Raw.newBuilder()
                        .setType(ByteString.copyFrom(type))
                        .setVersion(ByteString.copyFrom(version))
                        .setDataHash(ByteString.copyFrom(HashUtil.sha3(body.getBytes())))
                        .setDataSize(body.getBytes().length)
                        .build())
                .build();
        Proto.Transaction tx = Proto.Transaction.newBuilder()
                .setHeader(transactionHeader)
                .setBody(body)
                .build();
        return new TransactionHusk(tx);
    }

    public static Proto.Block getBlockFixture() {
        return getBlockFixture(999L);
    }

    public static Proto.Block getBlockFixture(Long index) {
        return getBlockFixture(index,
                new Sha3Hash("9358888ca1ccd444ad11fb0ea1b5d03483f87664183c6e91ddab1b577cce2c06"));
    }

    public static Proto.Block getBlockFixture(Long index, Sha3Hash prevHash) {
        return Proto.Block.newBuilder()
                .setHeader(
                        Proto.Block.Header.newBuilder()
                                .setRawData(Proto.Block.Header.Raw.newBuilder()
                                        .setType(ByteString.copyFrom(
                                                ByteBuffer.allocate(4).putInt(1).array()))
                                        .setVersion(ByteString.copyFrom(
                                                ByteBuffer.allocate(4).putInt(1).array()))
                                        .setIndex(index)
                                        .setPrevBlockHash(ByteString.copyFrom(
                                                prevHash.getBytes()
                                        ))
                                        .build()
                                ).build()
                )
                .addBody(getTransactionFixture())
                .addBody(getTransactionFixture())
                .addBody(getTransactionFixture())
                .build();
    }

    public static Proto.Block[] getBlockFixtures() {
        return new Proto.Block[] {getBlockFixture(), getBlockFixture(), getBlockFixture()};
    }

    public static BlockHusk createGenesisBlockHusk() {
        return createGenesisBlockHusk(wallet);
    }

    public static BlockHusk createGenesisBlockHusk(Wallet wallet) {
        return genesis(wallet, getTransfer());
    }

    private static BlockHusk genesis(Wallet wallet, JsonObject jsonObject) {
        TransactionHusk tx = createTxHuskByJson(jsonObject).sign(wallet);

        Proto.Block.Header.Raw raw = crateRaw(wallet.getAddress(),0, BlockHuskBuilder.EMPTY_BYTE);
        return BlockHuskBuilder.buildUnSigned(wallet, raw, Collections.singletonList(tx));
    }

    private static Proto.Block.Header.Raw crateRaw(byte[] address, long index,
                                                   byte[] prevBlockHash) {

        return Proto.Block.Header.Raw.newBuilder()
                .setType(ByteString.copyFrom(type))
                .setVersion(ByteString.copyFrom(version))
                .setPrevBlockHash(ByteString.copyFrom(prevBlockHash))
                .setIndex(index)
                .setAuthor(ByteString.copyFrom(address))
                .build();
    }

    public static byte[] randomBytes(int length) {
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
