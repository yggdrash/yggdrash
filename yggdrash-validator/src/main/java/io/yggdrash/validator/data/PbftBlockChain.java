package io.yggdrash.validator.data;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import io.yggdrash.validator.data.pbft.PbftMessage;
import io.yggdrash.validator.store.PbftBlockKeyStore;
import io.yggdrash.validator.store.PbftBlockStore;
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

public class PbftBlockChain {

    private static final Logger log = LoggerFactory.getLogger(PbftBlockChain.class);

    private final byte[] chain;
    private final String host;
    private final int port;

    private final PbftBlockKeyStore blockKeyStore;
    private final PbftBlockStore blockStore;

    private final PbftBlock rootBlock;
    private PbftBlock lastConfirmedBlock;
    private final Map<String, PbftMessage> unConfirmedMsgMap = new ConcurrentHashMap<>();

    private final TransactionStore transactionStore;

    @Autowired
    public PbftBlockChain(Block genesisBlock, DefaultConfig defaultConfig) {
        if (genesisBlock.getHeader().getIndex() != 0
                || !Arrays.equals(genesisBlock.getHeader().getPrevBlockHash(), new byte[32])) {
            log.error("PbftBlockChain() genesisBlock is not valid.");
            throw new NotValidateException();
        }

        this.chain = genesisBlock.getHeader().getChain();
        this.host = InetAddress.getLoopbackAddress().getHostAddress();
        this.port = Integer.parseInt(System.getProperty("grpc.port"));

        this.rootBlock = new PbftBlock(genesisBlock, null);
        this.lastConfirmedBlock = rootBlock;
        this.blockKeyStore = new PbftBlockKeyStore(
                new LevelDbDataSource(defaultConfig.getDatabasePath(),
                        this.host + "_" + this.port + "/" + Hex.toHexString(this.chain)
                                + "/pbftblockkey"));
        this.blockStore = new PbftBlockStore(
                new LevelDbDataSource(defaultConfig.getDatabasePath(),
                        this.host + "_" + this.port + "/" + Hex.toHexString(this.chain)
                                + "/pbftblock"));

        PbftBlock pbftBlock = rootBlock;
        if (this.blockKeyStore.size() > 0) {
            if (!Arrays.equals(this.blockKeyStore.get(0L), rootBlock.getHash())) {
                log.error("PbftBlockKeyStore is not valid.");
                throw new NotValidateException();
            }

            PbftBlock prevPbftBlock = rootBlock;
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
            this.blockKeyStore.put(0L, rootBlock.getHash());
            this.blockStore.put(rootBlock.getHash(), rootBlock);
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

    public PbftBlockKeyStore getBlockKeyStore() {
        return blockKeyStore;
    }

    public PbftBlockStore getBlockStore() {
        return blockStore;
    }

    public PbftBlock getRootBlock() {
        return rootBlock;
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
