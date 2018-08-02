package io.yggdrash.core;

import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.TimeUtils;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.SignatureException;

public class TransactionHeader implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(TransactionHeader.class);
    //todo: check Logger type for serializing(transit) or performance

    private byte[] type;
    private byte[] version;
    private byte[] dataHash;
    private long timestamp;
    private long dataSize;
    private byte[] signature;

    public TransactionHeader() {
    }

    public TransactionHeader(byte[] type,
                             byte[] version,
                             byte[] dataHash,
                             long timestamp,
                             long dataSize,
                             byte[] signature) {
        this.type = type;
        this.version = version;
        this.dataHash = dataHash;
        this.timestamp = timestamp;
        this.dataSize = dataSize;
        this.signature = signature;
    }

    /**
     * TransactionHeader Constructor.
     *
     * @param dataHash data hash
     * @param dataSize data size
     */
    public TransactionHeader(byte[] dataHash, long dataSize) {
        if (dataHash == null) {
            throw new NotValidateException("dataHash is not valid");
        }

        if (dataSize <= 0) {
            throw new NotValidateException("dataSize is not valid");
        }

        this.type = new byte[4];
        this.version = new byte[4];
        this.dataHash = dataHash;
        this.dataSize = dataSize;
        this.timestamp = TimeUtils.time();
    }

    public byte[] getType() {
        return type;
    }

    public byte[] getVersion() {
        return version;
    }

    public byte[] getDataHash() {
        return dataHash;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getDataSize() {
        return dataSize;
    }

    /**
     * Get the transaction hash.
     *
     * @return transaction hash
     */
    public byte[] getHash() {
        ByteArrayOutputStream transaction = new ByteArrayOutputStream();

        try {
            transaction.write(type);
            transaction.write(version);
            transaction.write(dataHash);
            transaction.write(ByteUtil.longToBytes(timestamp));
            transaction.write(ByteUtil.longToBytes(dataSize));
            transaction.write(signature);
        } catch (IOException e) {
            throw new NotValidateException(e);
        }

        return HashUtil.sha3(transaction.toByteArray());
    }

    /**
     * Get the transaction hash as hex string.
     *
     * @return transaction hash as hex string
     */
    public String getHashString() {
        return Hex.encodeHexString(this.getHash());
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    /**
     * Get transaction signature.
     *
     * @return transaction signature
     */
    public byte[] getSignature() {
        return signature;
    }

    /**
     * Get the data hash for signing.
     *
     * @return hash of sign data
     */
    public byte[] getDataHashForSigning() {

        if (type == null) {
            throw new NotValidateException("type is null");
        }

        if (version == null) {
            throw new NotValidateException("version is null");
        }

        if (dataHash == null) {
            throw new NotValidateException("dataHash is null");
        }

        ByteArrayOutputStream transaction = new ByteArrayOutputStream();

        try {
            transaction.write(type);
            transaction.write(version);
            transaction.write(dataHash);
            transaction.write(ByteUtil.longToBytes(timestamp));
            transaction.write(ByteUtil.longToBytes(dataSize));
        } catch (IOException e) {
            throw new NotValidateException(e);
        }

        return HashUtil.sha3(transaction.toByteArray());
    }

    /**
     * Get the address.
     *
     * @return address
     */
    public byte[] getAddress() {
        return ecKey().getAddress();
    }

    /**
     * Get the address.
     *
     * @return address
     */
    public String getAddressToString() {
        return Hex.encodeHexString(getAddress());
    }

    /**
     * Get the public key.
     *
     * @return public key
     */
    public byte[] getPubKey() {
        return ecKey().getPubKey();
    }

    /**
     * Get ECKey(include pubKey) using sig & signData.
     *
     * @return ECKey(include pubKey)
     */
    public ECKey ecKey() {
        ECKey ecKey = null;
        try {
            return ECKey.signatureToKey(getDataHashForSigning(), signature);
        } catch (SignatureException e) {
            throw new NotValidateException(e);
        }
    }

    @Override
    public String toString() {
        return "TransactionHeader{"
                + "type=" + Hex.encodeHexString(type)
                + ", version=" + Hex.encodeHexString(version)
                + ", dataHash=" + Hex.encodeHexString(dataHash)
                + ", timestamp=" + timestamp
                + ", dataSize=" + dataSize
                + ", signature=" + Hex.encodeHexString(signature)
                + '}';
    }


}
