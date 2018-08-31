package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


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
        jsonObject.addProperty("timestamp", Hex.toHexString(ByteUtil.longToBytes(this.timestamp)));
        jsonObject.addProperty("merkleRoot", Hex.toHexString(this.merkleRoot));
        jsonObject.addProperty("dataSize", Hex.toHexString(ByteUtil.longToBytes(this.bodyLength)));

        return jsonObject;
    }

    public String toString() {
        return this.toJsonObject().toString();
    }

    @Override
    public BlockHeader clone() throws CloneNotSupportedException {
        return (BlockHeader) super.clone();
    }

}
