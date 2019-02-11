package io.yggdrash.validator.data.ebft;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import io.yggdrash.validator.store.ebft.EbftBlockKeyStore;
import io.yggdrash.validator.store.ebft.EbftBlockStore;
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

public class EbftBlockChain {

    private static final Logger log = LoggerFactory.getLogger(EbftBlockChain.class);

    private final byte[] chain;
    private final String host;
    private final int port;

    private final EbftBlockKeyStore ebftBlockKeyStore;
    private final EbftBlockStore ebftBlockStore;
    private final EbftBlock rootEbftBlock;
    private EbftBlock lastConfirmedEbftBlock;
    private final Map<String, EbftBlock> unConfirmedEbftBlockMap = new ConcurrentHashMap<>();

    private final TransactionStore transactionStore;

    @Autowired
    public EbftBlockChain(Block genesisBlock, DefaultConfig defaultConfig) {
        if (genesisBlock.getHeader().getIndex() != 0
                || !Arrays.equals(genesisBlock.getHeader().getPrevBlockHash(), new byte[32])) {
            log.error("EbftBlockChain() genesisBlock is not valid.");
            throw new NotValidateException();
        }

        this.chain = genesisBlock.getHeader().getChain();
        this.host = InetAddress.getLoopbackAddress().getHostAddress();
        this.port = Integer.parseInt(System.getProperty("grpc.port"));

        this.rootEbftBlock = new EbftBlock(genesisBlock);
        this.lastConfirmedEbftBlock = rootEbftBlock;
        this.ebftBlockKeyStore = new EbftBlockKeyStore(
                new LevelDbDataSource(defaultConfig.getDatabasePath(),
                        this.host + "_" + this.port + "/" + Hex.toHexString(this.chain)
                                + "/blockkey"));
        this.ebftBlockStore = new EbftBlockStore(
                new LevelDbDataSource(defaultConfig.getDatabasePath(),
                        this.host + "_" + this.port + "/" + Hex.toHexString(this.chain)
                                + "/block"));

        EbftBlock ebftBlock = rootEbftBlock;
        if (this.ebftBlockKeyStore.size() > 0) {
            if (!Arrays.equals(this.ebftBlockKeyStore.get(0L), rootEbftBlock.getHash())) {
                log.error("EbftBlockChain() ebftBlockKeyStore is not valid.");
                throw new NotValidateException();
            }

            EbftBlock prevEbftBlock = rootEbftBlock;
            for (long l = 1; l < this.ebftBlockKeyStore.size(); l++) {
                ebftBlock = this.ebftBlockStore.get(this.ebftBlockKeyStore.get(l));
                if (Arrays.equals(prevEbftBlock.getHash(), ebftBlock.getPrevBlockHash())) {
                    prevEbftBlock = ebftBlock;
                } else {
                    log.error("EbftBlockChain() ebftBlockStore is not valid.");
                    throw new NotValidateException();
                }
            }

            this.lastConfirmedEbftBlock = ebftBlock;

        } else {
            this.ebftBlockKeyStore.put(0L, rootEbftBlock.getHash());
            this.ebftBlockStore.put(rootEbftBlock.getHash(), rootEbftBlock);
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

    public EbftBlockStore getEbftBlockStore() {
        return ebftBlockStore;
    }

    public EbftBlockKeyStore getEbftBlockKeyStore() {
        return ebftBlockKeyStore;
    }

    public TransactionStore getTransactionStore() {
        return transactionStore;
    }

    public EbftBlock getRootEbftBlock() {
        return rootEbftBlock;
    }

    public EbftBlock getLastConfirmedEbftBlock() {
        return lastConfirmedEbftBlock;
    }

    public Map<String, EbftBlock> getUnConfirmedEbftBlockMap() {
        return unConfirmedEbftBlockMap;
    }

    public void setLastConfirmedEbftBlock(EbftBlock lastConfirmedEbftBlock) {
        this.lastConfirmedEbftBlock = lastConfirmedEbftBlock;
    }

    /**
     * Get BlockList from EbftBlockStore with index, count.
     * 0 <= index && 1 < count <= 100
     *
     * @param index index of block
     * @param count count of blocks
     * @return list of EbftBlock
     */
    public List<EbftBlock> getEbftBlockList(long index, long count) {
        if (index < 0L || count < 1L || count > 100L) {
            log.debug("getEbftBlockList() index or count is not valid");
            return null;
        }

        byte[] key;
        List<EbftBlock> ebftBlockList = new ArrayList<>();
        for (long l = index; l < index + count; l++) {
            key = ebftBlockKeyStore.get(l);
            if (key != null) {
                ebftBlockList.add(ebftBlockStore.get(key));
            }
        }

        return ebftBlockList;
    }

}
