package io.yggdrash.validator.data.pbft;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import io.yggdrash.validator.store.pbft.PbftBlockKeyStore;
import io.yggdrash.validator.store.pbft.PbftBlockStore;
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

public class PbftBlockChain {

    private static final Logger log = LoggerFactory.getLogger(PbftBlockChain.class);
    public static final boolean TEST_NONE_TXSTORE = false;

    private final byte[] chain;
    private final String host;
    private final int port;

    private final PbftBlockKeyStore blockKeyStore;
    private final PbftBlockStore blockStore;
    private final PbftBlock genesisBlock;
    private final Map<String, PbftMessage> unConfirmedMsgMap = new ConcurrentHashMap<>();
    private final TransactionStore transactionStore;

    private PbftBlock lastConfirmedBlock;

    @Autowired
    public PbftBlockChain(Block genesisBlock, DefaultConfig defaultConfig) {
        if (genesisBlock.getHeader().getIndex() != 0
                || !Arrays.equals(genesisBlock.getHeader().getPrevBlockHash(), EMPTY_BYTE32)) {
            log.error("GenesisBlock is not valid.");
            throw new NotValidateException();
        }

        this.chain = genesisBlock.getHeader().getChain();
        this.host = InetAddress.getLoopbackAddress().getHostAddress();
        this.port = Integer.parseInt(System.getProperty("grpc.port"));

        this.genesisBlock = new PbftBlock(genesisBlock, null);
        this.lastConfirmedBlock = this.genesisBlock;
        this.blockKeyStore = new PbftBlockKeyStore(
                new LevelDbDataSource(defaultConfig.getDatabasePath(),
                        this.host + "_" + this.port + "/" + Hex.toHexString(this.chain)
                                + "/pbftblockkey"));
        this.blockStore = new PbftBlockStore(
                new LevelDbDataSource(defaultConfig.getDatabasePath(),
                        this.host + "_" + this.port + "/" + Hex.toHexString(this.chain)
                                + "/pbftblock"));

        PbftBlock pbftBlock = this.genesisBlock;
        if (this.blockKeyStore.size() > 0) {
            if (!Arrays.equals(this.blockKeyStore.get(0L), this.genesisBlock.getHash())) {
                log.error("PbftBlockKeyStore is not valid.");
                throw new NotValidateException();
            }

            PbftBlock prevPbftBlock = this.genesisBlock;
            for (long l = 1; l < this.blockKeyStore.size(); l++) {
                pbftBlock = this.blockStore.get(this.blockKeyStore.get(l));
                if (Arrays.equals(prevPbftBlock.getHash(), pbftBlock.getPrevBlockHash())) {
                    prevPbftBlock = pbftBlock;
                } else {
                    log.error("PbftBlockChain() bpbftBlockStore is not valid.");
                    throw new NotValidateException();
                }
            }

            this.lastConfirmedBlock = pbftBlock;

        } else {
            this.blockKeyStore.put(0L, this.genesisBlock.getHash());
            this.blockStore.put(this.genesisBlock.getHash(), this.genesisBlock);
        }

        if (TEST_NONE_TXSTORE) {
            this.transactionStore = null;
        } else {
            this.transactionStore = new TransactionStore(
                    new LevelDbDataSource(defaultConfig.getDatabasePath(),
                            this.host + "_" + this.port + "/" + Hex.toHexString(this.chain)
                                    + "/txs"));
        }
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

    public PbftBlockKeyStore getBlockKeyStore() {
        return blockKeyStore;
    }

    public PbftBlockStore getBlockStore() {
        return blockStore;
    }

    public PbftBlock getGenesisBlock() {
        return genesisBlock;
    }

    public void setLastConfirmedBlock(PbftBlock lastConfirmedBlock) {
        this.lastConfirmedBlock = lastConfirmedBlock;
    }

    public PbftBlock getLastConfirmedBlock() {
        return lastConfirmedBlock;
    }

    public Map<String, PbftMessage> getUnConfirmedMsgMap() {
        return unConfirmedMsgMap;
    }

    public TransactionStore getTransactionStore() {
        return transactionStore;
    }

    public List<PbftBlock> getPbftBlockList(long index, long count) {
        if (index < 0L || count < 1L || count > 100L) {
            log.debug("getPbftBlockList() index or count is not valid");
            return null;
        }

        byte[] key;
        List<PbftBlock> pbftBlockList = new ArrayList<>();
        for (long l = index; l < index + count; l++) {
            key = blockKeyStore.get(l);
            if (key != null) {
                pbftBlockList.add(blockStore.get(key));
            }
        }

        return pbftBlockList;
    }

}
