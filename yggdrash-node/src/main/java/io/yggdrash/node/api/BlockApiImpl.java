package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.exception.InternalErrorException;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.node.controller.BlockDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AutoJsonRpcServiceImpl
public class BlockApiImpl implements BlockApi {

    private static final Logger log = LoggerFactory.getLogger(BlockApiImpl.class);
    private final BranchGroup branchGroup;

    @Autowired
    public BlockApiImpl(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @Override
    public long blockNumber(String branchId) {
        try {
            return branchGroup.getLastIndex(BranchId.of(branchId)) + 1;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new InternalErrorException();
        }
    }

    @Override
    public BlockDto getBlockByHash(String branchId, String hashOfBlock, Boolean bool) {
        try {
            BlockHusk blockHusk = branchGroup.getBlockByHash(BranchId.of(branchId), hashOfBlock);
            return BlockDto.createBy(blockHusk);
        } catch (Exception exception) {
            throw new NonExistObjectException("block");
        }
    }

    @Override
    public BlockDto getBlockByNumber(String branchId, long numOfBlock, Boolean bool) {
        try {
            BlockHusk blockHusk = branchGroup.getBlockByIndex(BranchId.of(branchId), numOfBlock);
            return BlockDto.createBy(blockHusk);
        } catch (Exception exception) {
            throw new NonExistObjectException("block");
        }
    }

    @Override
    public int newBlockFilter() {
        try {
            return 0;
        } catch (Exception exception) {
            throw new InternalErrorException();
        }
    }

    @Override
    public BlockDto getLastBlock(String branchId) {
        BranchId id = BranchId.of(branchId);
        BlockHusk blockHusk = branchGroup.getBlockByIndex(id, branchGroup.getLastIndex(id));
        return BlockDto.createBy(blockHusk);
    }
}
