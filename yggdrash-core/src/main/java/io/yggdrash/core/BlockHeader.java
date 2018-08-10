package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.TimeUtils;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * The type Block header.
 */
public class BlockHeader implements Serializable {

    private byte[] type;
    private byte[] version;
    private byte[] prevBlockHash;
    private byte[] merkleRoot;
    private long timestamp;
    private long dataSize;
    private byte[] signature;
    private long index;

    public BlockHeader() {
    }

    private BlockHeader(Builder builder) {
        this.type = builder.type;
        this.version = builder.version;
        this.prevBlockHash = builder.prevBlockHash;
        this.merkleRoot = builder.merkleRoot;
        this.timestamp = builder.timestamp;
        this.dataSize = builder.dataSize;
        this.signature = builder.signature;

        this.index = builder.index;
    }

    /**
     * Gets index.
     *
     * @return the index
     */
    public long getIndex() {
        return index;
    }

    /**
     * Gets timestamp.
     *
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getType() {
        return type;
    }

    public byte[] getVersion() {
        return version;
    }

    public byte[] getMerkleRoot() {
        return merkleRoot;
    }

    public long getDataSize() {
        return dataSize;
    }

    public byte[] getSignature() {
        return signature;
    }

    /**
     * Get block hash byte [ ].
     *
     * @return the byte [ ]
     */
    public byte[] getBlockHash() {
        ByteArrayOutputStream block = new ByteArrayOutputStream();

        try {
            block.write(type);
            block.write(version);
            block.write(prevBlockHash);
            block.write(merkleRoot);
            block.write(ByteUtil.longToBytes(timestamp));
            block.write(ByteUtil.longToBytes(dataSize));
            block.write(signature);
        } catch (IOException e) {
            throw new NotValidateException(e);
        }

        return HashUtil.sha3(block.toByteArray());
    }

    public byte[] getAddress() {
        ByteArrayOutputStream block = new ByteArrayOutputStream();
        try {
            block.write(type);
            block.write(version);
            block.write(prevBlockHash);
            block.write(merkleRoot);
            block.write(ByteUtil.longToBytes(timestamp));
            block.write(ByteUtil.longToBytes(dataSize));

            byte[] dataHash = HashUtil.sha3(block.toByteArray());
            ECKey keyFromSig = ECKey.signatureToKey(dataHash, signature);

            return keyFromSig.getAddress();
        } catch (Exception e) {
            throw new NotValidateException(e);
        }
    }

    /**
     * Get prev block hash byte [ ].
     *
     * @return the byte [ ]
     */
    public byte[] getPrevBlockHash() {
        return prevBlockHash;
    }

    /**
     * The type Builder.
     */
    public static class Builder {

        private final byte[] type;
        private final byte[] version;
        private byte[] prevBlockHash;
        private byte[] merkleRoot;
        private long timestamp;
        private long dataSize;
        private byte[] signature;

        private long index;

        /**
         * Instantiates a new Builder.
         */
        public Builder() {
            type = new byte[4];
            version = new byte[4];
        }

        /**
         * Prev block builder.
         *
         * @param prevBlock the prev block
         * @return the builder
         */
        public Builder prevBlock(Block prevBlock) {
            if (prevBlock == null) {
                this.index = 0;
                this.prevBlockHash = null;
            } else {
                this.index = prevBlock.nextIndex();
                this.prevBlockHash = prevBlock.getBlockByteHash();
            }
            return this;
        }

        /**
         * Block body builder.
         *
         * @param blockBody the block body
         * @return the builder
         */
        public Builder blockBody(BlockBody blockBody) {
            this.merkleRoot = blockBody.getMerkleRoot();
            this.dataSize = blockBody.getSize();
            return this;
        }

        /**
         * Build block header with wallet.
         *
         * @return the block header
         */
        public BlockHeader build(Wallet wallet) {
            timestamp = TimeUtils.getCurrenttime();
            this.signature = wallet.signHashedData(this.getDataHashForSigning());
            return new BlockHeader(this);
        }

        /**
         * Build block header.
         *
         * @return the block header
         */
        public BlockHeader build(byte[] prev, byte[] merkle, long timestamp, long size, byte[] sig,
                                 long index) {
            this.prevBlockHash = prev;
            this.merkleRoot = merkle;
            this.timestamp = timestamp;
            this.dataSize = size;
            this.signature = sig;
            this.index = index;
            return new BlockHeader(this);
        }

        /**
         * Get data hash for signing.
         *
         * @return hash of sign data
         */
        private byte[] getDataHashForSigning() {
            ByteArrayOutputStream block = new ByteArrayOutputStream();

            try {
                block.write(type);
                block.write(version);

                if (prevBlockHash == null) {
                    prevBlockHash = new byte[32];
                }
                block.write(prevBlockHash);

                if (merkleRoot == null) {
                    merkleRoot = new byte[32];
                }
                block.write(merkleRoot);

                block.write(ByteUtil.longToBytes(timestamp));
                block.write(ByteUtil.longToBytes(dataSize));

                return HashUtil.sha3(block.toByteArray());
            } catch (IOException e) {
                throw new NotValidateException(e);
            }
        }
    }

    /**
     * Convert from BlockHeader.class to JsonObject.
     * @return block as JsonObject
     */
    public JsonObject toJsonObject() {
        //todo: change to serialize method

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("type", Hex.toHexString(this.type));
        jsonObject.addProperty("version", Hex.toHexString(this.version));
        jsonObject.addProperty("prevBlockHash", Hex.toHexString(this.prevBlockHash));
        jsonObject.addProperty("merkleRoot", Hex.toHexString(this.merkleRoot));
        jsonObject.addProperty("timestamp", Hex.toHexString(ByteUtil.longToBytes(this.timestamp)));
        jsonObject.addProperty("dataSize", Hex.toHexString(ByteUtil.longToBytes(this.dataSize)));
        jsonObject.addProperty("signature", Hex.toHexString(this.signature));

        return jsonObject;
    }
}
