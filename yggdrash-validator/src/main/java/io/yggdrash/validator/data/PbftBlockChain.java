package io.yggdrash.validator.data;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
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

    private boolean isProposed;
    private boolean isConsensused;

    private final byte[] chain;
    private final String host;
    private final int port;

    private final PbftBlockKeyStore pbftBlockKeyStore;
    private final PbftBlockStore pbftBlockStore;
    private final PbftBlock rootPbftBlock;
    private PbftBlock lastConfirmedBlock;
    private final Map<String, PbftBlock> unConfirmedPbftBlockMap = new ConcurrentHashMap<>();

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

        this.rootPbftBlock = new PbftBlock(0, this.chain, genesisBlock);
        this.lastConfirmedBlock = rootPbftBlock;
        this.pbftBlockKeyStore = new PbftBlockKeyStore(
                new LevelDbDataSource(defaultConfig.getDatabasePath(),
                        this.host + "_" + this.port + "/" + Hex.toHexString(this.chain)
                                + "/pbftblockkey"));
        this.pbftBlockStore = new PbftBlockStore(
                new LevelDbDataSource(defaultConfig.getDatabasePath(),
                        this.host + "_" + this.port + "/" + Hex.toHexString(this.chain)
                                + "/pbftblock"));

        PbftBlock pbftBlock = rootPbftBlock;
        if (this.pbftBlockKeyStore.size() > 0) {
            if (!Arrays.equals(this.pbftBlockKeyStore.get(0L), rootPbftBlock.getHash())) {
                log.error("PbftBlockKeyStore is not valid.");
                throw new NotValidateException();
            }

            PbftBlock prevPbftBlock = rootPbftBlock;
            for (long l = 1; l < this.pbftBlockKeyStore.size(); l++) {
                pbftBlock = this.pbftBlockStore.get(this.pbftBlockKeyStore.get(l));
                if (Arrays.equals(prevPbftBlock.getHash(), pbftBlock.getPrevBlockHash())) {
                    prevPbftBlock = pbftBlock;
                } else {
                    log.error("PbftBlockChain() bpbftBlockStore is not valid.");
                    throw new NotValidateException();
                }
            }

            this.lastConfirmedBlock = pbftBlock;

        } else {
            this.pbftBlockKeyStore.put(0L, rootPbftBlock.getHash());
            this.pbftBlockStore.put(rootPbftBlock.getHash(), rootPbftBlock);
        }

        this.transactionStore = new TransactionStore(
                new LevelDbDataSource(defaultConfig.getDatabasePath(),
                        this.host + "_" + this.port + "/" + Hex.toHexString(this.chain)
                                + "/txs"));

    }

    public List<PbftBlock> getPbftBlockList(long index, long count) {
        if (index < 0L || count < 1L || count > 100L) {
            log.debug("getPbftBlockList() index or count is not valid");
            return null;
        }

        byte[] key;
        List<PbftBlock> pbftBlockList = new ArrayList<>();
        for (long l = index; l < index + count; l++) {
            key = pbftBlockKeyStore.get(l);
            if (key != null) {
                pbftBlockList.add(pbftBlockStore.get(key));
            }
        }

        return pbftBlockList;
    }

}
