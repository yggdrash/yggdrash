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
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.proto.Proto;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class BlockHeader implements ProtoObject<Proto.Block.Header> {

    public static final int VERSION_LENGTH = 8;
    public static final int TYPE_LENGTH = 8;

    static final int LENGTH = 156;

    private final Proto.Block.Header protoHeader;

    private byte[] binaryForSigning;

    public BlockHeader(byte[] bytes) {
        this(toProto(bytes));
    }

    public BlockHeader(Proto.Block.Header protoBlockHeader) {
        this.protoHeader = protoBlockHeader;
    }

    public BlockHeader(
            byte[] chain,
            byte[] version,
            byte[] type,
            byte[] prevBlockHash,
            long index,
            long timestamp,
            byte[] merkleRoot,
            byte[] stateRoot,
            long bodyLength) {

        this.protoHeader = Proto.Block.Header.newBuilder()
                .setChain(ByteString.copyFrom(chain))
                .setVersion(ByteString.copyFrom(version))
                .setType(ByteString.copyFrom(type))
                .setPrevBlockHash(ByteString.copyFrom(prevBlockHash))
                .setIndex(index)
                .setTimestamp(timestamp)
                .setMerkleRoot(ByteString.copyFrom(merkleRoot))
                .setStateRoot(ByteString.copyFrom(stateRoot))
                .setBodyLength(bodyLength)
                .build();
    }

    public BlockHeader(
            byte[] chain,
            byte[] version,
            byte[] type,
            byte[] prevBlockHash,
            long index,
            long timestamp,
            byte[] stateRoot,
            BlockBody blockBody) {
        this(chain, version, type, prevBlockHash, index, timestamp,
                blockBody.getMerkleRoot(), stateRoot, blockBody.getLength());
    }

    public BlockHeader(JsonObject jsonObject) {
        this(Hex.decode(jsonObject.get("chain").getAsString()),
                Hex.decode(jsonObject.get("version").getAsString()),
                Hex.decode(jsonObject.get("type").getAsString()),
                Hex.decode(jsonObject.get("prevBlockHash").getAsString()),
                HexUtil.hexStringToLong(jsonObject.get("index").getAsString()),
                HexUtil.hexStringToLong(jsonObject.get("timestamp").getAsString()),
                Hex.decode(jsonObject.get("merkleRoot").getAsString()),
                Hex.decode(jsonObject.get("stateRoot").getAsString()),
                HexUtil.hexStringToLong(jsonObject.get("bodyLength").getAsString()));
    }

    public byte[] getChain() {
        return protoHeader.getChain().toByteArray();
    }

    public byte[] getVersion() {
        return protoHeader.getVersion().toByteArray();
    }

    public byte[] getType() {
        return protoHeader.getType().toByteArray();
    }

    public byte[] getPrevBlockHash() {
        return protoHeader.getPrevBlockHash().toByteArray();
    }

    public long getIndex() {
        return protoHeader.getIndex();
    }

    public long getTimestamp() {
        return protoHeader.getTimestamp();
    }

    public byte[] getMerkleRoot() {
        return protoHeader.getMerkleRoot().toByteArray();
    }

    public byte[] getStateRoot() {
        return protoHeader.getStateRoot().toByteArray();
    }

    public long getBodyLength() {
        return protoHeader.getBodyLength();
    }

    /**
     * Get the headerHash for signing.
     *
     * @return hash of header
     */
    public byte[] getHashForSigning() {
        return HashUtil.sha3(getBinaryForSigning());
    }

    /**
     * Get the binary data of BlockHeader (120Byte)
     *
     * @return the binary data
     */
    public byte[] getBinaryForSigning() {
        if (binaryForSigning == null) {
            setBinaryForSigning();
        }

        return binaryForSigning;
    }

    private void setBinaryForSigning() {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        try {
            bao.write(getChain());
            bao.write(getVersion());
            bao.write(getType());
            bao.write(getPrevBlockHash());
            bao.write(ByteUtil.longToBytes(getIndex()));
            bao.write(ByteUtil.longToBytes(getTimestamp()));
            bao.write(getMerkleRoot());
            bao.write(getStateRoot());
            bao.write(ByteUtil.longToBytes(getBodyLength()));
        } catch (IOException e) {
            throw new NotValidateException();
        }

        this.binaryForSigning = bao.toByteArray();
    }

    @Override
    public byte[] toBinary() {
        return protoHeader.toByteArray();
    }

    @Override
    public Proto.Block.Header getInstance() {
        return protoHeader;
    }

    /**
     * Convert from BlockHeader to JsonObject.
     *
     * @return jsonObject of block header
     */
    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("chain", Hex.toHexString(getChain()));
        jsonObject.addProperty("version", Hex.toHexString(getVersion()));
        jsonObject.addProperty("type", Hex.toHexString(getType()));
        jsonObject.addProperty("prevBlockHash", Hex.toHexString(getPrevBlockHash()));
        jsonObject.addProperty("index", Hex.toHexString(ByteUtil.longToBytes(getIndex())));
        jsonObject.addProperty("timestamp", Hex.toHexString(ByteUtil.longToBytes(getTimestamp())));
        jsonObject.addProperty("merkleRoot", Hex.toHexString(getMerkleRoot()));
        jsonObject.addProperty("stateRoot", Hex.toHexString(getStateRoot()));
        jsonObject.addProperty("bodyLength", Hex.toHexString(ByteUtil.longToBytes(getBodyLength())));

        return jsonObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BlockHeader other = (BlockHeader) o;
        return Arrays.equals(toBinary(), other.toBinary());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(toBinary());
    }

    @Override
    public String toString() {
        return toJsonObject().toString();
    }

    private static Proto.Block.Header toProto(byte[] bytes) {
        try {
            return Proto.Block.Header.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }
    }
}
