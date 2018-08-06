package io.yggdrash.node.api;

import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.Wallet;
import io.yggdrash.node.mock.TransactionMock;
import io.yggdrash.node.mock.WalletMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlockMockitoTest {
    @Mock
    private NodeManager nodeManagerMock;
    private Block block;

    private BlockApiImpl blockApiImpl;
    private String hashOfBlock;
    private String numOfblock;
    private Set<Block> blockList = new HashSet<>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        blockApiImpl = new BlockApiImpl(nodeManagerMock);

        TransactionMock txMock = new TransactionMock();
        Transaction tx = txMock.retTxMock();

        Wallet wallet = new Wallet(new DefaultConfig());
        WalletMock.sign(tx);

        BlockBody sampleBody = new BlockBody(Collections.singletonList(tx));

        BlockHeader genesisBlockHeader = new BlockHeader.Builder()
                .blockBody(sampleBody)
                .prevBlock(null)
                .build(wallet);

        BlockHeader blockHeader = new BlockHeader.Builder()
                .blockBody(sampleBody)
                .prevBlock(new Block(genesisBlockHeader, sampleBody)) // genesis block
                .build(wallet);
        block = new Block(blockHeader, sampleBody);
        hashOfBlock = block.getBlockHash();
        blockList.add(block);
        numOfblock = "1";
    }

    private static final Logger log = LoggerFactory.getLogger(BlockApi.class);

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
        Block res = blockApiImpl.getBlockByHash(hashOfBlock, true);
        assertThat(res).isNotNull();
        assertEquals(res.getBlockHash(),hashOfBlock);
    }

    @Test
    public void getBlockByNumberTest() {
        when(nodeManagerMock.getBlockByIndexOrHash(numOfblock)).thenReturn(block);
        Block res = blockApiImpl.getBlockByNumber(numOfblock, true);
        assertThat(res).isNotNull();
        assertEquals(res.getBlockHash(),hashOfBlock);
    }

    @Test
    public void newBlockFilterTest() {
        assertThat(blockApiImpl.newBlockFilter()).isEqualTo(0);
    }
}
