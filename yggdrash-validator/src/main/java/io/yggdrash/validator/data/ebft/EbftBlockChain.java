package io.yggdrash.validator.data.ebft;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.Consensus;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.consensus.ConsensusBlockChain;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.validator.store.ebft.EbftBlockKeyStore;
import io.yggdrash.validator.store.ebft.EbftBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Deprecated
public class EbftBlockChain implements ConsensusBlockChain<EbftProto.EbftBlock, EbftBlock> {

    private static final Logger log = LoggerFactory.getLogger(EbftBlockChain.class);

    private final BranchId branchId;

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
                || !Arrays.equals(genesisBlock.getHeader().getPrevBlockHash(), Constants.EMPTY_HASH)) {
            log.error("GenesisBlock is not valid.");
            throw new NotValidateException();
        }

        this.branchId = BranchId.of(genesisBlock.getHeader().getChain());

        this.genesisBlock = new EbftBlock(genesisBlock);
        this.lastConfirmedBlock = this.genesisBlock;
        this.blockKeyStore = new EbftBlockKeyStore(
                new LevelDbDataSource(dbPath, blockKeyStorePath));
        this.blockStore = new EbftBlockStore(
                new LevelDbDataSource(dbPath, blockStorePath));

        EbftBlock ebftBlock = this.genesisBlock;
        if (this.blockKeyStore.size() > 0) {
            if (!Arrays.equals(this.blockKeyStore.get(0L), this.genesisBlock.getHash().getBytes())) {
                throw new NotValidateException("EbftBlockKeyStore is not valid.");
            }

            EbftBlock prevEbftBlock = this.genesisBlock;
            for (long l = 1; l < this.blockKeyStore.size(); l++) {
                ebftBlock = this.blockStore.get(Sha3Hash.createByHashed(blockKeyStore.get(l)));
                if (prevEbftBlock.getHash().equals(ebftBlock.getPrevBlockHash())) {
                    prevEbftBlock = ebftBlock;
                } else {
                    throw new NotValidateException("EbftBlockStore is not valid.");
                }
            }

            this.lastConfirmedBlock = ebftBlock;

        } else {
            this.blockKeyStore.put(0L, genesisBlock.getHash().getBytes());
            this.blockStore.put(genesisBlock.getHash(), this.genesisBlock);
        }

        this.transactionStore = new TransactionStore(
                new LevelDbDataSource(dbPath, txStorePath));

        this.consensus = new Consensus(this.genesisBlock.getBlock());
    }

    @Override
    public BranchId getBranchId() {
        return branchId;
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
    public ConsensusBlock<EbftProto.EbftBlock> addBlock(ConsensusBlock<EbftProto.EbftBlock> block) {
        this.lock.lock();
        try {
            if (block == null
                    || block.getIndex() != this.lastConfirmedBlock.getIndex() + 1
                    || !block.verify()) {
                log.debug("Block is not valid.");
                return null;
            }

            this.blockKeyStore.put(block.getIndex(), block.getHash().getBytes());
            this.blockStore.put(Sha3Hash.createByHashed(block.getHash().getBytes()), block);

            this.lastConfirmedBlock = (EbftBlock) block;
            loggingBlock(this.lastConfirmedBlock);
            batchTxs(this.lastConfirmedBlock);
        } finally {
            this.lock.unlock();
        }
        return block;
    }

    private void loggingBlock(EbftBlock block) {
        try {
            log.info("EbftBlock [" + block.getIndex() + "] "
                    + block.getHash()
                    + " ("
                    + block.getBlock().getAddress()
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
    public List<ConsensusBlock<EbftProto.EbftBlock>> getBlockList(long index, long count) {
        List<ConsensusBlock<EbftProto.EbftBlock>> blockList = new ArrayList<>();
        if (index < 0L || count < 1L || count > 100L) {
            log.debug("index or count is not valid");
            return blockList;
        }

        byte[] key;
        for (long l = index; l < index + count; l++) {
            key = blockKeyStore.get(l);
            if (key != null) {
                blockList.add(blockStore.get(Sha3Hash.createByHashed(key)));
            }
        }

        return blockList;
    }

    private void batchTxs(ConsensusBlock block) {
        if (block == null
                || block.getBlock() == null
                || block.getBlock().getBody().getLength() == 0) {
            return;
        }
        Set<Sha3Hash> keys = block.getBlock().getBody().getTransactionList().stream()
                .map(Transaction::getHash).collect(Collectors.toSet());

        transactionStore.batch(keys);
    }
}
