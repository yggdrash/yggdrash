package io.yggdrash.node.mock;

import io.yggdrash.core.Block;

import java.io.IOException;

public class BlockMock {

    public String retBlockMock() throws IOException {
        BlockBuilderMock blockBuilderMock = new BlockBuilderMock();
        Block block = blockBuilderMock.build();
        return block.toString();
    }

}

