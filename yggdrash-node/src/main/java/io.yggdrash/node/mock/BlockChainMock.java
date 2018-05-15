package io.yggdrash.node.mock;

import io.yggdrash.node.BlockChain;

import java.util.LinkedHashMap;

public class BlockChainMock implements BlockChain {
    private LinkedHashMap<String, Block> blocks = new LinkedHashMap<>();

    @Override
    public Block addBlock(Block nextBlock) {
        blocks.put("Hash", nextBlock);
        return nextBlock;
    }

    @Override
    public Block getBlockByIndex(int index) {
        return new Block();
    }

    @Override
    public Block getBlockByHash(String hash) {
        return blocks.get(hash);
    }

    @Override
    public LinkedHashMap<byte[], Block> getBlocks() {
        return null;
    }
}
