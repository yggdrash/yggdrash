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

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.Proto;
import io.yggdrash.util.TimeUtils;
import io.yggdrash.util.FileUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.nio.file.Paths;
import java.util.Random;

public class TestUtils {
    private static Wallet wallet;
    public static final String YGG_HOME = "testOutput";
    private TestUtils() {}

    static {
        try {
            wallet = new Wallet();
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    public static void clearOutput() {
        FileUtil.recursiveDelete(Paths.get(YGG_HOME));
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

    public static byte[] randomBytes(int length) {
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

    public static Proto.Transaction[] getTransactionFixtures() {
        return new Proto.Transaction[] {getTransactionFixture(), getTransactionFixture()};
    }

    public static Proto.Block[] getBlockFixtures() {
        return new Proto.Block[] {getBlockFixture(), getBlockFixture(), getBlockFixture()};
    }
}
