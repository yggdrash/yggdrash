package io.yggdrash.validator.data.ebft;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.common.util.VerifierUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.consensus.Consensus;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.consensus.ConsensusBlockChain;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.BlockChainStore;
import io.yggdrash.core.store.BlockChainStoreBuilder;
import io.yggdrash.core.store.BlockStoreFactory;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.validator.data.BlockChainManagerMock;
import io.yggdrash.validator.store.ebft.EbftBlockKeyStore;
import io.yggdrash.validator.store.ebft.EbftBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class EbftBlockChain implements ConsensusBlockChain<EbftProto.EbftBlock, EbftBlock> {

    private static final Logger log = LoggerFactory.getLogger(EbftBlockChain.class);

    private final BranchId branchId;
    private final EbftBlockKeyStore blockKeyStore;
    private final Map<String, EbftBlock> unConfirmedBlockMap = new ConcurrentHashMap<>();
    private final BlockChainManagerMock<EbftProto.EbftBlock> blockChainManagerMock;
    private final EbftBlock genesisBlock;
    private final Consensus consensus;

    private final ReentrantLock lock = new ReentrantLock();

    public EbftBlockChain(Block genesisBlock,
                          String dbPath,
                          String blockKeyStorePath,
                          String blockStorePath
                          ) {


        String ebftBlockChainPath = String.format("%s%s", dbPath, blockStorePath);

        BlockStoreFactory storeFactory = (consensusAlgorithm, dbSource) -> new EbftBlockStore(dbSource);
        BlockChainStore store = BlockChainStoreBuilder.newBuilder(BranchId.NULL)
                .withProductionMode(true)
                .withDataBasePath(ebftBlockChainPath)
                .setBlockStoreFactory(storeFactory)
                .build();

        this.blockChainManagerMock = new BlockChainManagerMock<>(store);

        if (!VerifierUtils.verifyGenesisHash(genesisBlock)) {
            log.error("GenesisBlock is not valid.");
            throw new NotValidateException();
        }

        this.branchId = BranchId.of(genesisBlock.getHeader().getChain());
        this.genesisBlock = new EbftBlock(genesisBlock);
        this.consensus = new Consensus(this.genesisBlock.getBlock());
        this.blockKeyStore = new EbftBlockKeyStore(new LevelDbDataSource(dbPath, blockKeyStorePath));

        blockChainManagerMock.setLastConfirmedBlock(this.genesisBlock);
        EbftBlock ebftBlock = this.genesisBlock;

        if (this.blockKeyStore.size() > 0) {
            if (!Arrays.equals(this.blockKeyStore.get(0L), this.genesisBlock.getHash().getBytes())) {
                throw new NotValidateException("EbftBlockKeyStore is not valid.");
            }

            EbftBlock prevEbftBlock = this.genesisBlock;
            for (long l = 1; l < this.blockKeyStore.size(); l++) {
                Sha3Hash hash = Sha3Hash.createByHashed(blockKeyStore.get(l));
                ebftBlock = (EbftBlock) blockChainManagerMock.getBlockByHash(hash);
                if (prevEbftBlock.getHash().equals(ebftBlock.getPrevBlockHash())) {
                    prevEbftBlock = ebftBlock;
                } else {
                    throw new NotValidateException("EbftBlockStore is not valid.");
                }
            }

            blockChainManagerMock.setLastConfirmedBlock(ebftBlock);

        } else {
            this.blockKeyStore.put(0L, genesisBlock.getHash().getBytes());
            // BlockChainManager add block to the blockStore, set the lastConfirmedBlock, and then batch the txs.
            blockChainManagerMock.addBlock(this.genesisBlock); // todo: check efficiency & change index
        }
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
    public BlockChainManager<EbftProto.EbftBlock> getBlockChainManager() {
        return blockChainManagerMock;
    }

    @Override
    public EbftBlock getGenesisBlock() {
        return genesisBlock;
    }

    @Override
    public Map<String, EbftBlock> getUnConfirmedData() {
        return unConfirmedBlockMap;
    }

    @Override
    public ConsensusBlock<EbftProto.EbftBlock> addBlock(ConsensusBlock<EbftProto.EbftBlock> block) {
        this.lock.lock();
        try {
            blockChainManagerMock.addBlock(block); // todo: check efficiency & change index
            this.blockKeyStore.put(block.getIndex(), block.getHash().getBytes());
            loggingBlock((EbftBlock) block);
        } finally {
            this.lock.unlock();
        }
        return block;
    }

    @Override
    public boolean isValidator(String addr) {
        return true;
    }

    @Override
    public ValidatorSet getValidators() {
        return null;
    }

    private void loggingBlock(EbftBlock block) {
        try {
            log.info("EbftBlock [{}] ({}) ({})({})",
                    block.getIndex(),
                    block.getHash(),
                    block.getBlock().getAddress(),
                    block.getConsensusMessages().size()
            );
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }
}
