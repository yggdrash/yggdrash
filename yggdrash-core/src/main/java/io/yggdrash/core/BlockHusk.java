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

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.proto.Proto;
import io.yggdrash.trie.Trie;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.TimeUtils;

import java.nio.ByteBuffer;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BlockHusk implements ProtoHusk<Proto.Block>, Comparable<BlockHusk> {
    private static final byte[] EMPTY_BYTE = new byte[32];

    private Proto.Block block;

    public BlockHusk(byte[] bytes) throws InvalidProtocolBufferException {
        this.block = Proto.Block.parseFrom(bytes);
    }

    public BlockHusk(Proto.Block block) {
        this.block = block;
    }

    public BlockHusk sign(Wallet wallet) {
        Proto.Block.Header.Raw updatedRawData = Proto.Block.Header.Raw
                .newBuilder(getHeader().getRawData())
                .setTimestamp(TimeUtils.time()).build();
        byte[] signature = wallet.sign(updatedRawData.toByteArray());
        this.block = Proto.Block.newBuilder(block)
                .setHeader(
                        Proto.Block.Header.newBuilder()
                                .setRawData(updatedRawData)
                                .setSignature(ByteString.copyFrom(signature))
                                .build())
                .build();
        return this;
    }

    public Sha3Hash getHash() {
        return new Sha3Hash(block.getHeader().toByteArray());
    }

    public Address getAddress() {
        return new Address(ecKey().getAddress());
    }

    public Sha3Hash getPrevHash() {
        return Sha3Hash.createByHashed(getHeader().getRawData().getPrevBlockHash().toByteArray());
    }

    public long getIndex() {
        return this.block.getHeader().getRawData().getIndex();
    }

    public long nextIndex() {
        return getIndex() + 1;
    }

    public List<TransactionHusk> getBody() {
        List<TransactionHusk> result = new ArrayList<>();
        for (Proto.Transaction tx : block.getBodyList()) {
            result.add(new TransactionHusk(tx));
        }
        return result;
    }

    @Override
    public byte[] getData() {
        return block.toByteArray();
    }

    @Override
    public Proto.Block getInstance() {
        return this.block;
    }

    /**
     * Get ECKey(include pubKey) using sig & signData.
     *
     * @return ECKey(include pubKey)
     */
    private ECKey ecKey() {
        try {
            byte[] hashedRawData = new Sha3Hash(getHeader().getRawData().toByteArray()).getBytes();
            byte[] signatureBin = getHeader().getSignature().toByteArray();
            return ECKey.signatureToKey(hashedRawData, signatureBin);
        } catch (SignatureException e) {
            throw new InvalidSignatureException(e);
        }
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
        return Objects.hash(block);
    }

    /**
     * Convert from BlockHusk.class to JSON string.
     * @return block as JsonObject
     */
    public JsonObject toJsonObject() {
        //todo: change to serialize method

        JsonObject jsonObject = new JsonObject();
        Proto.Block.Header.Raw raw = this.block.getHeader().getRawData();
        jsonObject.addProperty("type",
                org.spongycastle.util.encoders.Hex.toHexString(raw.getType().toByteArray()));
        jsonObject.addProperty("version",
                org.spongycastle.util.encoders.Hex.toHexString(raw.getVersion().toByteArray()));
        jsonObject.addProperty("prevBlockHash",
                org.spongycastle.util.encoders.Hex.toHexString(
                        raw.getPrevBlockHash().toByteArray()));
        jsonObject.addProperty("merkleRoot",
                org.spongycastle.util.encoders.Hex.toHexString(raw.getMerkleRoot().toByteArray()));
        jsonObject.addProperty("timestamp",
                org.spongycastle.util.encoders.Hex.toHexString(
                    ByteUtil.longToBytes(raw.getTimestamp())));
        jsonObject.addProperty("dataSize",
                org.spongycastle.util.encoders.Hex.toHexString(
                        ByteUtil.longToBytes(raw.getDataSize())));
        jsonObject.addProperty("signature",
                org.spongycastle.util.encoders.Hex.toHexString(
                        this.block.getHeader().getSignature().toByteArray()));

        JsonArray jsonArray = new JsonArray();

        for (TransactionHusk tx : this.getBody()) {
            jsonArray.add(tx.toJsonObject());
        }

        jsonObject.add("data", jsonArray);

        return jsonObject;
    }

    public static BlockHusk build(Wallet wallet, Proto.Block.Header blockHeader,
                                  List<TransactionHusk> body) {
        Proto.Block.Builder builder = Proto.Block.newBuilder()
                .setHeader(blockHeader);
        for (TransactionHusk tx : body) {
            builder.addBody(tx.getInstance());
        }

        return new BlockHusk(builder.build()).sign(wallet);
    }

    public static BlockHusk build(Wallet wallet, List<TransactionHusk> body, BlockHusk prevBlock) {
        byte[] merkleRoot = Trie.getMerkleRoot(body);
        if (merkleRoot == null) {
            merkleRoot = EMPTY_BYTE;
        }
        Proto.Block.Header blockHeader = getHeader(wallet.getAddress(), merkleRoot,
                prevBlock.nextIndex(), prevBlock.getHash().getBytes(), getBodySize(body));
        return build(wallet, blockHeader, body);
    }

    @VisibleForTesting
    public static BlockHusk genesis(Wallet wallet, JsonObject jsonObject) {

        TransactionHusk tx = new TransactionHusk(jsonObject).sign(wallet);
        long dataSize = jsonObject.toString().getBytes().length;
        Proto.Block.Header blockHeader = getHeader(wallet.getAddress(),
                    Trie.getMerkleRoot(Collections.singletonList(tx)), 0, EMPTY_BYTE, dataSize);
        return build(wallet, blockHeader, Collections.singletonList(tx));
    }

    private Proto.Block.Header getHeader() {
        return this.block.getHeader();
    }

    private static Proto.Block.Header getHeader(byte[] address, byte[] merkleRoot, long index,
                                         byte[] prevBlockHash, long dataSize) {

        return Proto.Block.Header.newBuilder()
                .setRawData(Proto.Block.Header.Raw.newBuilder()
                        .setType(ByteString.copyFrom(ByteBuffer.allocate(4).putInt(1).array()))
                        .setVersion(ByteString.copyFrom(ByteBuffer.allocate(4).putInt(1).array()))
                        .setPrevBlockHash(ByteString.copyFrom(prevBlockHash))
                        .setMerkleRoot(ByteString.copyFrom(merkleRoot))
                        .setIndex(index)
                        .setAuthor(ByteString.copyFrom(address))
                        .setDataSize(dataSize)
                        .build())
                .build();
    }

    private static long getBodySize(List<TransactionHusk> body) {
        long size = 0;
        if (body == null || body.isEmpty()) {
            return size;
        }
        for (TransactionHusk tx : body) {
            if (tx.getInstance() != null) {
                size += tx.getInstance().toByteArray().length;
            }
        }
        return size;
    }

    @Override
    public int compareTo(BlockHusk o) {
        return Long.compare(o.getIndex(), getIndex());
    }
}
