package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.core.exception.NotValidateException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockChain {

    private static final Logger log = LoggerFactory.getLogger(BlockChain.class);

    // <Variable>
    private Block genesisBlock;
    private Block prevBlock;
    private Map<Object, Block> blocks; // <blockheader_hash, block>
    private final JsonObject packageInfo;

    public BlockChain() {
        this(new JsonObject());
    }

    private BlockChain(JsonObject packageInfo) {
        this.packageInfo = packageInfo;
        this.blocks = new ConcurrentHashMap<>();
        // TODO: generate genesisBlock & add into blockchain
    }

    public JsonObject getPackageInfo() {
        return packageInfo;
    }

    // <Get_Set Method>
    Block getGenesisBlock() {
        return this.genesisBlock;
    }

    public Block getPrevBlock() {
        return this.prevBlock;
    }

    public Map<Object, Block> getBlocks() {
        return blocks;
    }

    /**
     * Gets last block index.
     *
     * @return the last block index
     */
    public long getLastIndex() {
        if (isGenesisBlockChain()) {
            return 0;
        }
        return prevBlock.nextIndex();
    }

    /**
     * Add block.
     *
     * @param nextBlock the next block
     * @throws NotValidateException the not validate exception
     */
    public void addBlock(Block nextBlock) {
        if (isGenesisBlock(nextBlock)) {
            this.genesisBlock = nextBlock;
        } else if (!isValidNewBlock(prevBlock, nextBlock)) {
            throw new NotValidateException();
        }
        log.debug("Added block index=[{}], blockHash={}", nextBlock.getIndex(),
                nextBlock.getBlockHash());

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
        if (prevBlock == null) {
            return true;
        }
        log.trace(" prev : " + prevBlock.getBlockHash());
        log.trace(" new : " + nextBlock.getBlockHash());

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
        return blocks.size() / 2;
    }

    /**
     * Is valid chain boolean.
     *
     * @return the boolean
     */
    public boolean isValidChain() {
        return isValidChain(this);
    }

    /**
     * Is valid chain boolean.
     *
     * @param blockChain the block chain
     * @return the boolean
     */
    public boolean isValidChain(BlockChain blockChain) {
        if (blockChain.getPrevBlock() != null) {
            Block block = blockChain.getPrevBlock(); // Get Last Block
            while (block.getIndex() != 0L) {
                block = blockChain.getBlockByHash(block.getPrevBlockHash());
            }
            return block.getIndex() == 0L;
        }
        return true;
    }

    public Block getBlockByIndex(long index) {
        return blocks.get(index);
    }

    /**
     * Gets block by hash.
     *
     * @param hash the hash
     * @return the block by hash
     */
    public Block getBlockByHash(String hash) {
        return blocks.get(hash);
    }

    /**
     * Gets block by hash.
     *
     * @param hash the hash
     * @return the block by hash
     */
    public Block getBlockByHash(byte[] hash) {
        return blocks.get(Hex.encodeHexString(hash));
    }


    /**
     * Replace chain.
     *
     * @param otherChain the other chain
     */
    public void replaceChain(BlockChain otherChain) {
        if (isValidChain(otherChain) && otherChain.size() > this.size()) {
            log.info("Received blockchain is valid. Replacing current blockchain with received "
                    + "blockchain");
            this.blocks = otherChain.blocks;
            //TODO broadcastLatest();
        } else {
            log.info("Received blockchain invalid");
        }
    }

    /**
     * Is genesis block chain boolean.
     *
     * @return the boolean
     */
    public boolean isGenesisBlockChain() {
        return (this.prevBlock == null);
    }

    @Override
    public String toString() {
        return "BlockChain{"
                + "genesisBlock=" + genesisBlock
                + ", prevBlock=" + prevBlock
                + ", blocks=" + blocks
                + ", packageInfo=" + packageInfo
                + '}';
    }

    /**
     * Clear.
     */
    public void clear() {
        this.blocks.clear();
        this.prevBlock = null;
        this.genesisBlock = null;
    }
}
