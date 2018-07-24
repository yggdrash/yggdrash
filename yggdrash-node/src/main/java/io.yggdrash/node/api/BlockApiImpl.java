package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.NodeManager;
import io.yggdrash.node.exception.InternalErrorException;
import io.yggdrash.node.exception.NonExistObjectException;
import io.yggdrash.node.mock.BlockMock;
import org.springframework.stereotype.Service;

@Service
@AutoJsonRpcServiceImpl
public class BlockApiImpl implements BlockApi {

    private final NodeManager nodeManager;

    public BlockApiImpl(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    @Override
    public int blockNumber() {
        try {
            return 0;
        } catch (Exception exception) {
            throw new InternalErrorException();
        }
    }

    @Override
    public String getBlockByHash(String address, String tag) {
        try {
            //todo: getBlockByNumber
            BlockMock blockMock = new BlockMock(nodeManager);
            return blockMock.retBlockMock();
        } catch (Exception exception) {
            throw new NonExistObjectException("block");
        }
    }

    @Override
    public String getBlockByNumber(String hashOfBlock, Boolean bool) {
        try {
            //todo: getBlockByNumber
            BlockMock blockMock = new BlockMock(nodeManager);
            return blockMock.retBlockMock();
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
}



