package io.yggdrash.core;

import io.yggdrash.crypto.Signature;
import io.yggdrash.util.HashUtils;
import io.yggdrash.util.TimeUtils;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class TransactionHeader implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(TransactionHeader.class);

    private byte version;
    private byte[] type;
    private long timestamp;
    private byte[] from;
    private byte[] dataHash;
    private long dataSize;
    private byte[] signature;
    private byte[] transactionHash;

    // Constructor
    public TransactionHeader(Account from, byte[] dataHash, long dataSize) throws IOException {
        this.version  = 0x00;
        this.type = new byte[7];

        makeTxHeader(from, dataHash, dataSize);
    }

    /**
     * Transaction Header
     * @param from
     * @param dataHash
     * @param dataSize
     * @throws IOException
     */
    public void makeTxHeader(Account from, byte[] dataHash, long dataSize) throws IOException {
        this.timestamp = TimeUtils.time();
        this.from = from.getKey().getPublicKey();
        this.dataHash = dataHash;
        this.dataSize = dataSize;

        ByteArrayOutputStream transaction = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(timestamp);
        transaction.write(buffer.array());
        transaction.write(this.from);
        transaction.write(this.dataHash);

        buffer.clear();
        buffer.putLong(this.dataSize);
        transaction.write(buffer.array());

        this.signature = Signature.sign(from.getKey(), transaction.toByteArray());
        makeTxHash();
    }

    /**
     * Make Transaction Hash
     *
     * @throws IOException
     */
    private void makeTxHash() throws IOException {
        // Transaction Merge Bytes
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Long Type to byte
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

        outputStream.write(version);
        outputStream.write(type);
        // Timestamp to Byte[]
        buffer.putLong(timestamp);
        outputStream.write(buffer.array());
        outputStream.write(dataHash);
        outputStream.write(from);
        // Data Size to Byte[]
        buffer.clear();
        buffer.putLong(dataSize);
        outputStream.write(buffer.array());

        outputStream.write(signature);

        this.transactionHash = HashUtils.sha256(outputStream.toByteArray());
    }

    public byte[] hash() {
        return this.transactionHash;
    }

    public String hashString() {
        return Hex.encodeHexString(this.transactionHash);
    }

    public byte[] getFrom() {
        return from;
    }

    @Override
    public String toString() {
        return "TransactionHeader{" +
                "version=" + version +
                ", type=" + Hex.encodeHexString(type) +
                ", timestamp=" + timestamp +
                ", from=" + Hex.encodeHexString(from) +
                ", dataHash=" + Hex.encodeHexString(dataHash) +
                ", dataSize=" + dataSize +
                ", signature=" + Hex.encodeHexString(signature) +
                ", transactionHash=" + Hex.encodeHexString(transactionHash) +
                '}';
    }


}
