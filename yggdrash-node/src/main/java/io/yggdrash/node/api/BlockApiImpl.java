package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.exception.InternalErrorException;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.node.controller.BlockDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AutoJsonRpcServiceImpl
public class BlockApiImpl implements BlockApi {

    private final NodeManager nodeManager;

    @Autowired
    public BlockApiImpl(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    @Override
    public int blockNumber() {
        try {
            return nodeManager.getBlocks().size();
        } catch (Exception exception) {
            throw new InternalErrorException();
        }
    }

    @Override
    public Set<BlockHusk> getAllBlock() {
        return nodeManager.getBlocks();
    }

    @Override
    public BlockHusk getBlockByHash(String hashOfBlock, Boolean bool) {
        try {
            return nodeManager.getBlockByIndexOrHash(hashOfBlock);
        } catch (Exception exception) {
            throw new NonExistObjectException("block");
        }
    }

    @Override
    public BlockHusk getBlockByNumber(String numOfBlock, Boolean bool) {
        try {
            return nodeManager.getBlockByIndexOrHash(numOfBlock);
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
        return nodeManager.getBlocks().stream().sorted(Comparator.reverseOrder())
                .collect(Collectors.toList()).get(0);
    }
}
