package io.yggdrash.core.blockchain;

import io.yggdrash.core.blockchain.osgi.ContractManager;
import io.yggdrash.core.store.BranchStore;
import io.yggdrash.proto.PbftProto;

public class PbftBlockChainMock extends BlockChainImpl<PbftProto.PbftBlock, PbftBlockMock> {

    public PbftBlockChainMock(Branch branch,
                              Block genesisBlock,
                              BranchStore branchStore,
                              BlockChainManager<PbftProto.PbftBlock> blockChainManager,
                              ContractManager contractManager) {
        super(branch, new PbftBlockMock(genesisBlock), branchStore, blockChainManager, contractManager);
    }
}
