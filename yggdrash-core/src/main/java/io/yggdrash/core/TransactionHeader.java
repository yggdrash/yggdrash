package io.yggdrash.core;

import io.yggdrash.crypto.Signature;
import io.yggdrash.util.HashUtils;
import io.yggdrash.util.SerializeUtils;
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
    private byte[] data_hash;
    private long data_size;
    private byte[] signature;
    private byte[] txHash;

    // Constructor
    public TransactionHeader(Account from, byte[] data_hash, long data_size) throws IOException {
        this.version  = 0x00;
        this.type = new byte[7];

        makeTxHeader(from, data_hash, data_size);
    }

    /**
     * Transaction Header
     * @param from
     * @param data_hash
     * @param data_size
     * @throws IOException
     */
    public void makeTxHeader(Account from, byte[] data_hash, long data_size) throws IOException {
        this.timestamp = TimeUtils.time();
        this.from = from.getKey().getPub_key();
        this.data_hash = data_hash;
        this.data_size = data_size;

        ByteArrayOutputStream transaction = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(timestamp);
        transaction.write(buffer.array());
        transaction.write(this.from);
        transaction.write(this.data_hash);

        buffer.clear();
        buffer.putLong(this.data_size);
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
        outputStream.write(data_hash);
        outputStream.write(from);
        // Data Size to Byte[]
        buffer.clear();
        buffer.putLong(data_size);
        outputStream.write(buffer.array());

        outputStream.write(signature);

        this.txHash = HashUtils.sha256(outputStream.toByteArray());
    }

    public byte[] hash() {
        return this.txHash;
    }

    public String hashString() {
        return Hex.encodeHexString(this.txHash);
    }

    public byte[] getFrom() {
        return from;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("transactionHash=").append(Hex.encodeHexString(this.txHash));
        buffer.append("version=").append(Integer.toHexString(this.version));
        buffer.append("type=").append(Hex.encodeHexString(this.type));
        buffer.append("timestamp=").append(this.timestamp);
        buffer.append("from=").append(Hex.encodeHexString(this.from));
        buffer.append("dataHash=").append(Hex.encodeHexString(this.data_hash));
        buffer.append("dataSize=").append(this.data_size);
        buffer.append("signature=").append(Hex.encodeHexString(this.signature));
        return buffer.toString();
    }


}
