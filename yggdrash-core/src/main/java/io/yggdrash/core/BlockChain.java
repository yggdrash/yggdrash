package io.yggdrash.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class BlockChain {
    private static final Logger log = LoggerFactory.getLogger(BlockChain.class);

    // <Variable>

    private Block genesisBlock;
    private Block previousBlock;
    private LinkedHashMap<byte[], Block> blocks; // <blockheader_hash, block>


    // <Constructor>

    public BlockChain() throws IOException {
        Account from = new Account();
        BlockChain bc = new BlockChain(from);
        this.genesisBlock = bc.getGenesisBlock();
        this.previousBlock = bc.getPreviousBlock();
        this.blocks = bc.getBlocks();
    }

    // create blockchain & add genesis block
    public BlockChain(Account from) throws IOException {
        this.genesisBlock = new Block(from, null, 0, null);
        this.previousBlock = genesisBlock;
        this.blocks = new LinkedHashMap<>();
        this.blocks.put(this.genesisBlock.getHeader().getHash(), this.genesisBlock);
    }



    // <Get_Set Method>

    public Block getGenesisBlock() {
        return this.genesisBlock;
    }

    public Block getPreviousBlock() {
        return this.previousBlock;
    }

    public void setPreviousBlock(Block previousBlock) {
        this.previousBlock = previousBlock;
    }

    public LinkedHashMap<byte[], Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(LinkedHashMap<byte[], Block> blocks) {
        this.blocks = blocks;
    }


    // <Method>

    public void addBlock(Block newBlock) throws IOException {
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

        blocks.put(newBlock.getHeader().getHash(), newBlock);
        this.previousBlock = newBlock;
    }

    public boolean add(Block bk) throws IOException {

        if(bk == null || bk.getHeader() == null) return false;

        this.blocks.put(bk.getHeader().getHash(), bk);
        this.previousBlock = bk;

        return true;
    }


    private boolean isGenesisBlock(Block newBlock) {
        return genesisBlock == null && previousBlock == null && newBlock.getHeader().getIndex() == 0;
    }

    private boolean isValidNewBlock(Block previousBlock, Block newBlock) throws IOException {
        if (previousBlock == null) return true;

        if (previousBlock.getHeader().getIndex() + 1 != newBlock.getHeader().getIndex()) {
            log.warn("invalid index: prev:{} / new:{}", previousBlock.getHeader().getIndex(), newBlock.getHeader().getIndex());
            return false;
        } else if (!previousBlock.getHeader().getHash().equals(newBlock.getHeader().getHash())) {
            log.warn("invalid previous hash");
            return false;
        } else if (!newBlock.getHeader().getHash().equals(newBlock.getHeader().getHash())) {
            log.warn("invalid block hash");
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
        return (this.previousBlock == null);
    }

    public void printBlockChain() {
        System.out.println("BlockChain");
        System.out.println("genesisBlock=");
        this.genesisBlock.printBlock();
        System.out.println("previousBlock=");
        if(this.previousBlock != null) this.previousBlock.printBlock();

        System.out.println("\nBlockChain");
        for (byte[] bk_hash : this.blocks.keySet()) {
            System.out.print("-"+this.blocks.get(bk_hash).getHeader().getIndex());
        }
    }

    public void clear() {
        this.blocks.clear();
        this.previousBlock = null;
        this.genesisBlock = null;
    }
}
