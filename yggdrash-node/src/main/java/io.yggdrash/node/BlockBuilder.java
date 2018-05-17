package io.yggdrash.node;

import io.yggdrash.core.Block;

public interface BlockBuilder {
    Block build(String data);
}
