package io.yggdrash.node.mock;

import io.yggdrash.core.Account;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.node.BlockBuilder;

import java.util.Arrays;

public class BlockBuilderMock implements BlockBuilder {
    @Override
    public Block build(String data) {
        Account account = new Account();
        BlockBody blockBody = new BlockBody(Arrays.asList());
        BlockHeader blockHeader = new BlockHeader.Builder()
                .account(account)
                .prevBlock(null)
                .blockBody(blockBody).build();
        return new Block(blockHeader, blockBody);
    }
}
