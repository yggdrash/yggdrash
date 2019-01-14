package io.yggdrash.validator.data;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.validator.store.BlockConStore;
import io.yggdrash.validator.store.BlockConStoreBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockConChain {

    private static final Logger log = LoggerFactory.getLogger(BlockConChain.class);

    private final Map<Long, String> blockConKey = new ConcurrentHashMap<>();
    //private final Map<String, BlockCon> blockConMap = new ConcurrentHashMap<>();
    private final BlockConStore blockConStore;

    private final BlockCon rootBlockCon;
    private BlockCon lastConfirmedBlockCon;
    private final Map<String, BlockCon> unConfirmedBlockConMap = new ConcurrentHashMap<>();
    private boolean isProposed;
    private boolean isConsensused;

    @Autowired
    public BlockConChain(Block genesisBlock) {
        if (genesisBlock.getHeader().getIndex() != 0
                || !Arrays.equals(genesisBlock.getHeader().getPrevBlockHash(), new byte[32])) {
            log.error("BlockConChain() genesisBlock is not valid.");
            throw new NotValidateException();
        }

        this.rootBlockCon = new BlockCon(0, genesisBlock.getHeader().getChain(), genesisBlock);
        this.lastConfirmedBlockCon = rootBlockCon;
        //todo: delete blockConMap when working BlockConstore
        //this.blockConMap.put(rootBlockCon.getHashHex(), rootBlockCon);
        this.blockConKey.put(0L, rootBlockCon.getHashHex());

        this.blockConStore = new BlockConStoreBuilder(
                new DefaultConfig()).buildBlockConStore(
                new BranchId(genesisBlock.getHash()));
    }

    public Map<Long, String> getBlockConKey() {
        return blockConKey;
    }

//    public Map<String, BlockCon> getBlockConMap() {
//        return blockConMap;
//    }

    public BlockConStore getBlockConStore() {
        return blockConStore;
    }

    public BlockCon getRootBlockCon() {
        return rootBlockCon;
    }

    public BlockCon getLastConfirmedBlockCon() {
        return lastConfirmedBlockCon;
    }

    public Map<String, BlockCon> getUnConfirmedBlockConMap() {
        return unConfirmedBlockConMap;
    }

    public void setLastConfirmedBlockCon(BlockCon lastConfirmedBlockCon) {
        this.lastConfirmedBlockCon = lastConfirmedBlockCon;
    }

    public boolean isProposed() {
        return isProposed;
    }

    public void setProposed(boolean proposed) {
        isProposed = proposed;
    }

    public boolean isConsensused() {
        return isConsensused;
    }

    public void setConsensused(boolean consensused) {
        isConsensused = consensused;
    }
}
