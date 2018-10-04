package io.yggdrash.node.api;

import io.yggdrash.TestUtils;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.exception.InternalErrorException;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.node.controller.BlockDto;
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
    private long numOfBlock;
    private final List<BlockHusk> blockList = new ArrayList<>();
    private final BranchId stem = BranchId.stem();

    @Before
    public void setUp() {
        blockApiImpl = new BlockApiImpl(branchGroupMock);
        block = TestUtils.createGenesisBlockHusk();
        hashOfBlock = block.getHash().toString();
        blockList.add(block);
        numOfBlock = 1;
    }

    @Test
    public void blockNumberTest() {
        when(branchGroupMock.getLastIndex(stem)).thenReturn(0L);
        assertThat(blockApiImpl.blockNumber(stem.toString())).isEqualTo(blockList.size());
    }

    @Test(expected = InternalErrorException.class)
    public void blockNumberExceptionTest() {
        when(branchGroupMock.getLastIndex(stem)).thenThrow(new RuntimeException());
        blockApiImpl.blockNumber(stem.toString());
    }

    @Test
    public void getBlockByHashTest() {
        when(branchGroupMock.getBlockByHash(stem, hashOfBlock)).thenReturn(block);
        BlockDto res = blockApiImpl.getBlockByHash(stem.toString(), hashOfBlock, true);
        assertThat(res).isNotNull();
        assertEquals(res.getHash().toString(), hashOfBlock);
    }

    @Test
    public void getBlockByNumberTest() {
        when(branchGroupMock.getBlockByIndex(stem, numOfBlock)).thenReturn(block);
        BlockDto res = blockApiImpl.getBlockByNumber(stem.toString(), numOfBlock, true);
        assertThat(res).isNotNull();
        assertEquals(res.getHash().toString(), hashOfBlock);
    }

    @Test(expected = NonExistObjectException.class)
    public void getBlockByNumberExceptionTest() {
        when(branchGroupMock.getBlockByIndex(stem, numOfBlock)).thenThrow(new RuntimeException());
        blockApiImpl.getBlockByNumber(stem.toString(), numOfBlock, true);
    }

    @Test
    public void newBlockFilterTest() {
        assertThat(blockApiImpl.newBlockFilter()).isEqualTo(0);
    }
}
