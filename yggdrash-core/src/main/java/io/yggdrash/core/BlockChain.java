package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.core.exception.NotValidteException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class BlockChain {
    private static final Logger log = LoggerFactory.getLogger(BlockChain.class);

    // <Variable>
    private Block genesisBlock;
    private Block prevBlock;
    private Map<Object, Block> blocks; // <blockheader_hash, block>
    private JsonObject packageInfo;


    // <Constructor>
    public BlockChain() {
        this.packageInfo = new JsonObject();
        this.blocks = new HashMap<>();
    }

    // create blockchain & add genesis block
    public BlockChain(JsonObject packageInfo) {
        this.packageInfo = packageInfo;
        this.blocks = new HashMap<>();
        // TODO: generate genesisBlock & add into blockchain
    }

    public JsonObject getPackageInfo() {
        return packageInfo;
    }

    // <Get_Set Method>
    public Block getGenesisBlock() {
        return this.genesisBlock;
    }

    public Block getPrevBlock() {
        return this.prevBlock;
    }

    public Map<Object, Block> getBlocks() {
        return blocks;
    }

    // <Method>

    public void addBlock(Block nextBlock) throws NotValidteException {
        if(isGenesisBlock(nextBlock)) {
                this.genesisBlock = nextBlock;
        } else if(!isValidNewBlock(prevBlock, nextBlock)) {
            throw new NotValidteException();
        }
        log.debug("blockHash : " + nextBlock.getBlockHash());
        // ADD List hash
        // TODO CHANGE DATABASE
        this.blocks.put(nextBlock.getBlockHash(), nextBlock);
        this.blocks.put(nextBlock.getIndex(), nextBlock);
        this.prevBlock = nextBlock;
    }

    private boolean isGenesisBlock(Block newBlock) {
        return genesisBlock == null && prevBlock == null && newBlock.getIndex() == 0;
    }

    private boolean isValidNewBlock(Block prevBlock, Block nextBlock) {
        if (prevBlock == null) return true;
        log.debug(" prev : " + prevBlock.getBlockHash());
        log.debug(" new : " + nextBlock.getBlockHash());

        if (prevBlock.getIndex() + 1 != nextBlock.getIndex()) {
            log.warn("invalid index: prev:{} / new:{}", prevBlock.getIndex(), nextBlock.getIndex());
            return false;
        } else if (!prevBlock.getBlockHash().equals(nextBlock.getPrevBlockHash())) {
            log.warn("invalid previous hash");
            return false;
        }

        return true;
    }

    public int size() {
        return blocks.size()/2;
    }

    public boolean isValidChain() throws IOException {
        return isValidChain(this);
    }

    public boolean isValidChain(BlockChain blockChain) throws IOException {
        if(blockChain.getPrevBlock() != null){
            Block block = blockChain.getPrevBlock(); // Get Last Block
            while(block.getIndex() != 0L) {
                block = blockChain.getBlockByHash(block.getPrevBlockHash());
            }
            return block.getIndex() == 0L;
        }
        return true;
    }

    public Block getBlockByIndex(long index) {
        return blocks.get(new Long(index));
    }

    public Block getBlockByHash(String hash) {
        return blocks.get(hash);
    }

    public Block getBlockByHash(byte[] hash) {
        return blocks.get(Hex.encodeHexString(hash));
    }


    public void replaceChain(BlockChain otherChain) throws IOException {
        if(isValidChain(otherChain) && otherChain.size() > this.size()) {
            log.info("Received blockchain is valid. Replacing current blockchain with received " +
                    "blockchain");
            this.blocks = otherChain.blocks;
            //TODO broadcastLatest();
        } else {
            log.info("Received blockchain invalid");
        }
    }

    public boolean isGenesisBlockChain() {
        return (this.prevBlock == null);
    }

    @Override
    public String toString() {
        return "BlockChain{" +
                "genesisBlock=" + genesisBlock +
                ", prevBlock=" + prevBlock +
                ", blocks=" + blocks +
                ", packageInfo=" + packageInfo +
                '}';
    }

    public void clear() {
        this.blocks.clear();
        this.prevBlock = null;
        this.genesisBlock = null;
    }
}
