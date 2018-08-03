package io.yggdrash.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class Block implements Cloneable, Serializable, Comparable<Block> {

    private static final Logger log = LoggerFactory.getLogger(Block.class);

    private BlockHeader header;
    private BlockBody data;

    public Block() {}

    public Block(BlockHeader header, BlockBody data) {
        this.header = header;
        this.data = data;
    }

    /**
     * Instantiates a new Block.
     *
     * @param wallet    the wallet
     * @param prevBlock the prev block
     * @param blockBody the block body
     */
    public Block(Wallet wallet, Block prevBlock, BlockBody blockBody) {
        this.header = new BlockHeader.Builder()
                .prevBlock(prevBlock)
                .blockBody(blockBody)
                .build(wallet);

        this.data = blockBody;
    }

    @JsonIgnore
    public String getBlockHash() {
        return Hex.encodeHexString(header.getBlockHash());
    }

    @JsonIgnore
    public String getPrevBlockHash() {
        return header.getPrevBlockHash() == null ? "" :
                Hex.encodeHexString(header.getPrevBlockHash());
    }

    @JsonIgnore
    public byte[] getBlockByteHash() {
        return header.getBlockHash();
    }

    @JsonIgnore
    public long getIndex() {
        return header.getIndex();
    }

    public long nextIndex() {
        return header.getIndex() + 1;
    }

    @JsonIgnore
    public long getTimestamp() {
        return header.getTimestamp();
    }

    public BlockHeader getHeader() {
        return header;
    }

    public BlockBody getData() {
        return data;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return "Block{"
                + "header=" + header
                + ", data=" + data
                + '}';
    }

    @Override
    public int compareTo(Block o) {
        return Long.compare(header.getIndex(), o.header.getIndex());
    }
}
