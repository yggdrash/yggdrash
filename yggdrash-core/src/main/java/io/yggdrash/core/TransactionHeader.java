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

@Deprecated
public class TransactionHeader implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(TransactionHeader.class);
    //todo: check Logger type for serializing(transit) or performance

    private byte[] type;
    private byte[] version;
    private byte[] dataHash;
    private long dataSize;
    private long timestamp;
    private byte[] signature;

    public TransactionHeader() {
    }

    public TransactionHeader(byte[] type,
                             byte[] version,
                             byte[] dataHash,
                             long dataSize,
                             long timestamp,
                             byte[] signature) {
        this.type = type;
        this.version = version;
        this.dataHash = dataHash;
        this.dataSize = dataSize;
        this.timestamp = timestamp;
        this.signature = signature;
    }

    /**
     * TransactionHeader Constructor.
     *
     * @param dataHash data hash
     * @param dataSize data size
     */
    public TransactionHeader(Wallet wallet, byte[] dataHash, long dataSize) {
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
        this.signature = wallet.signHashedData(getDataHashForSigning());
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
            transaction.write(ByteUtil.longToBytes(dataSize));
            transaction.write(ByteUtil.longToBytes(timestamp));
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
        return Hex.toHexString(this.getHash());
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
        ByteArrayOutputStream transaction = new ByteArrayOutputStream();

        try {
            transaction.write(type);
            transaction.write(version);
            transaction.write(dataHash);
            transaction.write(ByteUtil.longToBytes(dataSize));
            transaction.write(ByteUtil.longToBytes(timestamp));
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
        return Hex.toHexString(getAddress());
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
        try {
            return ECKey.signatureToKey(getDataHashForSigning(), signature);
        } catch (SignatureException e) {
            throw new NotValidateException(e);
        }
    }

    @Override
    public String toString() {
        return "TransactionHeader{"
                + "type=" + Hex.toHexString(type)
                + ", version=" + Hex.toHexString(version)
                + ", dataHash=" + Hex.toHexString(dataHash)
                + ", timestamp=" + timestamp
                + ", dataSize=" + dataSize
                + ", signature=" + Hex.toHexString(signature)
                + '}';
    }

    /**
     * Convert from TransactionHeader.class to JsonObject.
     * @return transaction JsonObject
     */
    public JsonObject toJsonObject() {
        //todo: change to serialize method

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("type", Hex.toHexString(this.type));
        jsonObject.addProperty("version", Hex.toHexString(this.version));
        jsonObject.addProperty("dataHash", Hex.toHexString(this.dataHash));
        jsonObject.addProperty("timestamp", Hex.toHexString(ByteUtil.longToBytes(this.timestamp)));
        jsonObject.addProperty("dataSize", Hex.toHexString(ByteUtil.longToBytes(this.dataSize)));
        jsonObject.addProperty("signature", Hex.toHexString(this.signature));

        return jsonObject;
    }

}
