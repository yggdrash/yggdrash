package io.yggdrash.validator.data.pbft;

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
import io.yggdrash.proto.PbftProto;
import io.yggdrash.validator.data.BlockChainManagerMock;
import io.yggdrash.validator.store.ebft.EbftBlockStore;
import io.yggdrash.validator.store.pbft.PbftBlockKeyStore;
import io.yggdrash.validator.store.pbft.PbftBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PbftBlockChain implements ConsensusBlockChain<PbftProto.PbftBlock, PbftMessage> {

    private static final Logger log = LoggerFactory.getLogger(PbftBlockChain.class);

    private final BranchId branchId;
    private final PbftBlockKeyStore blockKeyStore;
    private final Map<String, PbftMessage> unConfirmedMsgMap = new ConcurrentHashMap<>();

    private final BlockChainManagerMock<PbftProto.PbftBlock> blockChainManagerMock;
    private final PbftBlock genesisBlock;
    private final Consensus consensus;

    public PbftBlockChain(Block genesisBlock, String dbPath,
                          String blockKeyStorePath, String blockStorePath) {

        String pbftBlockChainPath = String.format("%s%s", dbPath, blockStorePath);
        BlockStoreFactory storeFactory = (consensusAlgorithm, dbSource) -> new PbftBlockStore(dbSource);
        BlockChainStore store = BlockChainStoreBuilder.newBuilder(BranchId.NULL)
                .withProductionMode(true)
                .withDataBasePath(pbftBlockChainPath)
                .setBlockStoreFactory(storeFactory)
                .build();


        this.blockChainManagerMock = new BlockChainManagerMock<>(store);

        if (!VerifierUtils.verifyGenesisHash(genesisBlock)) {
            log.error("GenesisBlock is not valid.");
            throw new NotValidateException();
        }

        this.branchId = BranchId.of(genesisBlock.getHeader().getChain());
        this.genesisBlock = new PbftBlock(genesisBlock, PbftMessageSet.forGenesis());
        this.blockKeyStore = new PbftBlockKeyStore(new LevelDbDataSource(dbPath, blockKeyStorePath));

        if (this.blockKeyStore.size() == 0) {
            this.blockKeyStore.put(0L, this.genesisBlock.getHash().getBytes());
            blockChainManagerMock.addBlock(this.genesisBlock); // todo: check efficiency & change index
        } else {
            if (!Arrays.equals(this.blockKeyStore.get(0L), this.genesisBlock.getHash().getBytes())) {
                throw new NotValidateException("PbftBlockKeyStore is not valid.");
            }

            PbftBlock prevPbftBlock = (PbftBlock) blockChainManagerMock.getBlockByHash(
                    Sha3Hash.createByHashed(blockKeyStore.get(0L)));
            PbftBlock nextPbftBlock = null;
            for (long l = 1; l < this.blockKeyStore.size(); l++) {
                nextPbftBlock = (PbftBlock) blockChainManagerMock.getBlockByHash(
                        Sha3Hash.createByHashed(blockKeyStore.get(l)));
                if (prevPbftBlock.getHash().equals(nextPbftBlock.getPrevBlockHash())) {
                    prevPbftBlock.clear();
                    prevPbftBlock = nextPbftBlock;
                } else {
                    throw new NotValidateException("PbftBlockStore is not valid.");
                }
            }

            if (nextPbftBlock != null) {
                blockChainManagerMock.setLastConfirmedBlock(nextPbftBlock);
            }
        }

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
    public BlockChainManager<PbftProto.PbftBlock> getBlockChainManager() {
        return blockChainManagerMock;
    }

    @Override
    public PbftBlock getGenesisBlock() {
        return genesisBlock;
    }

    @Override
    public Map<String, PbftMessage> getUnConfirmedData() {
        return unConfirmedMsgMap;
    }

    @Override
    public ConsensusBlock<PbftProto.PbftBlock> addBlock(ConsensusBlock<PbftProto.PbftBlock> block) {
        blockChainManagerMock.addBlock(block); // todo: check efficiency & change index
        this.blockKeyStore.put(block.getIndex(), block.getHash().getBytes());
        loggingBlock((PbftBlock) block);
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

    private void loggingBlock(PbftBlock block) {
        try {
            log.info("PbftBlock ({}) [{}] ({}) ({}) ({}) ({}) ({})",
                    block.getConsensusMessages().getPrePrepare().getViewNumber(),
                    block.getIndex(),
                    block.getHash(),
                    block.getConsensusMessages().getPrepareMap().size(),
                    block.getConsensusMessages().getCommitMap().size(),
                    block.getConsensusMessages().getViewChangeMap().size(),
                    block.getBlock().getAddress()
            );
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }
}
