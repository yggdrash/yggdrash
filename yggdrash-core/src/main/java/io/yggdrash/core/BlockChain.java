package io.yggdrash.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class BlockChain {
    private static final Logger log = LoggerFactory.getLogger(BlockChain.class);

    Block previousBlock;

    LinkedHashMap<String, Block> blocks = new LinkedHashMap<>();

    public void addBlock(Block newBlock) {
        if(!isValidNewBlock(previousBlock, newBlock)) return;

        blocks.put(newBlock.hash, newBlock);
        this.previousBlock = newBlock;
    }

    private boolean isValidNewBlock(Block previousBlock, Block newBlock) {
        if (previousBlock == null) return true;

        if (previousBlock.index + 1 != newBlock.index) {
            log.warn("invalid index: prev:{} / new:{}", previousBlock.index, newBlock.index);
            return false;
        } else if (!previousBlock.hash.equals(newBlock.previousHash)) {
            log.warn("invalid previous hash");
            return false;
        } else if (!newBlock.calculateHash().equals(newBlock.hash)) {
            log.warn("invalid block hash");
            return false;
        }

        return true;
    }

    public int size() {
        return blocks.size();
    }

    public boolean isValidChain() {
        System.out.println(blocks.keySet().iterator().next());
        return true;
    }

    public Block getBlockByIndex(int index) {
        Iterator<String> iterator = blocks.keySet().iterator();
        for(int i = 0; i < index; i++) {
            iterator.next();
        }
        String key = iterator.next();
        return blocks.get(key);
    }

    public Block getBlockByHash(String hash) {
        return blocks.get(hash);
    }
}
