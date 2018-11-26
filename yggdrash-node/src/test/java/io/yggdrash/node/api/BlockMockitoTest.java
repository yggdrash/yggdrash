package io.yggdrash.node.api;

import io.yggdrash.TestUtils;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
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
    private final List<BlockHusk> blockList = new ArrayList<>();

    @Mock
    private BranchGroup branchGroupMock;
    private BlockHusk block;

    private BlockApiImpl blockApiImpl;
    private String hashOfBlock;
    private long numOfBlock;
    private BranchId branchId;

    @Before
    public void setUp() {
        blockApiImpl = new BlockApiImpl(branchGroupMock);
        block = TestUtils.createGenesisBlockHusk();
        branchId = block.getBranchId();
        hashOfBlock = block.getHash().toString();
        blockList.add(block);
        numOfBlock = 1;
    }

    @Test
    public void blockNumberTest() {
        when(branchGroupMock.getLastIndex(branchId)).thenReturn(1L);
        assertThat(blockApiImpl.blockNumber(branchId.toString())).isEqualTo(blockList.size());
    }

    @Test(expected = NonExistObjectException.class)
    public void blockNumberExceptionTest() {
        when(branchGroupMock.getLastIndex(branchId)).thenThrow(new RuntimeException());
        blockApiImpl.blockNumber(branchId.toString());
    }

    @Test
    public void getBlockByHashTest() {
        when(branchGroupMock.getBlockByHash(branchId, hashOfBlock)).thenReturn(block);
        BlockDto res = blockApiImpl.getBlockByHash(branchId.toString(), hashOfBlock, true);
        assertThat(res).isNotNull();
        assertEquals(res.hash, hashOfBlock);
    }

    @Test
    public void getBlockByNumberTest() {
        when(branchGroupMock.getBlockByIndex(branchId, numOfBlock)).thenReturn(block);
        BlockDto res = blockApiImpl.getBlockByNumber(branchId.toString(), numOfBlock, true);
        assertThat(res).isNotNull();
        assertEquals(res.hash, hashOfBlock);
    }

    @Test(expected = NonExistObjectException.class)
    public void getBlockByNumberExceptionTest() {
        when(branchGroupMock.getBlockByIndex(branchId, numOfBlock))
                .thenThrow(new RuntimeException());
        blockApiImpl.getBlockByNumber(branchId.toString(), numOfBlock, true);
    }

    @Test
    public void newBlockFilterTest() {
        assertThat(blockApiImpl.newBlockFilter()).isEqualTo(0);
    }
}
