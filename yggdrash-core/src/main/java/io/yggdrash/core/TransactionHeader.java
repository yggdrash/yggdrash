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

public class TransactionHeader implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(TransactionHeader.class);
    //todo: check Logger type for serializing(transit) or performance

    private byte[] chain;       // 20 Bytes
    private byte[] version;     // 8 Bytes
    private byte[] type;        // 8 Bytes
    private long timestamp;     // 8 Bytes
    private byte[] bodyHash;    // 32 Bytes
    private long bodyLength;      // 8 Bytes

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

//    /**
//     * TransactionHeader Constructor.
//     *
//     * @param dataHash data hash
//     * @param dataSize data size
//     */
//    public TransactionHeader(Wallet wallet, byte[] dataHash, long dataSize) {
//        if (dataHash == null) {
//            throw new NotValidateException("dataHash is not valid");
//        }
//
//        if (dataSize <= 0) {
//            throw new NotValidateException("dataSize is not valid");
//        }
//
//        this.type = new byte[4];
//        this.version = new byte[4];
//        this.dataHash = dataHash;
//        this.dataSize = dataSize;
//        this.timestamp = TimeUtils.time();
//        this.signature = wallet.signHashedData(getDataHashForSigning());
//    }

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

//    /**
//     * Get the transaction hash.
//     *
//     * @return transaction hash
//     */
//    public byte[] getHash() {
//        ByteArrayOutputStream transaction = new ByteArrayOutputStream();
//
//        try {
//            transaction.write(type);
//            transaction.write(version);
//            transaction.write(dataHash);
//            transaction.write(ByteUtil.longToBytes(dataSize));
//            transaction.write(ByteUtil.longToBytes(timestamp));
//            transaction.write(signature);
//        } catch (IOException e) {
//            throw new NotValidateException(e);
//        }
//
//        return HashUtil.sha3(transaction.toByteArray());
//    }

//    /**
//     * Get the transaction hash as hex string.
//     *
//     * @return transaction hash as hex string
//     */
//    public String getHashString() {
//        return Hex.toHexString(this.getHash());
//    }


    /**
     * Get the headerHash for signning.
     *
     * @return hash of header
     */
    public byte[] getHeaderHashForSigning() throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        bao.write(this.chain);
        bao.write(this.version);
        bao.write(this.type);
        bao.write(ByteUtil.longToBytes(this.timestamp));
        bao.write(this.bodyHash);
        bao.write(ByteUtil.longToBytes(this.bodyLength));

        return HashUtil.sha3(bao.toByteArray());
    }

//    /**
//     * Get the address.
//     *
//     * @return address
//     */
//    public byte[] getAddress() {
//        return ecKey().getAddress();
//    }

//    /**
//     * Get the address.
//     *
//     * @return address
//     */
//    public String getAddressToString() {
//        return Hex.toHexString(getAddress());
//    }

//    /**
//     * Get the public key.
//     *
//     * @return public key
//     */
//    public byte[] getPubKey() {
//        return ecKey().getPubKey();
//    }
//
//    /**
//     * Get ECKey(include pubKey) using sig & signData.
//     *
//     * @return ECKey(include pubKey)
//     */
//    public ECKey ecKey() {
//        try {
//            return ECKey.signatureToKey(getDataHashForSigning(), signature);
//        } catch (SignatureException e) {
//            throw new NotValidateException(e);
//        }
//    }

    @Override
    public String toString() {
        return "TransactionHeader{"
                + "chain=" + Hex.toHexString(chain)
                + ", version=" + Hex.toHexString(version)
                + ", type=" + Hex.toHexString(type)
                + ", timestamp=" + Hex.toHexString(ByteUtil.longToBytes(timestamp))
                + ", bodyHash=" + Hex.toHexString(bodyHash)
                + ", bodyLength=" + Hex.toHexString(ByteUtil.longToBytes(bodyLength))
                + "}";
    }

    /**
     * Convert from TransactionHeader.class to JsonObject.
     * @return transaction JsonObject
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

}
