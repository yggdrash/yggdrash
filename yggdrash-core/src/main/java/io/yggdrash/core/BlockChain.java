package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.core.exception.NotValidteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class BlockChain {
    private static final Logger log = LoggerFactory.getLogger(BlockChain.class);

    // <Variable>

    private Block genesisBlock;
    private Block prevBlock;
    private LinkedHashMap<byte[], Block> blocks; // <blockheader_hash, block>
    private JsonObject packageInfo;


    // <Constructor>

    public BlockChain() throws IOException {
        this.packageInfo = new JsonObject();
        this.blocks = new LinkedHashMap<>();
    }

    // create blockchain & add genesis block
    public BlockChain(JsonObject packageInfo) throws IOException {
        this.packageInfo = packageInfo;
        this.blocks = new LinkedHashMap<>();
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

    public void setPrevBlock(Block prevBlock) {
        this.prevBlock = prevBlock;
    }

    public LinkedHashMap<byte[], Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(LinkedHashMap<byte[], Block> blocks) {
        this.blocks = blocks;
    }


    // <Method>

    public void addBlock(Block nextBlock) throws IOException, NotValidteException {
        if(isGenesisBlock(nextBlock)) {
                this.genesisBlock = nextBlock;
        } else if(!isValidNewBlock(prevBlock, nextBlock)) {
            throw new NotValidteException();
        }
        log.debug("blockhash : "+nextBlock.getHeader().hashString());
        // ADD List hash
        // TODO CHANGE DATABASE
        this.blocks.put(nextBlock.getHeader().getHash(), nextBlock);
        this.prevBlock = nextBlock;
    }

    private boolean isGenesisBlock(Block newBlock) {
        return genesisBlock == null && prevBlock == null && newBlock.getHeader().getIndex() == 0;
    }

    private boolean isValidNewBlock(Block prevBlock, Block nextBlock) throws IOException {
        if (prevBlock == null) return true;
        BlockHeader prevBlockHeader = prevBlock.getHeader();
        BlockHeader nextBlockHeader = nextBlock.getHeader();
        log.debug(" prev : "+prevBlockHeader.hashString());
        log.debug(" new : "+nextBlockHeader.hashString());

        if (prevBlockHeader.getIndex() + 1 != nextBlockHeader.getIndex()) {
            log.warn("invalid index: prev:{} / new:{}", prevBlockHeader.getIndex(), nextBlockHeader.getIndex());
            return false;
        } else if (!Arrays.equals(prevBlockHeader.getHash(), nextBlockHeader.getPre_block_hash())) {
            log.warn("invalid previous hash");
            return false;
        }

        return true;
    }

    public int size() {
        return blocks.size();
    }

    public boolean isValidChain() throws IOException {
        return isValidChain(this);
    }

    public boolean isValidChain(BlockChain blockChain) throws IOException {
        Iterator<byte[]> iterator = blockChain.blocks.keySet().iterator();
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
        Iterator<byte[]> iterator = blocks.keySet().iterator();
        for(int i = 0; i < index; i++) {
            iterator.next();
        }
        byte[] key = iterator.next();
        return blocks.get(key);
    }

    public Block getBlockByHash(String hash) {
        return blocks.get(hash);
    }

    public Block getBlockByHash(byte[] hash) {
        return blocks.get(hash);
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

    public void printBlockChain() {
        System.out.println("BlockChain");
        System.out.println("genesisBlock=");
        this.genesisBlock.printBlock();
        System.out.println("prevBlock=");
        if(this.prevBlock != null) this.prevBlock.printBlock();

        System.out.println("\nBlockChain");
        for (byte[] bk_hash : this.blocks.keySet()) {
            System.out.print("-"+this.blocks.get(bk_hash).getHeader().getIndex());
        }
    }

    public void clear() {
        this.blocks.clear();
        this.prevBlock = null;
        this.genesisBlock = null;
    }
}
