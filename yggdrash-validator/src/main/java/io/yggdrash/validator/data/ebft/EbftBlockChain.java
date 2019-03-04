package io.yggdrash.validator.data.ebft;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
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

import static io.yggdrash.common.config.Constants.EMPTY_BYTE32;

public class EbftBlockChain {

    private static final Logger log = LoggerFactory.getLogger(EbftBlockChain.class);

    private final byte[] chain;
    private final String host;
    private final int port;

    private final EbftBlockKeyStore blockKeyStore;
    private final EbftBlockStore blockStore;
    private final EbftBlock genesisBlock;
    private final Map<String, EbftBlock> unConfirmedBlockMap = new ConcurrentHashMap<>();
    private final TransactionStore transactionStore;

    private EbftBlock lastConfirmedBlock;

    @Autowired
    public EbftBlockChain(Block genesisBlock, DefaultConfig defaultConfig) {
        if (genesisBlock.getHeader().getIndex() != 0
                || !Arrays.equals(genesisBlock.getHeader().getPrevBlockHash(), EMPTY_BYTE32)) {
            log.error("GenesisBlock is not valid.");
            throw new NotValidateException();
        }

        this.chain = genesisBlock.getHeader().getChain();
        this.host = InetAddress.getLoopbackAddress().getHostAddress();
        this.port = Integer.parseInt(System.getProperty("grpc.port"));

        this.genesisBlock = new EbftBlock(genesisBlock);
        this.lastConfirmedBlock = this.genesisBlock;
        this.blockKeyStore = new EbftBlockKeyStore(
                new LevelDbDataSource(defaultConfig.getDatabasePath(),
                        this.host + "_" + this.port + "/" + Hex.toHexString(this.chain)
                                + "/ebftblockkey"));
        this.blockStore = new EbftBlockStore(
                new LevelDbDataSource(defaultConfig.getDatabasePath(),
                        this.host + "_" + this.port + "/" + Hex.toHexString(this.chain)
                                + "/ebftblock"));

        EbftBlock ebftBlock = this.genesisBlock;
        if (this.blockKeyStore.size() > 0) {
            if (!Arrays.equals(this.blockKeyStore.get(0L), this.genesisBlock.getHash())) {
                log.error("EbftBlockChain() blockKeyStore is not valid.");
                throw new NotValidateException();
            }

            EbftBlock prevEbftBlock = this.genesisBlock;
            for (long l = 1; l < this.blockKeyStore.size(); l++) {
                ebftBlock = this.blockStore.get(this.blockKeyStore.get(l));
                if (Arrays.equals(prevEbftBlock.getHash(), ebftBlock.getPrevBlockHash())) {
                    prevEbftBlock = ebftBlock;
                } else {
                    log.error("EbftBlockChain() blockStore is not valid.");
                    throw new NotValidateException();
                }
            }

            this.lastConfirmedBlock = ebftBlock;

        } else {
            this.blockKeyStore.put(0L, this.genesisBlock.getHash());
            this.blockStore.put(this.genesisBlock.getHash(), this.genesisBlock);
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

    public EbftBlockStore getBlockStore() {
        return blockStore;
    }

    public EbftBlockKeyStore getBlockKeyStore() {
        return blockKeyStore;
    }

    public TransactionStore getTransactionStore() {
        return transactionStore;
    }

    public EbftBlock getGenesisBlock() {
        return genesisBlock;
    }

    public EbftBlock getLastConfirmedBlock() {
        return lastConfirmedBlock;
    }

    public Map<String, EbftBlock> getUnConfirmedBlockMap() {
        return unConfirmedBlockMap;
    }

    public void setLastConfirmedBlock(EbftBlock lastConfirmedBlock) {
        this.lastConfirmedBlock = lastConfirmedBlock;
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
            key = blockKeyStore.get(l);
            if (key != null) {
                ebftBlockList.add(blockStore.get(key));
            }
        }

        return ebftBlockList;
    }

}
