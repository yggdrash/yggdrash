package io.yggdrash.node;

import io.yggdrash.node.mock.Block;

public interface BlockBuilder {
    Block build(String data);
}
