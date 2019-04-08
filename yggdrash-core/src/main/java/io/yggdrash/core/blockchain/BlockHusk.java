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
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.trie.Trie;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Address;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static io.yggdrash.common.config.Constants.EMPTY_BYTE8;

public class BlockHusk implements ProtoHusk<Proto.Block>, Comparable<BlockHusk> {
    private static final Logger log = LoggerFactory.getLogger(TransactionHusk.class);

    private Proto.Block protoBlock;
    private transient Block coreBlock;
    private List<TransactionHusk> body;

    public BlockHusk(byte[] bytes) {
        try {
            this.protoBlock = Proto.Block.parseFrom(bytes);
            this.coreBlock = Block.toBlock(this.protoBlock);
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    public BlockHusk(Proto.Block block) {
        this.protoBlock = block;
        try {
            this.coreBlock = Block.toBlock(this.protoBlock);
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    public BlockHusk(Block block) {
        this.coreBlock = block;
        this.protoBlock = Block.toProtoBlock(this.coreBlock);
        this.body = new ArrayList<>();
        for (Transaction tx : block.getBody().getBody()) {
            this.body.add(new TransactionHusk(tx));
        }
    }

    public BlockHusk(Wallet wallet, List<TransactionHusk> body, BlockHusk prevBlock) {
        this.body = body;
        if (wallet == null || body == null || prevBlock == null) {
            throw new NotValidateException();
        }

        byte[] merkleRoot = Trie.getMerkleRootHusk(body);

        long length = 0;

        Proto.TransactionList.Builder txBuilder = Proto.TransactionList.newBuilder();
        for (TransactionHusk txHusk: body) {
            length += txHusk.getCoreTransaction().length();
            txBuilder.addTransactions(txHusk.getProtoTransaction());
        }

        Proto.Block.Header blockHeader = getHeader(
                prevBlock.getHeader().getChain().toByteArray(),
                EMPTY_BYTE8,
                EMPTY_BYTE8,
                prevBlock.getHash().getBytes(),
                prevBlock.getIndex() + 1,
                TimeUtils.time(),
                merkleRoot,
                length);

        try {
            byte[] hashDataForSign = BlockHeader.toBlockHeader(blockHeader).getHashForSigning();

            this.protoBlock = Proto.Block.newBuilder()
                    .setHeader(blockHeader)
                    .setSignature(ByteString.copyFrom(wallet.sign(hashDataForSign, true)))
                    .setBody(txBuilder.build())
                    .build();

            this.coreBlock = Block.toBlock(this.protoBlock);

        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    public Block getCoreBlock() {
        return coreBlock;
    }

    public Sha3Hash getHash() {
        return new Sha3Hash(this.coreBlock.getHash(), true);
    }

    public Address getAddress() {
        try {
            return new Address(this.coreBlock.getAddress());
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    public byte[] getPublicKey() {
        return this.coreBlock.getPubKey();
    }

    public BranchId getBranchId() {
        byte[] chain = protoBlock.getHeader().getChain().toByteArray();
        return new BranchId(Sha3Hash.createByHashed(chain));
    }

    public Sha3Hash getPrevHash() {
        return new Sha3Hash(this.coreBlock.getPrevBlockHash(), true);
    }

    public long getIndex() {
        return this.protoBlock.getHeader().getIndex();
    }

    public List<TransactionHusk> getBody() {
        if (body != null) {
            return body;
        }
        this.body = new ArrayList<>();
        for (Proto.Transaction tx : protoBlock.getBody().getTransactionsList()) {
            body.add(new TransactionHusk(tx));
        }
        return body;
    }

    public int getBodyCount() {
        return protoBlock.getBody().getTransactionsCount();
    }

    public long getBodyLength() {
        return coreBlock.getHeader().getBodyLength();
    }

    @Override
    public byte[] getData() {
        return protoBlock.toByteArray();
    }

    @Override
    public Proto.Block getInstance() {
        return this.protoBlock;
    }

    public boolean verify() {
        return this.coreBlock.verify();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BlockHusk other = (BlockHusk) o;
        return Arrays.equals(getHash().getBytes(), other.getHash().getBytes());
    }

    @Override
    public int hashCode() {
        return Objects.hash(protoBlock);
    }

    /**
     * Convert from BlockHusk.class to JSON string.
     * @return block as JsonObject
     */
    public JsonObject toJsonObject() {
        return this.coreBlock.toJsonObject();
    }

    JsonObject toJsonObjectByProto() {
        try {
            String print = JsonFormat.printer()
                    .includingDefaultValueFields().print(this.protoBlock);
            JsonObject jsonObject = new JsonParser().parse(print).getAsJsonObject();
            jsonObject.addProperty("blockId", getHash().toString());
            return jsonObject;
        } catch (InvalidProtocolBufferException e) {
            log.warn(e.getMessage());
        }
        return null;
    }

    private Proto.Block.Header getHeader() {
        return this.protoBlock.getHeader();
    }

    private static Proto.Block.Header getHeader(
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

    @Override
    public int compareTo(BlockHusk o) {
        return Long.compare(getIndex(), o.getIndex());
    }
}
