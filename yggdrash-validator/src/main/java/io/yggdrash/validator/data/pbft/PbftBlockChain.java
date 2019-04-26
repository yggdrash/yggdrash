package io.yggdrash.validator.data.pbft;

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
import io.yggdrash.proto.PbftProto;
import io.yggdrash.validator.store.pbft.PbftBlockKeyStore;
import io.yggdrash.validator.store.pbft.PbftBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class PbftBlockChain implements ConsensusBlockChain<PbftProto.PbftBlock, PbftMessage> {

    private static final Logger log = LoggerFactory.getLogger(PbftBlockChain.class);

    private final BranchId branchId;

    private final PbftBlockKeyStore blockKeyStore;
    private final PbftBlockStore blockStore;
    private final PbftBlock genesisBlock;
    private final Map<String, PbftMessage> unConfirmedMsgMap = new ConcurrentHashMap<>();
    private final TransactionStore transactionStore;

    private PbftBlock lastConfirmedBlock;

    private final Consensus consensus;

    private final ReentrantLock lock = new ReentrantLock();

    public PbftBlockChain(Block genesisBlock, String dbPath,
                          String blockKeyStorePath, String blockStorePath, String txStorePath) {
        if (genesisBlock.getHeader().getIndex() != 0
                || !Arrays.equals(genesisBlock.getHeader().getPrevBlockHash(), Constants.EMPTY_HASH)) {
            log.error("GenesisBlock is not valid.");
            throw new NotValidateException();
        }

        this.branchId = BranchId.of(genesisBlock.getHeader().getChain());

        this.genesisBlock = new PbftBlock(genesisBlock, PbftMessageSet.forGenesis());
        this.lastConfirmedBlock = this.genesisBlock;
        this.blockKeyStore = new PbftBlockKeyStore(
                new LevelDbDataSource(dbPath, blockKeyStorePath));
        this.blockStore = new PbftBlockStore(
                new LevelDbDataSource(dbPath, blockStorePath));

        if (this.blockKeyStore.size() == 0) {
            this.blockKeyStore.put(0L, this.genesisBlock.getHash().getBytes());
            this.blockStore.addBlock(this.genesisBlock); // todo: check efficiency & change index
            //this.blockStore.put(genesisBlock.getHash(), this.genesisBlock);
        } else {
            if (!Arrays.equals(this.blockKeyStore.get(0L), this.genesisBlock.getHash().getBytes())) {
                throw new NotValidateException("PbftBlockKeyStore is not valid.");
            }

            PbftBlock prevPbftBlock = this.blockStore.get(Sha3Hash.createByHashed(blockKeyStore.get(0L)));
            PbftBlock nextPbftBlock = null;
            for (long l = 1; l < this.blockKeyStore.size(); l++) {
                nextPbftBlock = this.blockStore.get(Sha3Hash.createByHashed(blockKeyStore.get(l)));
                if (prevPbftBlock.getHash().equals(nextPbftBlock.getPrevBlockHash())) {
                    prevPbftBlock.clear();
                    prevPbftBlock = nextPbftBlock;
                } else {
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

    @Override
    public BranchId getBranchId() {
        return branchId;
    }

    @Override
    public Consensus getConsensus() {
        return consensus;
    }

    @Override
    public PbftBlockKeyStore getBlockKeyStore() {
        return blockKeyStore;
    }

    @Override
    public PbftBlockStore getBlockStore() {
        return blockStore;
    }

    @Override
    public TransactionStore getTransactionStore() {
        return transactionStore;
    }

    @Override
    public PbftBlock getGenesisBlock() {
        return genesisBlock;
    }

    @Override
    public PbftBlock getLastConfirmedBlock() {
        return lastConfirmedBlock;
    }

    @Override
    public Map<String, PbftMessage> getUnConfirmedData() {
        return unConfirmedMsgMap;
    }

    @Override
    public ConsensusBlock<PbftProto.PbftBlock> addBlock(ConsensusBlock<PbftProto.PbftBlock> block) {
        this.lock.lock();
        try {
            if (block == null
                    || block.getIndex() != this.lastConfirmedBlock.getIndex() + 1
                    || !block.verify()) {
                log.debug("Block is not valid.");
                return null;
            }

            this.blockKeyStore.put(block.getIndex(), block.getHash().getBytes());
            this.blockStore.addBlock(block); // todo: check efficiency & change index
            // this.blockStore.put(block.getHash(), block);

            this.lastConfirmedBlock = (PbftBlock) block;
            loggingBlock(this.lastConfirmedBlock);
            batchTxs(this.lastConfirmedBlock);
        } finally {
            this.lock.unlock();
        }
        return block;
    }

    private void loggingBlock(PbftBlock block) {
        try {
            log.info("PbftBlock "
                    + "("
                    + block.getConsensusMessages().getPrePrepare().getViewNumber()
                    + ") "
                    + "["
                    + block.getIndex()
                    + "]"
                    + block.getHash()
                    + " ("
                    + block.getConsensusMessages().getPrepareMap().size()
                    + ")"
                    + " ("
                    + block.getConsensusMessages().getCommitMap().size()
                    + ")"
                    + " ("
                    + block.getConsensusMessages().getViewChangeMap().size()
                    + ")"
                    + " ("
                    + block.getBlock().getAddress()
                    + ")");
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
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
