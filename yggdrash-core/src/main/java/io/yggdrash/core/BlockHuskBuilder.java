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

package io.yggdrash.core;

import com.google.protobuf.ByteString;
import io.yggdrash.proto.Proto;
import io.yggdrash.trie.Trie;

import java.nio.ByteBuffer;
import java.util.List;

public class BlockHuskBuilder {

    public static final byte[] EMPTY_BYTE = new byte[32];
    public static final int DEFAULT_TYPE = 1;
    public static final int DEFAULT_VERSION = 1;


//    public static BlockHusk buildSigned(Wallet wallet, List<TransactionHusk> body,
//                                          BlockHusk prevBlock) {
//        return buildUnSigned(wallet, body, prevBlock).sign(wallet);
//    }
//
//    public static BlockHusk buildUnSigned(Wallet wallet, List<TransactionHusk> body,
//                                          BlockHusk prevBlock) {
//
//        byte[] type = ByteBuffer.allocate(4).putInt(DEFAULT_TYPE).array();
//        byte[] version = ByteBuffer.allocate(4).putInt(DEFAULT_VERSION).array();
//
//        Proto.Block.Header.Raw raw = Proto.Block.Header.Raw.newBuilder()
//                .setType(ByteString.copyFrom(type))
//                .setVersion(ByteString.copyFrom(version))
//                .setPrevBlockHash(ByteString.copyFrom(prevBlock.getHash().getBytes()))
//                .setIndex(prevBlock.nextIndex())
//                .setAuthor(ByteString.copyFrom(wallet.getAddress()))
//                .build();
//
//        return buildUnSigned(wallet, raw, body);
//    }
//
//    public static BlockHusk buildUnSigned(Wallet wallet, Proto.Block.Header.Raw raw,
//                                          List<TransactionHusk> body) {
//
//        Proto.Block.Builder builder = Proto.Block.newBuilder();
//
//        long dataSize = 0;
//        for (TransactionHusk tx : body) {
//            builder.addBody(tx.getInstance());
//            dataSize += tx.getData().length;
//        }
//
//        byte[] merkleRoot = Trie.getMerkleRoot(body);
//        if (merkleRoot == null) {
//            merkleRoot = EMPTY_BYTE;
//        }
//
//        Proto.Block.Header.Raw updatedRaw = Proto.Block.Header.Raw.newBuilder(raw)
//                .setDataSize(dataSize)
//                .setMerkleRoot(ByteString.copyFrom(merkleRoot))
//                .setAuthor(ByteString.copyFrom(wallet.getAddress()))
//                .build();
//
//        builder.setHeader(Proto.Block.Header.newBuilder().setRawData(updatedRaw).build());
//        return new BlockHusk(builder.build()).sign(wallet);
//    }
}
