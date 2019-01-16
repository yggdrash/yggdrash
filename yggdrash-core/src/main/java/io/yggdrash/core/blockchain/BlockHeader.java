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
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.core.exception.InternalErrorException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.proto.Proto;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;


public class BlockHeader implements Cloneable {

    static final int CHAIN_LENGTH = 20;
    static final int VERSION_LENGTH = 8;
    static final int TYPE_LENGTH = 8;
    static final int PREVBLOCKHASH_LENGTH = 32;
    static final int INDEX_LENGTH = 8;
    static final int TIMESTAMP_LENGTH = 8;
    static final int MERKLEROOT_LENGTH = 32;
    static final int BODYLENGTH_LENGTH = 8;

    // Data format v0.0.3
    private final byte[] chain;           // 20 Bytes
    private final byte[] version;         // 8 Bytes
    private final byte[] type;            // 8 Bytes
    private final byte[] prevBlockHash;   // 32 Bytes
    private final long index;             // 8 Bytes
    private final long timestamp;         // 8 Bytes
    private final byte[] merkleRoot;      // 32 Bytes
    private final long bodyLength;        // 8 Bytes

    public BlockHeader(
            byte[] chain,
            byte[] version,
            byte[] type,
            byte[] prevBlockHash,
            long index,
            long timestamp,
            byte[] merkleRoot,
            long bodyLength) {
        this.chain = chain;
        this.version = version;
        this.type = type;
        this.prevBlockHash = prevBlockHash;
        this.index = index;
        this.timestamp = timestamp;
        this.merkleRoot = merkleRoot;
        this.bodyLength = bodyLength;
    }

    public BlockHeader(
            byte[] chain,
            byte[] version,
            byte[] type,
            byte[] prevBlockHash,
            long index,
            long timestamp,
            BlockBody blockBody) throws IOException {
        this(chain, version, type, prevBlockHash, index, timestamp,
                blockBody.getMerkleRoot(), blockBody.length());
    }

    public BlockHeader(JsonObject jsonObject) {
        this.chain = Hex.decode(jsonObject.get("chain").getAsString());
        this.version = Hex.decode(jsonObject.get("version").getAsString());
        this.type = Hex.decode(jsonObject.get("type").getAsString());
        this.prevBlockHash = Hex.decode(jsonObject.get("prevBlockHash").getAsString());
        this.index = HexUtil.hexStringToLong(jsonObject.get("index").getAsString());
        this.timestamp = HexUtil.hexStringToLong(jsonObject.get("timestamp").getAsString());
        this.merkleRoot = Hex.decode(jsonObject.get("merkleRoot").getAsString());
        this.bodyLength = HexUtil.hexStringToLong(jsonObject.get("bodyLength").getAsString());
    }

    public BlockHeader(byte[] blockHeaderBytes) {
        int pos = 0;

        this.chain = new byte[CHAIN_LENGTH];
        System.arraycopy(blockHeaderBytes, pos, this.chain, 0, this.chain.length);
        pos += this.chain.length;

        this.version = new byte[VERSION_LENGTH];
        System.arraycopy(blockHeaderBytes, pos, this.version, 0, this.version.length);
        pos += this.version.length;

        this.type = new byte[TYPE_LENGTH];
        System.arraycopy(blockHeaderBytes, pos, this.type, 0, this.type.length);
        pos += this.type.length;

        this.prevBlockHash = new byte[PREVBLOCKHASH_LENGTH];
        System.arraycopy(blockHeaderBytes, pos, this.prevBlockHash, 0, this.prevBlockHash.length);
        pos += this.prevBlockHash.length;

        byte[] indexBytes = new byte[INDEX_LENGTH];
        System.arraycopy(blockHeaderBytes, pos, indexBytes, 0, indexBytes.length);
        pos += indexBytes.length;
        this.index = ByteUtil.byteArrayToLong(indexBytes);

        byte[] timestampBytes = new byte[TIMESTAMP_LENGTH];
        System.arraycopy(blockHeaderBytes, pos, timestampBytes, 0, timestampBytes.length);
        pos += timestampBytes.length;
        this.timestamp = ByteUtil.byteArrayToLong(timestampBytes);

        this.merkleRoot = new byte[MERKLEROOT_LENGTH];
        System.arraycopy(blockHeaderBytes, pos, this.merkleRoot, 0, this.merkleRoot.length);
        pos += this.merkleRoot.length;

        byte[] bodyLengthBytes = new byte[BODYLENGTH_LENGTH];
        System.arraycopy(blockHeaderBytes, pos, bodyLengthBytes, 0, bodyLengthBytes.length);
        pos += bodyLengthBytes.length;
        this.bodyLength = ByteUtil.byteArrayToLong(bodyLengthBytes);

        if (pos != blockHeaderBytes.length) {
            throw new NotValidateException();
        }
    }

