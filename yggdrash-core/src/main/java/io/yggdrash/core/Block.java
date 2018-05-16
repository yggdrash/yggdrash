package io.yggdrash.core;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class Block implements Cloneable, Serializable {
    private final static Logger log = LoggerFactory.getLogger(Block.class);

    private final BlockHeader header;
    private final BlockBody data;

    public Block(BlockHeader header, BlockBody data) {
        this.header = header;
        this.data = data;
    }

    public Block(Account author, Block prevBlock, BlockBody blockBody) {
        this.header = new BlockHeader.Builder()
                .account(author)
                .prevBlock(prevBlock)
                .blockBody(blockBody)
                .build();

        this.data = blockBody;
    }

    public String getBlockHash() {
        return Hex.encodeHexString(header.getBlockHash());
    }

    public String getPrevBlockHash() {
        return Hex.encodeHexString(header.getPrevBlockHash());
    }

    byte[] getBlockByteHash() {
        return header.getBlockHash();
    }

    public long getIndex() {
        return header.getIndex();
    }

    public long nextIndex() {
        return header.getIndex() + 1;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return "Block{" +
                "header=" + header +
                ", data=" + data +
                '}';
    }
}
