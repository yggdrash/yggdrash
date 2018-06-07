package io.yggdrash.core;

import io.yggdrash.crypto.ECKey.ECDSASignature;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.TimeUtils;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

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
    private byte[] signatureData;

    /**
     * TransactionHeader Constructor
     * @param from account for creating tx
     * @param dataHash data hash
     * @param dataSize data size
     * @throws IOException IOException
     */
    public TransactionHeader(Account from, byte[] dataHash, long dataSize) throws IOException {
        this.version  = 0x00;
        this.type = new byte[7];

        makeTxHeader(from, dataHash, dataSize);
        makeTxHash();
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
        this.from = from.getKey().getPubKey();
        this.dataHash = dataHash;
        this.dataSize = dataSize;
        this.signatureData = getSignDataHash();
        // signature is just byteArray
        this.signature = from.getKey().sign(signatureData).toBinary();
    }

    /**
     * Make Transaction Hash
     *
     * @throws IOException
     */
    private void makeTxHash() throws IOException {
        ByteArrayOutputStream transaction = new ByteArrayOutputStream();

        transaction.write(version);
        transaction.write(type);
        transaction.write(ByteUtil.longToBytes(timestamp));
        transaction.write(this.from);
        transaction.write(this.dataHash);
        transaction.write(ByteUtil.longToBytes(dataSize));
        transaction.write(this.signature);

        this.transactionHash = HashUtil.sha3(transaction.toByteArray());
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

    public byte[] getSignature() {
        return signature;
    }

    public byte[] getSignDataHash() throws IOException {
        ByteArrayOutputStream transaction = new ByteArrayOutputStream();

        transaction.write(version);
        transaction.write(type);
        transaction.write(ByteUtil.longToBytes(timestamp));
        transaction.write(this.from);
        transaction.write(this.dataHash);
        transaction.write(ByteUtil.longToBytes(dataSize));

        return HashUtil.sha3(transaction.toByteArray());
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
