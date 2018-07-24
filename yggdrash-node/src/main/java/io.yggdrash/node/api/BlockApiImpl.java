package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.NodeManager;
import io.yggdrash.node.mock.BlockMock;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@AutoJsonRpcServiceImpl
public class BlockApiImpl implements BlockApi {

    private final NodeManager nodeManager;

    public BlockApiImpl(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    @Override
    public int blockNumber() {
        return 0;
    }

    @Override
    public String getBlockByHash(String address, String tag) throws IOException {
        //todo: getBlockByNumber
        BlockMock blockMock = new BlockMock(nodeManager);
        return blockMock.retBlockMock();
    }

    @Override
    public String getBlockByNumber(String hashOfBlock, Boolean bool) throws IOException {
        //todo: getBlockByNumber
        BlockMock blockMock = new BlockMock(nodeManager);
        return blockMock.retBlockMock();
    }

    @Override
    public int newBlockFilter() {
        return 0;
    }
}



