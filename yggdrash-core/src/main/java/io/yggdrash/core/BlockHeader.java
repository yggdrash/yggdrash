package io.yggdrash.core;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.Proto;
import io.yggdrash.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;


public class BlockHeader implements Cloneable {

    // Data format v0.0.3
    private byte[] chain;           // 20 Bytes
    private byte[] version;         // 8 Bytes
    private byte[] type;            // 8 Bytes
    private byte[] prevBlockHash;   // 32 Bytes
    private long index;             // 8 Bytes
    private long timestamp;         // 8 Bytes
    private byte[] merkleRoot;      // 32 Bytes
    private long bodyLength;        // 8 Bytes

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
        this.index = ByteUtil.byteArrayToLong(Hex.decode(jsonObject.get("index").getAsString()));
        this.timestamp = ByteUtil.byteArrayToLong(
                Hex.decode(jsonObject.get("timestamp").getAsString()));
        this.merkleRoot = Hex.decode(jsonObject.get("merkleRoot").getAsString());
        this.bodyLength = ByteUtil.byteArrayToLong(
                Hex.decode(jsonObject.get("bodyLength").getAsString()));
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


    protected void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }


    public byte[] toBinary() throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        bao.write(chain);
        bao.write(version);
        bao.write(type);
        bao.write(prevBlockHash);
        bao.write(ByteUtil.longToBytes(index));
        bao.write(ByteUtil.longToBytes(timestamp));
        bao.write(merkleRoot);
        bao.write(ByteUtil.longToBytes(bodyLength));

        return bao.toByteArray();
    }

    public long length() throws IOException {
        return this.toBinary().length;
    }

    public byte[] getHashForSignning() throws IOException {
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

    public Proto.Block.Header toProtoBlockHeader() {
        return this.toProtoBlockHeader(this);
    }

    public static Proto.Block.Header toProtoBlockHeader(BlockHeader blockHeader) {
        Proto.Block.Header protoHeader = Proto.Block.Header.newBuilder()
                .setChain(ByteString.copyFrom(blockHeader.getChain()))
                .setVersion(ByteString.copyFrom(blockHeader.getVersion()))
                .setType(ByteString.copyFrom(blockHeader.getType()))
                .setPrevBlockHash(ByteString.copyFrom(blockHeader.getPrevBlockHash()))
                .setIndex(ByteString.copyFrom(ByteUtil.longToBytes(blockHeader.getIndex())))
                .setTimestamp(ByteString.copyFrom(
                        ByteUtil.longToBytes(blockHeader.getTimestamp())))
                .setMerkleRoot(ByteString.copyFrom(blockHeader.getMerkleRoot()))
                .setBodyLength(ByteString.copyFrom(
                        ByteUtil.longToBytes(blockHeader.getBodyLength())))
                .build();

        return protoHeader;
    }

    public static BlockHeader toBlockHeader(Proto.Block.Header protoBlockHeader) {

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
