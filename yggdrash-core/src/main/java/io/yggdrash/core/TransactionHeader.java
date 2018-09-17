package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.core.exception.InternalErrorException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
        this(chain, version, type, timestamp, txBody.getBodyHash(), txBody.length());
    }

    public TransactionHeader(JsonObject jsonObject) {
        this.chain = Hex.decode(jsonObject.get("chain").getAsString());
        this.version = Hex.decode(jsonObject.get("version").getAsString());
        this.type = Hex.decode(jsonObject.get("type").getAsString());
        this.timestamp = ByteUtil.byteArrayToLong(
                Hex.decode(jsonObject.get("timestamp").getAsString()));
        this.bodyHash = Hex.decode(jsonObject.get("bodyHash").getAsString());
        this.bodyLength = ByteUtil.byteArrayToLong(
                Hex.decode(jsonObject.get("bodyLength").getAsString()));
    }

    public TransactionHeader(byte[] txHeaderBytes) {
        int pos = 0;

        this.chain = new byte[20];
        System.arraycopy(txHeaderBytes, pos, this.chain, 0, this.chain.length);
        pos += this.chain.length;

        this.version = new byte[8];
        System.arraycopy(txHeaderBytes, pos, this.version, 0, this.version.length);
        pos += this.version.length;

        this.type = new byte[8];
        System.arraycopy(txHeaderBytes, pos, this.type, 0, this.type.length);
        pos += this.type.length;

        byte[] timestampBytes = new byte[8];
        System.arraycopy(txHeaderBytes, pos, timestampBytes, 0, timestampBytes.length);
        this.timestamp = ByteUtil.byteArrayToLong(timestampBytes);
        pos += timestampBytes.length;

        this.bodyHash = new byte[32];
        System.arraycopy(txHeaderBytes, pos, this.bodyHash, 0, this.bodyHash.length);
        pos += this.bodyHash.length;

        byte[] bodyLengthBytes = new byte[8];
        System.arraycopy(txHeaderBytes, pos, bodyLengthBytes, 0, bodyLengthBytes.length);
        this.bodyLength = ByteUtil.byteArrayToLong(bodyLengthBytes);
        pos += bodyLengthBytes.length;

        if (pos != txHeaderBytes.length) {
            throw new NotValidateException();
        }

    }

    public long length() throws IOException {
        return this.toBinary().length;
    }

    public byte[] getChain() {
        return this.chain;
    }

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

    /**
     * Get the headerHash for signning.
     *
     * @return hash of header
     */
    public byte[] getHashForSignning() {
        return HashUtil.sha3(this.toBinary());
    }

    /**
     * Get the binary data of TransactionHeader (84Byte)
     *
     * @return the binary data of TransactionHeader (84 byte)
     */
    public byte[] toBinary() {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        try {
            bao.write(this.chain);
            bao.write(this.version);
            bao.write(this.type);
            bao.write(ByteUtil.longToBytes(this.timestamp));
            bao.write(this.bodyHash);
            bao.write(ByteUtil.longToBytes(this.bodyLength));

            return bao.toByteArray();
        } catch (IOException e) {
            throw new InternalErrorException("toBinary error");
        }
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
        jsonObject.addProperty("bodyLength",
                Hex.toHexString(ByteUtil.longToBytes(this.bodyLength)));

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
