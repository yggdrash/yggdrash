package io.yggdrash.core;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.yggdrash.core.exception.InternalErrorException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.Proto;
import io.yggdrash.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class BlockHeader implements Cloneable {

    public static final int CHAIN_LENGTH = 20;
    public static final int VERSION_LENGTH = 8;
    public static final int TYPE_LENGTH = 8;
    public static final int PREVBLOCKHASH_LENGTH = 32;
    public static final int INDEX_LENGTH = 8;
    public static final int TIMESTAMP_LENGTH = 8;
    public static final int MERKLEROOT_LENGTH = 32;
    public static final int BODYLENGTH_LENGTH = 8;

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

    public BlockHeader(byte[] blockHeaderBytes) {
        int pos = 0;

        this.chain = new byte[20];
        System.arraycopy(blockHeaderBytes, pos, this.chain, 0, this.chain.length);
        pos += this.chain.length;

        this.version = new byte[8];
        System.arraycopy(blockHeaderBytes, pos, this.version, 0, this.version.length);
        pos += this.version.length;

        this.type = new byte[8];
        System.arraycopy(blockHeaderBytes, pos, this.type, 0, this.type.length);
        pos += this.type.length;

        this.prevBlockHash = new byte[32];
        System.arraycopy(blockHeaderBytes, pos, this.prevBlockHash, 0, this.prevBlockHash.length);
        pos += this.prevBlockHash.length;

        byte[] indexBytes = new byte[8];
        System.arraycopy(blockHeaderBytes, pos, indexBytes, 0, indexBytes.length);
        pos += indexBytes.length;
        this.index = ByteUtil.byteArrayToLong(indexBytes);

        byte[] timestampBytes = new byte[8];
        System.arraycopy(blockHeaderBytes, pos, timestampBytes, 0, timestampBytes.length);
        pos += timestampBytes.length;
        this.timestamp = ByteUtil.byteArrayToLong(timestampBytes);

        this.merkleRoot = new byte[32];
        System.arraycopy(blockHeaderBytes, pos, this.merkleRoot, 0, this.merkleRoot.length);
        pos += this.merkleRoot.length;

        byte[] bodyLengthBytes = new byte[8];
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

    protected void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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

    public Proto.Block.Header toProtoBlockHeader() {
        return toProtoBlockHeader(this);
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
