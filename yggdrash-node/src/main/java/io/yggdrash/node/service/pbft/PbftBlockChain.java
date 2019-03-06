package io.yggdrash.node.service.pbft;

import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.pbft.PbftBlock;
import io.yggdrash.core.blockchain.pbft.PbftMessage;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.store.pbft.PbftBlockKeyStore;
import io.yggdrash.core.store.pbft.PbftBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.yggdrash.common.config.Constants.DEFAULT_PORT;
import static io.yggdrash.common.config.Constants.EMPTY_BYTE32;

@Profile("validator")
public class PbftBlockChain {

    private static final Logger log = LoggerFactory.getLogger(PbftBlockChain.class);

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
    public PbftBlockChain(Block genesisBlock, String dbPath,
                          String blockKeyStorePath, String blockStorePath, String txStorePath) {
        if (genesisBlock.getHeader().getIndex() != 0
                || !Arrays.equals(genesisBlock.getHeader().getPrevBlockHash(), EMPTY_BYTE32)) {
            log.error("GenesisBlock is not valid.");
            throw new NotValidateException();
        }

        this.chain = genesisBlock.getHeader().getChain();
        this.host = InetAddress.getLoopbackAddress().getHostAddress();
        if (System.getProperty("grpc.port") == null) {
            this.port = DEFAULT_PORT;
        } else {
            this.port = Integer.parseInt(System.getProperty("grpc.port"));
        }

        this.genesisBlock = new PbftBlock(genesisBlock, null);
        this.lastConfirmedBlock = this.genesisBlock;
        this.blockKeyStore = new PbftBlockKeyStore(
                new LevelDbDataSource(dbPath, blockKeyStorePath));
        this.blockStore = new PbftBlockStore(
                new LevelDbDataSource(dbPath, blockStorePath));

        PbftBlock pbftBlock = this.genesisBlock;
        if (this.blockKeyStore.size() == 0) {
            this.blockKeyStore.put(0L, this.genesisBlock.getHash());
            this.blockStore.put(this.genesisBlock.getHash(), this.genesisBlock);

        } else {
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
                    throw new NotValidateException("PbftBlockStore is not valid.");
                }
            }

            this.lastConfirmedBlock = pbftBlock;

        }

        this.transactionStore = new TransactionStore(
                new LevelDbDataSource(dbPath, txStorePath));
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
