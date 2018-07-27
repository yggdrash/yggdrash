package io.yggdrash.node.mock;

import io.yggdrash.core.Block;
import io.yggdrash.core.NodeManager;

import java.io.IOException;

public class BlockMock {

    private final NodeManager nodeManager;

    public BlockMock(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public Block retBlockMock() throws IOException {
        BlockBuilderMock blockBuilderMock = new BlockBuilderMock(nodeManager);
        return blockBuilderMock.build(nodeManager.getWallet());
    }

}

