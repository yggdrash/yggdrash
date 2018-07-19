package io.yggdrash.node.api;

import io.yggdrash.core.Block;
import io.yggdrash.core.NodeManager;
import io.yggdrash.node.mock.BlockBuilderMock;
import io.yggdrash.node.mock.BlockMock;
import io.yggdrash.node.mock.NodeManagerMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@Import(ApplicationConfig.class)
public class BlockApiTest {
    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);

    private final NodeManager nodeManager = new NodeManagerMock();

    @Test
    public void createBlockMock() throws IOException {
        BlockMock blockMock = new BlockMock(nodeManager);
        log.debug("blockMock" + blockMock.retBlockMock());
    }

    @Test
    public void blockBuildMockTest() throws IOException {
        BlockBuilderMock blockBuilderMock = new BlockBuilderMock(nodeManager);
        Block block = blockBuilderMock.build(nodeManager.getWallet());
        log.debug("blockBuilderMock : " + block.toString());
    }
}
