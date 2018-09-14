package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.exception.InternalErrorException;
import io.yggdrash.core.exception.NonExistObjectException;
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
        try {
            return branchGroup.getLastIndex(BranchId.of(branchId)) + 1;
        } catch (Exception exception) {
            throw new InternalErrorException();
        }
    }

    @Override
    public BlockHusk getBlockByHash(String branchId, String hashOfBlock, Boolean bool) {
        try {
            return branchGroup.getBlockByHash(BranchId.of(branchId), hashOfBlock);
        } catch (Exception exception) {
            throw new NonExistObjectException("block");
        }
    }

    @Override
    public BlockHusk getBlockByNumber(String branchId, long numOfBlock, Boolean bool) {
        try {
            return branchGroup.getBlockByIndex(BranchId.of(branchId), numOfBlock);
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
    public BlockHusk getLastBlock(String branchId) {
        BranchId id = BranchId.of(branchId);
        return branchGroup.getBlockByIndex(id, branchGroup.getLastIndex(id));
    }
}
