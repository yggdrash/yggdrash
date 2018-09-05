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
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.Proto;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.FileUtil;
import io.yggdrash.util.TimeUtils;

import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.security.SignatureException;
import java.util.List;
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

        Proto.Transaction.Header protoHeader = Proto.Transaction.Header.newBuilder()
                .setChain(ByteString.copyFrom(new byte[20]))
                .setVersion(ByteString.copyFrom(new byte[8]))
                .setType(ByteString.copyFrom(new byte[8]))
                .setTimestamp(ByteString.copyFrom(ByteUtil.longToBytes(TimeUtils.time())))
                .setBodyHash(ByteString.copyFrom(HashUtil.sha3(body.getBytes())))
                .setBodyLength(ByteString.copyFrom(ByteUtil.longToBytes(body.length())))
                .build();

        Proto.Transaction protoTransaction = Proto.Transaction.newBuilder()
                .setHeader(protoHeader)
                .setSignature(ByteString.copyFrom(wallet.sign(protoHeader.toByteArray())))
                .setBody(ByteString.copyFrom(body.getBytes()))
                .build();

        return protoTransaction;
    }

    public static Proto.Block getBlockFixture() {
        return getBlockFixture(999L);
    }

    public static Proto.Block getBlockFixture(Long index) {
        return getBlockFixture(index,
                new Sha3Hash("9358888ca1ccd444ad11fb0ea1b5d03483f87664183c6e91ddab1b577cce2c06"));
    }

    public static Proto.Block getBlockFixture(Long index, Sha3Hash prevHash) {

        Proto.Block.Header protoHeader = Proto.Block.Header.newBuilder()
                .setChain(ByteString.copyFrom(new byte[20]))
                .setVersion(ByteString.copyFrom(new byte[8]))
                .setType(ByteString.copyFrom(new byte[8]))
                .setPrevBlockHash(ByteString.copyFrom(prevHash.getBytes()))
                .setIndex(ByteString.copyFrom(ByteUtil.longToBytes(index)))
                .setTimestamp(ByteString.copyFrom(ByteUtil.longToBytes(TimeUtils.time())))
                .setMerkleRoot(ByteString.copyFrom(new byte[32]))
                .setBodyLength(ByteString.copyFrom(ByteUtil.longToBytes(100L)))
                .build();

        Proto.TransactionList txList = Proto.TransactionList.newBuilder().build();
        List<Proto.Transaction> list = txList.getTransactionsList();
        list.add(getTransactionFixture());

        return Proto.Block.newBuilder()
                .setHeader(protoHeader)
                .setSignature(ByteString.copyFrom(wallet.sign(protoHeader.toByteArray())))
                .setBody(txList)
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

    public static JsonObject getTransfer() {
        JsonArray params = new JsonArray();
        JsonObject param1 = new JsonObject();
        param1.addProperty("address", "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76");
        JsonObject param2 = new JsonObject();
        param2.addProperty("amount", 100);
        params.add(param1);
        params.add(param2);
        JsonObject txObj = new JsonObject();
        txObj.addProperty("method", "transfer");
        txObj.add("params", params);

        return txObj;
    }

    public static Proto.Transaction[] getTransactionFixtures() {
        return new Proto.Transaction[] {getTransactionFixture(), getTransactionFixture()};
    }

    public static Proto.Block[] getBlockFixtures() {
        return new Proto.Block[] {getBlockFixture(), getBlockFixture(), getBlockFixture()};
    }
}
