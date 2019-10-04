package io.yggdrash.core.consensus;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.store.BlockKeyStore;

import java.util.List;
import java.util.Map;

public interface ConsensusBlockChain<T, V> {
    BranchId getBranchId();

    Consensus getConsensus();

    BlockKeyStore getBlockKeyStore();

    ConsensusBlock<T> getGenesisBlock();

    Map<String, V> getUnConfirmedData();

    Map<String, List<String>> addBlock(ConsensusBlock<T> block); // return errorLogs

    Map<String, List<String>> addBlock(ConsensusBlock<T> block, boolean broadcast); // return errorLogs

    Sha3Hash executeAndAddToPendingPool(Transaction tx); // return stateRootHash

    boolean isValidator(String addr);

    ValidatorSet getValidators();

    BlockChainManager<T> getBlockChainManager();
}
