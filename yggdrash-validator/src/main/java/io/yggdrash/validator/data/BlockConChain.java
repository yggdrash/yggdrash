package io.yggdrash.validator.data;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import io.yggdrash.validator.store.BlockConKeyStore;
import io.yggdrash.validator.store.BlockConStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockConChain {

    private static final Logger log = LoggerFactory.getLogger(BlockConChain.class);

    private boolean isProposed;
    private boolean isConsensused;

    private final byte[] chain;
    private final String host;
    private final int port;

    private final BlockConKeyStore blockConKeyStore;
    private final BlockConStore blockConStore;
    private final BlockCon rootBlockCon;
    private BlockCon lastConfirmedBlockCon;
    private final Map<String, BlockCon> unConfirmedBlockConMap = new ConcurrentHashMap<>();

    private final TransactionStore transactionStore;

    @Autowired
    public BlockConChain(Block genesisBlock, DefaultConfig defaultConfig) {
        if (genesisBlock.getHeader().getIndex() != 0
                || !Arrays.equals(genesisBlock.getHeader().getPrevBlockHash(), new byte[32])) {
            log.error("BlockConChain() genesisBlock is not valid.");
            throw new NotValidateException();
        }

        this.chain = genesisBlock.getHeader().getChain();
        this.host = InetAddress.getLoopbackAddress().getHostAddress();
        this.port = Integer.parseInt(System.getProperty("grpc.port"));

        this.rootBlockCon = new BlockCon(0, this.chain, genesisBlock);
        this.lastConfirmedBlockCon = rootBlockCon;
        this.blockConKeyStore = new BlockConKeyStore(
                new LevelDbDataSource(defaultConfig.getDatabasePath(),
                        this.host + "_" + this.port + "/" + Hex.toHexString(this.chain)
                                + "/blockconkey"));
        this.blockConStore = new BlockConStore(
                new LevelDbDataSource(defaultConfig.getDatabasePath(),
                        this.host + "_" + this.port + "/" + Hex.toHexString(this.chain)
                                + "/blockcon"));

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

        this.transactionStore = new TransactionStore(
                new LevelDbDataSource(defaultConfig.getDatabasePath(),
                        this.host + "_" + this.port + "/" + Hex.toHexString(this.chain)
                                + "/txs"));

    }

    public byte[] getChain() {
        return chain;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public BlockConStore getBlockConStore() {
        return blockConStore;
    }

    public BlockConKeyStore getBlockConKeyStore() {
        return blockConKeyStore;
    }

    public TransactionStore getTransactionStore() {
        return transactionStore;
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


    /**
     * Get BlockConList from BlockConStore with index, count.
     * 0 <= index && 1 < count <= 100
     *
     * @param index index of block
     * @param count count of blocks
     * @return list of BlockCon
     */
    public List<BlockCon> getBlockConList(long index, long count) {
        if (index < 0L || count < 1L || count > 100L) {
            log.debug("getBlockConList() index or count is not valid");
            return null;
        }

        byte[] key;
        List<BlockCon> blockConList = new ArrayList<>();
        for (long l = index; l < index + count; l++) {
            key = blockConKeyStore.get(l);
            if (key != null) {
                blockConList.add(blockConStore.get(key));
            }
        }

        return blockConList;
    }

}
