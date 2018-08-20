package io.yggdrash.core;

import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.genesis.GenesisBlock;
import io.yggdrash.core.store.BlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

public class BlockChain {

    private static final Logger log = LoggerFactory.getLogger(BlockChain.class);

    // <Variable>
    private BlockHusk genesisBlock;
    private BlockHusk prevBlock;
    private JsonObject packageInfo;
    private BlockStore blockStore;

    public BlockChain(String chainId) {
        this(new BlockStore(chainId));
    }

    public BlockChain(BlockStore blockStore) {
        this(new JsonObject(), blockStore);
    }

    private BlockChain(JsonObject packageInfo, BlockStore blockStore) {
        this.blockStore = blockStore;
        this.packageInfo = packageInfo;
        try {
            this.genesisBlock = new GenesisBlock().getGenesisBlock();
        } catch (IOException e) {
            throw new NotValidateException("IOException");
        } catch (InvalidCipherTextException e) {
            throw new NotValidateException("InvalidCipherTextException");
        }
        this.addBlock(this.genesisBlock);
    }

    public JsonObject getPackageInfo() {
        return packageInfo;
    }

    // <Get_Set Method>
    BlockHusk getGenesisBlock() {
        return this.genesisBlock;
    }

    public BlockHusk getPrevBlock() {
        return this.prevBlock;
    }

    public Set<BlockHusk> getBlocks() {
        return blockStore.getAll();
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
    public void addBlock(BlockHusk nextBlock) {
        if (!isValidNewBlock(prevBlock, nextBlock)) {
            throw new NotValidateException("Invalid to chain");
        }
        log.debug("Added block index=[{}], blockHash={}", nextBlock.getIndex(),
                nextBlock.getHash());
        this.blockStore.put(nextBlock);
        this.prevBlock = nextBlock;
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
        } else if (!Arrays.equals(prevBlock.getHash().getBytes(), nextBlock.getPrevHash())) {
            log.warn("invalid previous hash");
            return false;
        }

        return true;
    }

    public long size() {
        return blockStore.size();
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
            BlockHusk block = blockChain.getPrevBlock(); // Get Last Block
            while (block.getIndex() != 0L) {
                block = blockChain.getBlockByHash(block.getPrevBlockHash());
            }
            return block.getIndex() == 0L;
        }
        return true;
    }

    public BlockHusk getBlockByIndex(long index) {
        for (BlockHusk block: this.getBlocks()) {
            if (block.getIndex() == index) {
                return block;
            }
        }
        throw new NonExistObjectException("Block index=" + index);
    }

    /**
     * Gets block by hash.
     *
     * @param hash the hash
     * @return the block by hash
     */
    public BlockHusk getBlockByHash(String hash) {
        return getBlockByHash(new Sha3Hash(hash));
    }

    /**
     * Gets block by hash.
     *
     * @param hash the hash
     * @return the block by hash
     */
    public BlockHusk getBlockByHash(Sha3Hash hash) {
        try {
            return blockStore.get(hash);
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
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
                + ", height=" + this.getLastIndex()
                + ", packageInfo=" + packageInfo
                + '}';
    }

    public void close() {
        this.blockStore.close();
    }

    public String toStringStatus() {
        StringBuilder builder = new StringBuilder();

        builder.append("[BlockChain Status]\n")
                .append("genesisBlock=")
                .append(genesisBlock.getHash()).append("\n").append("currentBlock=" + "[")
                .append(prevBlock.getIndex()).append("]").append(prevBlock.getHash()).append("\n");

        String prevBlockHash = this.prevBlock.getPrevBlockHash();
        if (prevBlockHash == null) {
            prevBlockHash = "";
        }

        try {
            do {
                builder.append("<-- " + "[")
                        .append(blockStore.get(new Sha3Hash(prevBlockHash)).getIndex())
                        .append("]").append(prevBlockHash).append("\n");

                prevBlockHash = blockStore.get(new Sha3Hash(prevBlockHash)).getPrevBlockHash();

            } while (prevBlockHash != null
                    && !prevBlockHash.equals(
                        "0000000000000000000000000000000000000000000000000000000000000000"));
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }

        return builder.toString();

    }
}
