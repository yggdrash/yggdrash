package io.yggdrash.node.mock;

import io.yggdrash.node.BlockBuilder;

public class BlockBuilderMock implements BlockBuilder {

    @Override
    public Block build(String data) {
        return new Block();
    }
}
