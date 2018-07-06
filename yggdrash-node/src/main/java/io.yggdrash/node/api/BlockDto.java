package io.yggdrash.node.api;

import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;

import java.io.IOException;

public class BlockDto {
    private long index;
    private String hash;
    private String previousHash;
    private long timestamp;
    private BlockBody body;

    public static BlockDto createBy(Block block) throws IOException {
        BlockDto blockDto = new BlockDto();
        blockDto.setIndex(block.getIndex());
        blockDto.setHash(block.getBlockHash());
        blockDto.setPreviousHash(block.getPrevBlockHash());
        blockDto.setTimestamp(block.getTimestamp());
        return blockDto;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public BlockBody getBody() {
        return body;
    }

    public void setBody(BlockBody body) {
        this.body = body;
    }
}
