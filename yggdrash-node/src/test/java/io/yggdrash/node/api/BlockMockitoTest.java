package io.yggdrash.node.api;

import io.yggdrash.core.NodeManager;
import io.yggdrash.core.husk.BlockHusk;
import io.yggdrash.node.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class BlockMockitoTest {
    @Mock
    private NodeManager nodeManagerMock;
    private BlockHusk block;

    private BlockApiImpl blockApiImpl;
    private String hashOfBlock;
    private String numOfblock;
    private Set<BlockHusk> blockList = new HashSet<>();

    @Before
    public void setUp() {
        blockApiImpl = new BlockApiImpl(nodeManagerMock);
        block = TestUtils.createGenesisBlockHusk();
        hashOfBlock = block.getHash().toString();
        blockList.add(block);
        numOfblock = "1";
    }

    @Test
    public void blockNumberTest() {
        when(nodeManagerMock.getBlocks()).thenReturn(blockList);
        assertThat(blockApiImpl.blockNumber()).isEqualTo(blockList.size());
    }

    @Test
    public void getAllBlockTest() {
        when(nodeManagerMock.getBlocks()).thenReturn(blockList);
        assertThat(blockApiImpl.getAllBlock()).isNotEmpty();
        assertThat(blockApiImpl.getAllBlock().size()).isEqualTo(1);
    }

    @Test
    public void getBlockByHashTest() {
        when(nodeManagerMock.getBlockByIndexOrHash(hashOfBlock)).thenReturn(block);
        BlockHusk res = blockApiImpl.getBlockByHash(hashOfBlock, true);
        assertThat(res).isNotNull();
        assertEquals(res.getHash().toString(), hashOfBlock);
    }

    @Test
    public void getBlockByNumberTest() {
        when(nodeManagerMock.getBlockByIndexOrHash(numOfblock)).thenReturn(block);
        BlockHusk res = blockApiImpl.getBlockByNumber(numOfblock, true);
        assertThat(res).isNotNull();
        assertEquals(res.getHash().toString(), hashOfBlock);
    }

    @Test
    public void newBlockFilterTest() {
        assertThat(blockApiImpl.newBlockFilter()).isEqualTo(0);
    }
}
