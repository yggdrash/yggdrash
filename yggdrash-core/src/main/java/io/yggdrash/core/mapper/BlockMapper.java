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

package io.yggdrash.core.mapper;

import com.google.protobuf.ByteString;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.Transaction;
import io.yggdrash.proto.BlockChainProto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Mapper for the block and proto block.
 */
public class BlockMapper {

    /**
     * Proto block to block.
     *
     * @param protoBlock the proto block
     * @return the block
     */
    public static Block protoBlockToBlock(BlockChainProto.Block protoBlock) throws IOException {
        BlockBody data = protoBodyToBody(protoBlock.getData());
        BlockHeader header = protoHeaderToHeader(protoBlock.getHeader(), data);
        return new Block(header, data);
    }

    /**
     * Block to proto block.
     *
     * @param block the block
     * @return the proto block
     */
    public static BlockChainProto.Block blockToProtoBlock(Block block) {
        BlockHeader header = block.getHeader();
        BlockBody data = block.getData();

        BlockChainProto.BlockBody.Builder bodyBuilder = BlockChainProto.BlockBody.newBuilder();
        for (Transaction tx : data.getTransactionList()) {
            bodyBuilder.addTrasactions(TransactionMapper.transactionToProtoTransaction(tx));
        }

        return BlockChainProto.Block.newBuilder()
                .setHeader(headerToProtoHeader(header)).setData(bodyBuilder).build();

    }

    private static BlockHeader protoHeaderToHeader(BlockChainProto.BlockHeader protoHeader,
                                                   BlockBody body) throws IOException {
        BlockHeader.Builder builder = new BlockHeader.Builder();
        builder.blockBody(body);
        return builder.build(protoHeader.getIndex(), protoHeader.getPrevBlockHash().toByteArray(),
                protoHeader.getTimestamp(), protoHeader.getSignature().toByteArray());
    }

    private static BlockChainProto.BlockHeader headerToProtoHeader(BlockHeader header) {
        BlockChainProto.BlockHeader.Builder builder = BlockChainProto.BlockHeader.newBuilder()
                .setType(ByteString.copyFrom(header.getType()))
                .setVersion(ByteString.copyFrom(header.getVersion()))
                .setIndex(header.getIndex())
                .setTimestamp(header.getTimestamp())
                .setDataSize(header.getDataSize());

        if (header.getPrevBlockHash() != null) {
            builder.setPrevBlockHash(ByteString.copyFrom(header.getPrevBlockHash()));
        }
        if (header.getMerkleRoot() != null) {
            builder.setMerkleRoot(ByteString.copyFrom(header.getMerkleRoot()));
        }
        if (header.getSignature() != null) {
            builder.setSignature(ByteString.copyFrom(header.getSignature()));
        }
        return builder.build();
    }

    private static BlockBody protoBodyToBody(BlockChainProto.BlockBody data) throws IOException {
        List<Transaction> transactionList = new ArrayList<>();
        for (BlockChainProto.Transaction tx : data.getTrasactionsList()) {
            transactionList.add(TransactionMapper.protoTransactionToTransaction(tx));
        }
        return new BlockBody(transactionList);
    }
}
