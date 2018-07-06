package io.yggdrash.core;

import com.google.protobuf.ByteString;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.BlockChainProto;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.SerializeUtils;
import io.yggdrash.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * The type Block header.
 */
public class BlockHeader implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(BlockHeader.class);

    private final byte[] type;
    private final byte[] version;
    private final byte[] prevBlockHash;
    private final byte[] merkleRoot;
    private final long timestamp;
    private final long dataSize;
    private final byte[] signature;

    private final long index;

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
     * Value of block header.
     *
     * @param protoHeader the proto header
     * @return the block header
     */
    public static BlockHeader valueOf(BlockChainProto.BlockHeader protoHeader) {
        Builder builder = new Builder();
        builder.index = protoHeader.getIndex();
        builder.prevBlockHash = protoHeader.getPrevBlockHash().toByteArray();
        builder.merkleRoot = protoHeader.getMerkleRoot().toByteArray();
        builder.timestamp = protoHeader.getTimestamp();
        builder.dataSize = protoHeader.getDataSize();
        builder.signature = protoHeader.getSignature().toByteArray();
        return new BlockHeader(builder);
    }

    /**
     * Of block chain proto . block header.
     *
     * @param header the header
     * @return the block chain proto . block header
     */
    public static BlockChainProto.BlockHeader of(BlockHeader header) {
        BlockChainProto.BlockHeader.Builder builder = BlockChainProto.BlockHeader.newBuilder()
                .setType(ByteString.copyFrom(header.type))
                .setVersion(ByteString.copyFrom(header.version))
                .setIndex(header.index)
                .setTimestamp(header.timestamp)
                .setDataSize(header.dataSize);

        if (header.prevBlockHash != null) {
            builder.setPrevBlockHash(ByteString.copyFrom(header.prevBlockHash));
        }
        if (header.merkleRoot != null) {
            builder.setMerkleRoot(ByteString.copyFrom(header.merkleRoot));
        }
        if (header.signature != null) {
            builder.setSignature(ByteString.copyFrom(header.signature));
        }
        return builder.build();
    }

    /**
     * Gets index.
     *
     * @return the index
     */
    /*
     * Getter & Setter
     *
     * 객체를 최대한 캡슐화 하기 위해서 getter, setter 는 최소한으로 작성. 특히 setter 는 지양
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

    /**
     * Get block hash byte [ ].
     *
     * @return the byte [ ]
     */
    public byte[] getBlockHash() throws IOException {
        ByteArrayOutputStream block = new ByteArrayOutputStream();

        block.write(type);
        block.write(version);
        if (prevBlockHash != null) {
            block.write(prevBlockHash);
        }
        if (merkleRoot != null) {
            block.write(merkleRoot);
        }
        block.write(ByteUtil.longToBytes(timestamp));
        block.write(ByteUtil.longToBytes(dataSize));
        block.write(signature);
        block.write(ByteUtil.longToBytes(index));

        return HashUtil.sha256(block.toByteArray());
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

        private byte[] type;
        private byte[] version;
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
        public Builder prevBlock(Block prevBlock) throws IOException {
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
        public Builder blockBody(BlockBody blockBody) throws IOException {
            this.merkleRoot = blockBody.getMerkleRoot();
            this.dataSize = blockBody.getSize();
            return this;
        }

        /**
         * Build block header.
         *
         * @return the block header
         */
        public BlockHeader build(Account from) {
            timestamp = TimeUtils.getCurrenttime();
            this.signature = from.getKey().sign(
                    HashUtil.sha256(SerializeUtils.serialize(this))).toByteArray();
            return new BlockHeader(this);
        }
    }
}