    public byte[] getChain() {
        return chain;
    }

    public byte[] getVersion() {
        return version;
    }

    public byte[] getType() {
        return type;
    }

    public byte[] getPrevBlockHash() {
        return prevBlockHash;
    }

    public long getIndex() {
        return index;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getMerkleRoot() {
        return merkleRoot;
    }

    public long getBodyLength() {
        return bodyLength;
    }

    public byte[] toBinary() {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        try {
            bao.write(chain);
            bao.write(version);
            bao.write(type);
            bao.write(prevBlockHash);
            bao.write(ByteUtil.longToBytes(index));
            bao.write(ByteUtil.longToBytes(timestamp));
            bao.write(merkleRoot);
            bao.write(ByteUtil.longToBytes(bodyLength));

            return bao.toByteArray();
        } catch (IOException e) {
            throw new InternalErrorException("toBinary error");
        }
    }

    public long length() {
        return this.toBinary().length;
    }

    public byte[] getHashForSigning() {
        return HashUtil.sha3(this.toBinary());
    }

    public JsonObject toJsonObject() {

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("chain", Hex.toHexString(this.chain));
        jsonObject.addProperty("version", Hex.toHexString(this.version));
        jsonObject.addProperty("type", Hex.toHexString(this.type));
        jsonObject.addProperty("prevBlockHash", Hex.toHexString(this.prevBlockHash));
        jsonObject.addProperty("index", Hex.toHexString(ByteUtil.longToBytes(this.index)));
        jsonObject.addProperty("timestamp",
                Hex.toHexString(ByteUtil.longToBytes(this.timestamp)));
        jsonObject.addProperty("merkleRoot", Hex.toHexString(this.merkleRoot));
        jsonObject.addProperty("bodyLength",
                Hex.toHexString(ByteUtil.longToBytes(this.bodyLength)));

        return jsonObject;
    }

    public String toString() {
        return this.toJsonObject().toString();
    }

    @Override
    public BlockHeader clone() throws CloneNotSupportedException {
        return (BlockHeader) super.clone();
    }

    public boolean equals(BlockHeader newBlockHeader) {
        return Arrays.equals(this.getHashForSigning(), newBlockHeader.getHashForSigning());
    }

    static BlockHeader toBlockHeader(Proto.Block.Header protoBlockHeader) {

        BlockHeader blockHeader = new BlockHeader(
                protoBlockHeader.getChain().toByteArray(),
                protoBlockHeader.getVersion().toByteArray(),
                protoBlockHeader.getType().toByteArray(),
                protoBlockHeader.getPrevBlockHash().toByteArray(),
                ByteUtil.byteArrayToLong(protoBlockHeader.getIndex().toByteArray()),
                ByteUtil.byteArrayToLong(protoBlockHeader.getTimestamp().toByteArray()),
                protoBlockHeader.getMerkleRoot().toByteArray(),
                ByteUtil.byteArrayToLong(protoBlockHeader.getBodyLength().toByteArray())
        );

        return blockHeader;
    }
}
