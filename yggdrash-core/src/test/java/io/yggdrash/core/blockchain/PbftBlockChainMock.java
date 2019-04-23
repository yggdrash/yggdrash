package io.yggdrash.core.blockchain;

import io.yggdrash.common.store.StateStore;
import io.yggdrash.contract.core.store.OutputStore;
import io.yggdrash.contract.core.store.OutputType;
import io.yggdrash.core.blockchain.osgi.ContractContainer;
import io.yggdrash.core.store.BranchStore;
import io.yggdrash.core.store.ConsensusBlockStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.proto.PbftProto;

import java.util.Map;

public class PbftBlockChainMock extends BlockChainImpl<PbftProto.PbftBlock, PbftBlockMock> {

    public PbftBlockChainMock(Branch branch,
                              Block genesisBlock,
                              ConsensusBlockStore<PbftProto.PbftBlock> blockStore,
                              TransactionStore transactionStore,
                              BranchStore branchStore,
                              StateStore stateStore,
                              TransactionReceiptStore transactionReceiptStore,
                              ContractContainer contractContainer,
                              Map<OutputType, OutputStore> outputStores) {
        super(branch, new PbftBlockMock(genesisBlock), blockStore, transactionStore, branchStore, stateStore,
                transactionReceiptStore, contractContainer, outputStores);
    }
}
