package io.yggdrash.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class BlockChain {
    private static final Logger log = LoggerFactory.getLogger(BlockChain.class);

    Block previousBlock;
    Block genesisBlock;

    LinkedHashMap<String, Block> blocks = new LinkedHashMap<>();

    public void addBlock(Block newBlock) {
        if(isGenesisBlock(newBlock)) {
            try {
                this.genesisBlock = (Block) newBlock.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                log.warn("{}", e);
            }
        } else if(!isValidNewBlock(previousBlock, newBlock)) {
            //TODO Exception 날려야 한다.
            return;
        }

        blocks.put(newBlock.hash, newBlock);
        this.previousBlock = newBlock;
    }

    private boolean isGenesisBlock(Block newBlock) {
        return genesisBlock == null && previousBlock == null && newBlock.index == 0;
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
        return isValidChain(this);
    }

    public boolean isValidChain(BlockChain blockChain) {
        Iterator<String> iterator = blockChain.blocks.keySet().iterator();
        Block firstBlock = blockChain.blocks.get(iterator.next());
        if (!this.genesisBlock.equals(firstBlock)) return false;

        Block previousBlock = firstBlock;
        Block nextBlock;
        while (iterator.hasNext()) {
            nextBlock = blockChain.blocks.get(iterator.next());
            if (!isValidNewBlock(previousBlock, nextBlock)) {
                return false;
            }
            previousBlock = nextBlock;
        }
        return true;
    }

    public Block getBlockByIndex(int index) {
        if(blocks.size() - 1 < index) return null;

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

    public void replaceChain(BlockChain otherChain) {
        if(isValidChain(otherChain) && otherChain.size() > this.size()) {
            log.info("Received blockchain is valid. Replacing current blockchain with received " +
                    "blockchain");
            this.blocks = otherChain.blocks;
            //TODO broadcastLatest();
        } else {
            log.info("Received blockchain invalid");
        }
    }

    public LinkedHashMap<String, Block> getBlocks() {
        return blocks;
    }

    public void clear() {
        this.blocks.clear();
        this.previousBlock = null;
        this.genesisBlock = null;
    }
}
