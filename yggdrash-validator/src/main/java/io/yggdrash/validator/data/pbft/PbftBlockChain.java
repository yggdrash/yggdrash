package io.yggdrash.validator.data.pbft;

import io.yggdrash.common.utils.Sha3Hash;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.validator.config.Consensus;
import io.yggdrash.validator.data.ConsensusBlockChain;
import io.yggdrash.validator.store.pbft.PbftBlockKeyStore;
import io.yggdrash.validator.store.pbft.PbftBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.yggdrash.common.config.Constants.EMPTY_BYTE32;

public class PbftBlockChain implements ConsensusBlockChain<String, PbftMessage> {

    private static final Logger log = LoggerFactory.getLogger(PbftBlockChain.class);

    private final byte[] chain;

    private final PbftBlockKeyStore blockKeyStore;
    private final PbftBlockStore blockStore;
    private final PbftBlock genesisBlock;
    private final Map<String, PbftMessage> unConfirmedMsgMap = new ConcurrentHashMap<>();
    private final TransactionStore transactionStore;

    private PbftBlock lastConfirmedBlock;

    private final Consensus consensus;

    public PbftBlockChain(Block genesisBlock, String dbPath,
                          String blockKeyStorePath, String blockStorePath, String txStorePath) {
        if (genesisBlock.getHeader().getIndex() != 0
                || !Arrays.equals(genesisBlock.getHeader().getPrevBlockHash(), EMPTY_BYTE32)) {
            log.error("GenesisBlock is not valid.");
            throw new NotValidateException();
        }

        this.chain = genesisBlock.getHeader().getChain();

        this.genesisBlock = new PbftBlock(genesisBlock, null);
        this.lastConfirmedBlock = this.genesisBlock;
        this.blockKeyStore = new PbftBlockKeyStore(
                new LevelDbDataSource(dbPath, blockKeyStorePath));
        this.blockStore = new PbftBlockStore(
                new LevelDbDataSource(dbPath, blockStorePath));

        if (this.blockKeyStore.size() == 0) {
            this.blockKeyStore.put(0L, this.genesisBlock.getHash());
            this.blockStore.put(this.genesisBlock.getHash(), this.genesisBlock);

        } else {
            if (!Arrays.equals(this.blockKeyStore.get(0L), this.genesisBlock.getHash())) {
                log.error("PbftBlockKeyStore is not valid.");
                throw new NotValidateException();
            }

            PbftBlock prevPbftBlock = this.blockStore.get(this.blockKeyStore.get(0L));
            PbftBlock nextPbftBlock = null;
            for (long l = 1; l < this.blockKeyStore.size(); l++) {
                nextPbftBlock = this.blockStore.get(this.blockKeyStore.get(l));
                if (Arrays.equals(prevPbftBlock.getHash(), nextPbftBlock.getPrevBlockHash())) {
                    prevPbftBlock.clear();
                    prevPbftBlock = nextPbftBlock;
                } else {
                    log.error("PbftBlockStore is not valid.");
                    throw new NotValidateException("PbftBlockStore is not valid.");
                }
            }

            if (nextPbftBlock != null) {
                this.lastConfirmedBlock = nextPbftBlock;
            }
        }

        this.transactionStore = new TransactionStore(
                new LevelDbDataSource(dbPath, txStorePath));

        this.consensus = new Consensus(this.genesisBlock.getBlock());
    }

    public byte[] getChain() {
        return chain;
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

    public Map<String, PbftMessage> getUnConfirmedData() {
        return unConfirmedMsgMap;
    }

    public TransactionStore getTransactionStore() {
        return transactionStore;
    }

    public Consensus getConsensus() {
        return consensus;
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

    public void batchTxs(PbftBlock block) {
        if (block == null
                || block.getBlock() == null
                || block.getBlock().getBody().length() == 0) {
            return;
        }
        Set<Sha3Hash> keys = new HashSet<>();

        for (Transaction tx : block.getBlock().getBody().getBody()) {
            keys.add(new Sha3Hash(tx.getHash(), true));
        }
        transactionStore.batch(keys);
    }
}
