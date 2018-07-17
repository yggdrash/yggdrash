package io.yggdrash.node.api;

import io.yggdrash.core.Block;
import io.yggdrash.node.mock.BlockBuilderMock;
import io.yggdrash.node.mock.BlockMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@Import(JsonRpcConfig.class)
public class BlockApiImplTest {
    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);

    @Test
    public void createBlockMock() throws IOException {
        BlockMock blockMock = new BlockMock();
        log.debug("blockMock" + blockMock.retBlockMock());
    }

    @Test
    public void blockBuildMockTest() throws IOException {
        BlockBuilderMock blockBuilderMock = new BlockBuilderMock();
        Block block = blockBuilderMock.build();
        log.debug("blockBuilderMock : " + block.toString());
    }
}
