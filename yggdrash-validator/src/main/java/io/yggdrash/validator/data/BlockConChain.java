package io.yggdrash.validator.data;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import io.yggdrash.validator.store.BlockConKeyStore;
import io.yggdrash.validator.store.BlockConStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockConChain {

    private static final Logger log = LoggerFactory.getLogger(BlockConChain.class);

    private final BlockConKeyStore blockConKeyStore;
    private final BlockConStore blockConStore;
    private final BlockCon rootBlockCon;
    private BlockCon lastConfirmedBlockCon;
    private final Map<String, BlockCon> unConfirmedBlockConMap = new ConcurrentHashMap<>();
    private boolean isProposed;
    private boolean isConsensused;

    @Autowired
    public BlockConChain(Block genesisBlock, DefaultConfig defaultConfig) {
        if (genesisBlock.getHeader().getIndex() != 0
                || !Arrays.equals(genesisBlock.getHeader().getPrevBlockHash(), new byte[32])) {
            log.error("BlockConChain() genesisBlock is not valid.");
            throw new NotValidateException();
        }

        this.rootBlockCon = new BlockCon(0, genesisBlock.getHeader().getChain(), genesisBlock);
        this.lastConfirmedBlockCon = rootBlockCon;
        this.blockConKeyStore = new BlockConKeyStore(
                new LevelDbDataSource(defaultConfig.getDatabasePath(),
                        Hex.toHexString(genesisBlock.getHeader().getChain())
                                + "/blockconkey"
                                + System.getProperty("grpc.port")));
        this.blockConStore = new BlockConStore(
                new LevelDbDataSource(defaultConfig.getDatabasePath(),
                        Hex.toHexString(genesisBlock.getHeader().getChain())
                                + "/blockcon"
                                + System.getProperty("grpc.port")));

        BlockCon blockCon = rootBlockCon;
        if (this.blockConKeyStore.size() > 0) {
            if (!Arrays.equals(this.blockConKeyStore.get(0L), rootBlockCon.getHash())) {
                log.error("BlockConChain() blockConKeyStore is not valid.");
                throw new NotValidateException();
            }

            BlockCon prevBlockCon = rootBlockCon;
            for (long l = 1; l < this.blockConKeyStore.size(); l++) {
                blockCon = this.blockConStore.get(this.blockConKeyStore.get(l));
                if (Arrays.equals(prevBlockCon.getHash(), blockCon.getPrevBlockHash())) {
                    prevBlockCon = blockCon;
                } else {
                    log.error("BlockConChain() blockConStore is not valid.");
                    throw new NotValidateException();
                }
            }

            this.lastConfirmedBlockCon = blockCon;

        } else {
            this.blockConKeyStore.put(0L, rootBlockCon.getHash());
            this.blockConStore.put(rootBlockCon.getHash(), rootBlockCon);
        }

    }

    public BlockConStore getBlockConStore() {
        return blockConStore;
    }

    public BlockConKeyStore getBlockConKeyStore() {
        return blockConKeyStore;
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
