package io.yggdrash.validator.data;

import io.yggdrash.core.blockchain.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockConChain {

    private static final Logger log = LoggerFactory.getLogger(BlockConChain.class);

    private final Map<Long, String> blockConKey = new ConcurrentHashMap<>();
    private final Map<String, BlockCon> blockConMap = new ConcurrentHashMap<>();
    private final BlockCon rootBlockCon;
    private BlockCon lastConfirmedBlockCon;
    private final Map<String, BlockCon> unConfirmedBlockConMap = new ConcurrentHashMap<>();
    private boolean isProposed;
    private boolean isConsensused;

    @Autowired
    public BlockConChain(Block genesisBlock) {
        //todo: check genesis block index, prevHash
        this.rootBlockCon = new BlockCon(0, genesisBlock.getHeader().getChain(), genesisBlock);
        this.lastConfirmedBlockCon = rootBlockCon;
        this.blockConMap.put(rootBlockCon.getHashHex(), rootBlockCon);
        this.blockConKey.put(0L, rootBlockCon.getHashHex());
    }

    public Map<Long, String> getBlockConKey() {
        return blockConKey;
    }

    public Map<String, BlockCon> getBlockConMap() {
        return blockConMap;
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
