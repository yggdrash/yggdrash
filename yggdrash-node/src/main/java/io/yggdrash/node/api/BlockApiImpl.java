package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.gateway.dto.BlockDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AutoJsonRpcServiceImpl
public class BlockApiImpl implements BlockApi {

    private final BranchGroup branchGroup;

    @Autowired
    public BlockApiImpl(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @Override
    public long blockNumber(String branchId) {
        return branchGroup.getLastIndex(BranchId.of(branchId));
    }

    @Override
    public BlockDto getBlockByHash(String branchId, String blockId, Boolean bool) {
        ConsensusBlock block = branchGroup.getBlockByHash(BranchId.of(branchId), blockId);
        return BlockDto.createBy(block);
    }

    @Override
    public BlockDto getBlockByNumber(String branchId, long numOfBlock, Boolean bool) {
        ConsensusBlock block = branchGroup.getBlockByIndex(BranchId.of(branchId), numOfBlock);
        return BlockDto.createBy(block);
    }

    @Override
    public BlockDto getLastBlock(String branchId) {
        ConsensusBlock block = branchGroup.getBlockByIndex(BranchId.of(branchId), branchGroup.getLastIndex(BranchId.of(branchId)));
        return BlockDto.createBy(block);
    }

}
