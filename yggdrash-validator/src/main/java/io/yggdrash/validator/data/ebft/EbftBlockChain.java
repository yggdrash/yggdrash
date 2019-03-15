package io.yggdrash.validator.data.ebft;

import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.validator.config.Consensus;
import io.yggdrash.validator.data.ConsensusBlockChain;
import io.yggdrash.validator.store.ebft.EbftBlockKeyStore;
import io.yggdrash.validator.store.ebft.EbftBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.yggdrash.common.config.Constants.EMPTY_BYTE32;

public class EbftBlockChain implements ConsensusBlockChain<String, EbftBlock> {

    private static final Logger log = LoggerFactory.getLogger(EbftBlockChain.class);

    private final byte[] chain;

    private final EbftBlockKeyStore blockKeyStore;
    private final EbftBlockStore blockStore;
    private final EbftBlock genesisBlock;
    private final Map<String, EbftBlock> unConfirmedBlockMap = new ConcurrentHashMap<>();
    private final TransactionStore transactionStore;

    private EbftBlock lastConfirmedBlock;

    private final Consensus consensus;

    @Autowired
    public EbftBlockChain(Block genesisBlock, String dbPath,
                          String blockKeyStorePath, String blockStorePath, String txStorePath) {
        if (genesisBlock.getHeader().getIndex() != 0
                || !Arrays.equals(genesisBlock.getHeader().getPrevBlockHash(), EMPTY_BYTE32)) {
            log.error("GenesisBlock is not valid.");
            throw new NotValidateException();
        }

        this.chain = genesisBlock.getHeader().getChain();

        this.genesisBlock = new EbftBlock(genesisBlock);
        this.lastConfirmedBlock = this.genesisBlock;
        this.blockKeyStore = new EbftBlockKeyStore(
                new LevelDbDataSource(dbPath, blockKeyStorePath));
        this.blockStore = new EbftBlockStore(
                new LevelDbDataSource(dbPath, blockStorePath));

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
                new LevelDbDataSource(dbPath, txStorePath));

        this.consensus = new Consensus(this.genesisBlock.getBlock());
    }

    public byte[] getChain() {
        return chain;
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

    public Map<String, EbftBlock> getUnConfirmedData() {
        return unConfirmedBlockMap;
    }

    @Override
    public Consensus getConsensus() {
        return consensus;
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
