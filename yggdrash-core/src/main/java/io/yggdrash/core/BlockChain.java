package io.yggdrash.core;

import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.husk.BlockHusk;
import io.yggdrash.core.store.BlockStore;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockChain {

    private static final Logger log = LoggerFactory.getLogger(BlockChain.class);
    private final JsonObject branchInfo;
    // <Variable>
    private Block prevBlock;
    private Block currentBlock;
    private Map<Object, Block> blocks; // <blockheader_hash, block>
    // For Husk
    private BlockHusk genesisBlockHusk;
    private BlockHusk prevBlockHusk;
    private BlockStore blockStore;

    @Deprecated
    public BlockChain() {
        this(new JsonObject());
    }

    public BlockChain(BlockStore blockStore) {
        this(new JsonObject());
        this.blockStore = blockStore;
    }

    public BlockChain(String chainId) {
        this(new JsonObject());
        this.blockStore = new BlockStore(chainId);
    }

    /**
     * Branch Information (Load Json File)
     *
     * @param branchInfo
     */
    public BlockChain(JsonObject branchInfo) {
        this.branchInfo = branchInfo;
        this.blocks = new ConcurrentHashMap<>();

        // @TODO loadBlockchain
        loadBlockChain();
    }

    private void loadBlockChain() {
        // @TODO check is exist;
        if (isExist()) { // Load Exist

        } else { // Generate GenensisBlock
            loadGenesis();
        }
    }

    private boolean isExist() {
        // @TODO Check exist self storage
        return false;
    }


    private boolean loadGenesis() {
        // load Genesis By PackageInfo
//        try {
//            this.genesisBlock = new GenesisBlock().getGenesisBlock();
//        } catch (IOException e) {
//            throw new NotValidateException("IOException");
//        } catch (InvalidCipherTextException e) {
//            throw new NotValidateException("InvalidCipherTextException");
//        }
//        this.prevBlock = null;
//        this.addBlock(this.genesisBlock);
        return true;
    }


    public JsonObject getPackageInfo() {
        return branchInfo;
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
    @Deprecated
    public void addBlock(Block nextBlock) {

        if (!isValidNewBlock(prevBlock, nextBlock)) {
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

    public void addBlock(BlockHusk nextBlock) {
        if (!isValidNewBlock(prevBlockHusk, nextBlock)) {
            throw new NotValidateException();
        }
        log.debug("Added block index=[{}], blockHash={}", nextBlock.getIndex(),
                nextBlock.getHash());
        this.blockStore.put(nextBlock.getHash(), nextBlock);
    }

    @Deprecated
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

    private boolean isValidNewBlock(BlockHusk prevBlock, BlockHusk nextBlock) {
        if (prevBlock == null) {
            return true;
        }
        log.trace(" prev : " + prevBlock.getHash());
        log.trace(" new : " + nextBlock.getHash());

        if (prevBlock.getIndex() + 1 != nextBlock.getIndex()) {
            log.warn("invalid index: prev:{} / new:{}", prevBlock.getIndex(), nextBlock.getIndex());
            return false;
        } else if (!prevBlock.equals(nextBlock)) {
            log.warn("invalid previous hash");
            return false;
        }

        return true;
    }

    public long size() {
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


    public BlockHusk getBlockByHash(Sha3Hash key) throws InvalidProtocolBufferException {
        return blockStore.get(key);
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
                + "prevBlock=" + prevBlock
                + ", blocks=" + blocks
                + ", packageInfo=" + branchInfo
                + '}';
    }

    /**
     * Clear.
     */
    public void clear() {
        this.blocks.clear();
        this.prevBlock = null;
    }

    public void close() {
        this.blockStore.close();
    }

    public String toStringStatus() {
//        String currentBlockHash = prevBlock.getBlockHash();
        StringBuffer stringBuffer = new StringBuffer();
//
//        stringBuffer.append("[BlockChain Status]\n")
//                .append("currentBlock=[")
//                .append(prevBlock.getIndex())
//                .append("]")
//                .append(currentBlockHash)
//                .append("\n");
//
//        String prevBlockHash = this.prevBlock.getPrevBlockHash();
        String prevBlockHash = prevBlock.getBlockHash();
        do {
            stringBuffer.append("[")
                    .append(blocks.get(prevBlockHash).getIndex())
                    .append("]")
                    .append(prevBlockHash)
                    .append("\n");

            prevBlockHash = blocks.get(prevBlockHash).getPrevBlockHash();

        } while (prevBlockHash != null
                && !prevBlockHash.equals(
                "0000000000000000000000000000000000000000000000000000000000000000"));

        return stringBuffer.toString();

    }
}
