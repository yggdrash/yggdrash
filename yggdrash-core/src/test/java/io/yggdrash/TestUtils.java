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
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.Proto;
import io.yggdrash.util.FileUtil;
import io.yggdrash.util.TimeUtils;

import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

public class TestUtils {
    private static Wallet wallet;
    public static final String YGG_HOME = "testOutput";
    private static byte[] type =
            ByteBuffer.allocate(4).putInt(BlockHuskBuilder.DEFAULT_TYPE).array();
    private static byte[] version =
            ByteBuffer.allocate(4).putInt(BlockHuskBuilder.DEFAULT_VERSION).array();

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

    public static JsonObject getSampleBranch1() {
        String name = "TEST1";
        String symbol = "TEST1";
        String property = "dex";
        String type = "immunity";
        String description = "TEST1";
        String version = "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76";
        String referenceAddress = "";
        String reserveAddress = "0x2G5f8A319550f80f9D362ab2eE0D1f023EC665a3";
        return createBranch(name, symbol, property, type, description,
                version, referenceAddress, reserveAddress);
    }

    public static JsonObject getSampleBranch2() {
        String name = "TEST2";
        String symbol = "TEST2";
        String property = "exchange";
        String type = "mutable";
        String description = "TEST2";
        String version = "0xe4452ervbo091qw4f5n2s8799232abr213er2c90";
        String referenceAddress = "";
        String reserveAddress = "0x2G5f8A319550f80f9D362ab2eE0D1f023EC665a3";
        return createBranch(name, symbol, property, type, description,
                version, referenceAddress, reserveAddress);
    }

    public static JsonObject getSampleBranch3(String branchId) {
        String name = "Ethereum TO YEED";
        String symbol = "ETH TO YEED";
        String property = "exchange";
        String type = "immunity";
        String description = "ETH TO YEED";
        String version = "0xb5790adeafbb9ac6c9be60955484ab1547ab0b76";
        String referenceAddress = branchId;
        String reserveAddress = "0x1F8f8A219550f89f9D372ab2eE0D1f023EC665a3";
        return createBranch(name, symbol, property, type, description,
                version, referenceAddress, reserveAddress);
    }

    private static JsonObject createBranch(String name,
                                           String symbol,
                                           String property,
                                           String type,
                                           String description,
                                           String version,
                                           String referenceAddress,
                                           String reserveAddress) {
        JsonArray versionHistory = new JsonArray();
        versionHistory.add(version);
        JsonObject branch = new JsonObject();
        branch.addProperty("name", name);
        //branch.addProperty("owner", wallet.getHexAddress());
        branch.addProperty("owner", "9e187f5264037ab77c87fcffcecd943702cd72c3");
        branch.addProperty("symbol", symbol);
        branch.addProperty("property", property);
        branch.addProperty("type", type);
        branch.addProperty("timestamp", "0000016531dfa31c");
        branch.addProperty("description", description);
        branch.addProperty("tag", 0.1);
        branch.addProperty("version", version);
        branch.add("versionHistory", versionHistory);
        branch.addProperty("reference_address", referenceAddress);
        branch.addProperty("reserve_address", reserveAddress);

        return branch;
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
}
