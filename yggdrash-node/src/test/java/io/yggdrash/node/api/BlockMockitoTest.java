package io.yggdrash.node.api;

import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.exception.InternalErrorException;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.node.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlockMockitoTest {
    @Mock
    private BranchGroup branchGroupMock;
    private BlockHusk block;

    private BlockApiImpl blockApiImpl;
    private String hashOfBlock;
    private String numOfblock;
    private List<BlockHusk> blockList = new ArrayList<>();

    @Before
    public void setUp() {
        blockApiImpl = new BlockApiImpl(branchGroupMock);
        block = TestUtils.createGenesisBlockHusk();
        hashOfBlock = block.getHash().toString();
        blockList.add(block);
        numOfblock = "1";
    }

    @Test
    public void blockNumberTest() {
        when(branchGroupMock.getLastIndex()).thenReturn(0L);
        assertThat(blockApiImpl.blockNumber()).isEqualTo(blockList.size());
    }

    @Test(expected = InternalErrorException.class)
    public void blockNumberExceptionTest() {
        when(branchGroupMock.getLastIndex()).thenThrow(new RuntimeException());
        blockApiImpl.blockNumber();
    }

    @Test
    public void getBlockByHashTest() {
        when(branchGroupMock.getBlockByIndexOrHash(hashOfBlock)).thenReturn(block);
        BlockHusk res = blockApiImpl.getBlockByHash(hashOfBlock, true);
        assertThat(res).isNotNull();
        assertEquals(res.getHash().toString(), hashOfBlock);
    }

    @Test
    public void getBlockByNumberTest() {
        when(branchGroupMock.getBlockByIndexOrHash(numOfblock)).thenReturn(block);
        BlockHusk res = blockApiImpl.getBlockByNumber(numOfblock, true);
        assertThat(res).isNotNull();
        assertEquals(res.getHash().toString(), hashOfBlock);
    }

    @Test(expected = NonExistObjectException.class)
    public void getBlockByNumberExceptionTest() {
        when(branchGroupMock.getBlockByIndexOrHash(numOfblock)).thenThrow(new RuntimeException());
        blockApiImpl.getBlockByNumber(numOfblock, true);
    }

    @Test
    public void newBlockFilterTest() {
        assertThat(blockApiImpl.newBlockFilter()).isEqualTo(0);
    }
}
