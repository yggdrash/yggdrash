package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
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
    public long blockNumber() {
        try {
            return branchGroup.getLastIndex() + 1;
        } catch (Exception exception) {
            throw new InternalErrorException();
        }
    }

    @Override
    public BlockHusk getBlockByHash(String hashOfBlock, Boolean bool) {
        try {
            return branchGroup.getBlockByHash(hashOfBlock);
        } catch (Exception exception) {
            throw new NonExistObjectException("block");
        }
    }

    @Override
    public BlockHusk getBlockByNumber(long numOfBlock, Boolean bool) {
        try {
            return branchGroup.getBlockByIndex(numOfBlock);
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
    public BlockHusk getLastBlock() {
        return branchGroup.getBlockByHash(String.valueOf(branchGroup.getLastIndex()));
    }
}
