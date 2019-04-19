
/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.blockchain;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.common.trie.Trie;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.consensus.AbstractBlock;
import io.yggdrash.core.consensus.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.Proto;

import java.util.List;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static io.yggdrash.common.config.Constants.EMPTY_BYTE8;

@Deprecated
public class BlockHusk extends AbstractBlock<Proto.Block> implements Comparable<BlockHusk> {

    public BlockHusk(byte[] bytes) {
        super(toProto(bytes));
    }

    public BlockHusk(io.yggdrash.core.blockchain.Block block) {
        super(io.yggdrash.core.blockchain.Block.toProtoBlock(block));
    }

    @Override
    public Object getConsensusMessages() {
        throw new FailedOperationException("Not implemented");
    }

    @Override
    public Proto.Block getInstance() {
        return getProtoBlock();
    }

    @Override
    public byte[] getData() {
        return getInstance().toByteArray();
    }

    @Override
    public JsonObject toJsonObject() {
        return getBlock().toJsonObject();
    }

    @Override
    public void clear() {
        throw new FailedOperationException("Not implemented");
    }

    @Override
    public int compareTo(BlockHusk o) {
        return Long.compare(getIndex(), o.getIndex());
    }

    private static Proto.Block toProto(byte[] bytes) {
        try {
            return Proto.Block.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }
    }

    public static BlockHusk nextBlock(Wallet wallet, List<TransactionHusk> body, Block prevBlock) {
        if (body == null || prevBlock == null) {
            throw new NotValidateException();
        }

        byte[] merkleRoot = Trie.getMerkleRootHusk(body);

        long length = 0;

        Proto.TransactionList.Builder txBuilder = Proto.TransactionList.newBuilder();
        for (TransactionHusk txHusk: body) {
            length += txHusk.getCoreTransaction().length();
            txBuilder.addTransactions(txHusk.getProtoTransaction());
        }

        Proto.Block.Header blockHeader = BlockHusk.getHeader(
                prevBlock.getBranchId().getBytes(),
                EMPTY_BYTE8,
                EMPTY_BYTE8,
                prevBlock.getHash().getBytes(),
                prevBlock.getIndex() + 1,
                TimeUtils.time(),
                merkleRoot,
                length);

        byte[] hashDataForSign = BlockHeader.toBlockHeader(blockHeader).getHashForSigning();

        Proto.Block protoBlock = Proto.Block.newBuilder()
                .setHeader(blockHeader)
                .setSignature(ByteString.copyFrom(wallet.sign(hashDataForSign, true)))
                .setBody(txBuilder.build())
                .build();
        return new BlockHusk(protoBlock.toByteArray());
    }

    public static Proto.Block.Header getHeader(
            byte[] chain,
            byte[] version,
            byte[] type,
            byte[] prevBlockHash,
            long index,
            long timestamp,
            byte[] merkleRoot,
            long bodyLength) {

        return Proto.Block.Header.newBuilder()
                .setChain(ByteString.copyFrom(chain))
                .setVersion(ByteString.copyFrom(version))
                .setType(ByteString.copyFrom(type))
                .setPrevBlockHash(ByteString.copyFrom(prevBlockHash))
                .setIndex(index)
                .setTimestamp(fromMillis(timestamp))
                .setMerkleRoot(ByteString.copyFrom(merkleRoot))
                .setBodyLength(bodyLength)
                .build();
    }

}