package io.yggdrash.core;

import io.yggdrash.crypto.Signature;
import io.yggdrash.util.HashUtils;
import io.yggdrash.util.SerializeUtils;
import io.yggdrash.util.TimeUtils;
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * Transaction Header Class
 */
public class TxHeader implements Serializable {

    private byte version; /** transaction version */
    private byte[] type; /** transaction type */
    private long timestamp; /** Transaction timestamp */
    private byte[] from; /** account for created tx */
    private byte[] to; /** account for targeted tx */
    private byte[] data_hash; /** transaction data hash */
    private long data_size; /** transaction data size */
    private byte[] signature; /** transaction signature */
    private byte[] txHash; /** transaction hash */

    /**
     * Transaction Header Constructor
     * @param from account for created tx
     * @param to account for targeted tx
     * @param data_hash transaction data hash
     * @param data_size transaction data size
     * @throws IOException
     */
    public TxHeader(Account from, Account to, byte[] data_hash, long data_size) throws IOException {
        this.version  = 0x00;
        this.type = new byte[7];

        makeTxHeader(from, to, data_hash, data_size);
        makeTxHash();
    }


    private void makeTxHeader(Account from, Account to, byte[] data_hash, long data_size) throws IOException {
        this.timestamp = TimeUtils.time();
        this.from = from.getKey().getPub_key();
        this.to = to.getKey().getPub_key();
        this.data_hash = data_hash;
        this.data_size = data_size;
        this.signature = null;

        this.signature = Signature.sign(from.getKey(), SerializeUtils.serialize(this));
    }


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

        outputStream.write(from);
        outputStream.write(to);
        outputStream.write(data_hash);
        outputStream.write(from);

        // Data Size to Byte[]
        buffer.clear();
        buffer.putLong(data_size);
        outputStream.write(buffer.array());

        outputStream.write(signature);

        this.txHash = HashUtils.sha256(outputStream.toByteArray());
    }

    /**
     * get transaction hash
     * @return transaction hash
     */
    public byte[] hash() {
        return this.txHash;
    }

    /**
     * get transaction hash string
     * @return transaction hash string
     */
    public String hashString() {
        return Hex.encodeHexString(this.txHash);
    }

    /**
     * get account for created tx
     * @return
     */
    public byte[] getFrom() {
        return from;
    }

    /**
     * print transaction header
     */
    public void printTxHeader() {
        System.out.println("txHash=" + Hex.encodeHexString(this.txHash));
        System.out.println("version=" + Integer.toHexString(this.version));
        System.out.println("type=" + Hex.encodeHexString(this.type));
        System.out.println("timestamp=" + this.timestamp);
        System.out.println("from=" + Hex.encodeHexString(this.from));
        System.out.println("to=" + Hex.encodeHexString(this.to));
        System.out.println("data_hash=" + Hex.encodeHexString(this.data_hash));
        System.out.println("data_size=" + this.data_size);
        System.out.println("signature=" + Hex.encodeHexString(this.signature));
    }


}
