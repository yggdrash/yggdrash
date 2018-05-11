package io.yggdrash.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockGenerator {

    private static final Logger log = LoggerFactory.getLogger(BlockGenerator.class);

    private Block previousBlock;

    public Block generate(String data, Long timestamp) {
        Block newBlock;

        if (this.previousBlock == null) {
            newBlock = new Block(0L, "", timestamp, data);
            log.debug("created genesis block: {}", newBlock);
        } else {
            newBlock = new Block(previousBlock.nextIndex(), previousBlock.hash, timestamp, data);
            log.debug("created block: {}", newBlock);
        }

        this.previousBlock = newBlock;
        return newBlock;
    }

    public Block generate(String data) {
        return generate(data, System.currentTimeMillis());
    }

    public void init() {
        this.previousBlock = null;
    }
}
