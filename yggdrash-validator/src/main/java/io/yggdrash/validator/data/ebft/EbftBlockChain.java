package io.yggdrash.validator.data.ebft;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.validator.config.Consensus;
import io.yggdrash.validator.data.ConsensusBlock;
import io.yggdrash.validator.data.ConsensusBlockChain;
import io.yggdrash.validator.store.ebft.EbftBlockKeyStore;
import io.yggdrash.validator.store.ebft.EbftBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

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

    private final ReentrantLock lock = new ReentrantLock();

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

    @Override
    public byte[] getChain() {
        return chain;
    }

    @Override
    public Consensus getConsensus() {
        return consensus;
    }

    @Override
    public EbftBlockKeyStore getBlockKeyStore() {
        return blockKeyStore;
    }

    @Override
    public EbftBlockStore getBlockStore() {
        return blockStore;
    }

    @Override
    public TransactionStore getTransactionStore() {
        return transactionStore;
    }

    @Override
    public EbftBlock getGenesisBlock() {
        return genesisBlock;
    }

    @Override
    public EbftBlock getLastConfirmedBlock() {
        return lastConfirmedBlock;
    }

    @Override
    public Map<String, EbftBlock> getUnConfirmedData() {
        return unConfirmedBlockMap;
    }

    @Override
    public void addBlock(ConsensusBlock block) {
        this.lock.lock();
        try {
            if (block == null
                    || block.getIndex() != this.lastConfirmedBlock.getIndex() + 1
                    || !block.verify()) {
                log.debug("Block is not valid.");
                return;
            }

            this.blockKeyStore.put(block.getIndex(), block.getHash());
            this.blockStore.put(block.getHash(), (EbftBlock) block);

            this.lastConfirmedBlock = (EbftBlock) block.clone();
            loggingBlock(this.lastConfirmedBlock);
            batchTxs(this.lastConfirmedBlock);
        } finally {
            this.lock.unlock();
        }
    }

    private void loggingBlock(EbftBlock block) {
        try {
            log.debug("EbftBlock [" + block.getIndex() + "] "
                    + block.getHashHex()
                    + " ("
                    + block.getBlock().getAddressHex()
                    + ") "
                    + "("
                    + block.getConsensusMessages().size()
                    + ")");
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }

    /**
     * Get BlockList from BlockStore with index, count.
     *
     * @param index index of block (0 <= index)
     * @param count count of blocks (1 < count <= 100)
     * @return list of Block
     */
    @Override
    public List<ConsensusBlock> getBlockList(long index, long count) {
        if (index < 0L || count < 1L || count > 100L) {
            log.debug("index or count is not valid");
            return null;
        }

        byte[] key;
        List<ConsensusBlock> blockList = new ArrayList<>();
        for (long l = index; l < index + count; l++) {
            key = blockKeyStore.get(l);
            if (key != null) {
                blockList.add(blockStore.get(key));
            }
        }

        return blockList;
    }

    private void batchTxs(ConsensusBlock block) {
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
