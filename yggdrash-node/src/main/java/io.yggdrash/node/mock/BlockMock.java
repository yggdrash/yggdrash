package io.yggdrash.node.mock;

import io.yggdrash.core.Block;
import io.yggdrash.core.NodeManager;

import java.io.IOException;

public class BlockMock {

    private final NodeManager nodeManager;

    public BlockMock(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public String retBlockMock() throws IOException {
        BlockBuilderMock blockBuilderMock = new BlockBuilderMock(nodeManager);
        Block block = blockBuilderMock.build(nodeManager.getWallet());
        return block.toString();
    }

}

