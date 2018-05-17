package io.yggdrash.node.mock;

import io.yggdrash.core.Block;
import io.yggdrash.node.BlockChain;

import java.util.LinkedHashMap;

public class BlockChainMock implements BlockChain {
    private LinkedHashMap<String, Block> blocks = new LinkedHashMap<>();

    @Override
    public Block addBlock(Block nextBlock) {
        blocks.put(nextBlock.getBlockHash(), nextBlock);
        blocks.put(String.valueOf(nextBlock.getIndex()), nextBlock);
        return nextBlock;
    }

    @Override
    public Block getBlockByIndex(int index) {
        return blocks.get(String.valueOf(index));
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
