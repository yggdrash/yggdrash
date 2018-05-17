package io.yggdrash.core;

import io.yggdrash.crypto.Signature;
import io.yggdrash.util.HashUtils;
import io.yggdrash.util.SerializeUtils;
import io.yggdrash.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class BlockHeader implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(BlockHeader.class);

    private final byte version;
    private final byte[] payload;
    private final long index;
    private final long timestamp;
    private final byte[] prevBlockHash;
    private final byte[] author;
    private final byte[] merkleRoot;
    private final long dataSize;
    private final byte[] signature;


    /*
     * Getter & Setter
     *
     * 객체를 최대한 캡슐화 하기 위해서 getter, setter 는 최소한으로 작성. 특히 setter 는 지양
     */
    public long getIndex() {
        return index;
    }
    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getBlockHash() {
        return HashUtils.sha256(SerializeUtils.serialize(this));
    }
    public byte[] getPrevBlockHash() {
        return prevBlockHash;
    }

    private BlockHeader(Builder builder) {
        this.version = builder.version;
        this.payload = builder.payload;
        this.index = builder.index;
        this.timestamp = builder.timestamp;
        this.prevBlockHash = builder.prevBlockHash;
        this.author = builder.author;
        this.merkleRoot = builder.merkleRoot;
        this.dataSize = builder.dataSize;
        this.signature = builder.signature;
    }

    public static class Builder {

        private byte version;
        private byte[] payload;
        private long index;
        private long timestamp;
        private byte[] prevBlockHash;
        private byte[] author;
        private byte[] merkleRoot;
        private long dataSize;
        private byte[] signature;
        private Account account;

        public Builder() {
            version = 0x00;
            payload = new byte[7];
            timestamp = TimeUtils.getCurrenttime();
        }

        public Builder account(Account account) {
            this.account = account;
            this.author = account.getKey().getPublicKey();
            return this;
        }

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

        public Builder blockBody(BlockBody blockBody) {
            this.merkleRoot = blockBody.getMerkleRoot();
            this.dataSize = blockBody.getSize();
            return this;
        }

        public BlockHeader build() {
            this.signature = Signature.sign(
                    this.account.getKey(), SerializeUtils.serialize(this));
            return new BlockHeader(this);
        }
    }
}
