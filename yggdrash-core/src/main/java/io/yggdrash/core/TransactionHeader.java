package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.SignatureException;

public class TransactionHeader implements Cloneable {

    private static final Logger log = LoggerFactory.getLogger(TransactionHeader.class);

    // Transaction Format v0.0.3
    private byte[] chain;       // 20 Bytes
    private byte[] version;     // 8 Bytes
    private byte[] type;        // 8 Bytes
    private long timestamp;     // 8 Bytes
    private byte[] bodyHash;    // 32 Bytes
    private long bodyLength;    // 8 Bytes

    public TransactionHeader(
            byte[] chain,
            byte[] version,
            byte[] type,
            long timestamp,
            byte[] bodyHash,
            long bodyLength) {
        this.chain = chain;
        this.version = version;
        this.type = type;
        this.timestamp = timestamp;
        this.bodyHash = bodyHash;
        this.bodyLength = bodyLength;
    }

    public TransactionHeader(
            byte[] chain,
            byte[] version,
            byte[] type,
            long timestamp,
            TransactionBody txBody) {
        this.chain = chain;
        this.version = version;
        this.type = type;
        this.timestamp = timestamp;
        this.bodyHash = txBody.getBodyHash();
        this.bodyLength = txBody.length();
    }

    public long length() throws IOException {
        return this.toBinary().length;
    }

    public byte[] getChain() { return this.chain; }

    public byte[] getVersion() {
        return this.version;
    }

    public byte[] getType() {
        return this.type;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public byte[] getBodyHash() {
        return this.bodyHash;
    }

    public long getBodyLength() {
        return this.bodyLength;
    }

    protected void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Get the headerHash for signning.
     *
     * @return hash of header
     */
    public byte[] getHeaderHashForSigning() throws IOException {
        return HashUtil.sha3(this.toBinary());
    }

    /**
     * Get the binary data of TransactionHeader (84Byte)
     *
     * @return the binary data of TransactionHeader (84 byte)
     */
    public byte[] toBinary() throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        bao.write(this.chain);
        bao.write(this.version);
        bao.write(this.type);
        bao.write(ByteUtil.longToBytes(this.timestamp));
        bao.write(this.bodyHash);
        bao.write(ByteUtil.longToBytes(this.bodyLength));

        return bao.toByteArray();
    }

    /**
     * Convert from TransactionHeader to JsonObject.
     *
     * @return jsonObject of transaction header
     */
    public JsonObject toJsonObject() {

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("chain", Hex.toHexString(this.chain));
        jsonObject.addProperty("version", Hex.toHexString(this.version));
        jsonObject.addProperty("type", Hex.toHexString(this.type));
        jsonObject.addProperty("timestamp", Hex.toHexString(ByteUtil.longToBytes(this.timestamp)));
        jsonObject.addProperty("bodyHash", Hex.toHexString(this.bodyHash));
        jsonObject.addProperty("bodyLength", Hex.toHexString(ByteUtil.longToBytes(this.bodyLength)));

        return jsonObject;
    }

    public String toString() {
        return this.toJsonObject().toString();
    }

    @Override
    public TransactionHeader clone() throws CloneNotSupportedException {
        return (TransactionHeader) super.clone();
    }

}
