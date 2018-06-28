/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.net;

import com.google.protobuf.ByteString;
import io.yggdrash.proto.BlockChainProto;

public final class NodeTestData {

    private NodeTestData() {
    }

    public static BlockChainProto.Transaction[] transactions() {
        return new BlockChainProto.Transaction[] {
                BlockChainProto.Transaction.newBuilder().setData("tx1").build(),
                BlockChainProto.Transaction.newBuilder().setData("tx2").build(),
                BlockChainProto.Transaction.newBuilder().setData("tx3").build()
        };
    }

    public static BlockChainProto.Block[] blocks() {
        BlockChainProto.Transaction tx
                = BlockChainProto.Transaction.newBuilder().setData("tx").build();
        return new BlockChainProto.Block[] {
                BlockChainProto.Block.newBuilder()
                        .setHeader(BlockChainProto.BlockHeader.newBuilder().setAuthor(
                                ByteString.copyFromUtf8("author1")))
                        .setData(BlockChainProto.BlockBody.newBuilder().addTrasactions(tx)).build(),
                BlockChainProto.Block.newBuilder()
                        .setHeader(BlockChainProto.BlockHeader.newBuilder().setAuthor(
                                ByteString.copyFromUtf8("author2")))
                        .setData(BlockChainProto.BlockBody.newBuilder().addTrasactions(tx)).build(),
                BlockChainProto.Block.newBuilder()
                        .setHeader(BlockChainProto.BlockHeader.newBuilder().setAuthor(
                                ByteString.copyFromUtf8("author3")))
                        .setData(BlockChainProto.BlockBody.newBuilder().addTrasactions(tx)).build()
        };
    }
}
