package io.yggdrash.node;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.gateway.BlockChainCollector;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BlockChainCollectorTest {

    @Test
    public void shouldCallOnAddedBlock() {
        BlockChainCollector mock = mock(BlockChainCollector.class);
        BlockChain blockChain = BlockChainTestUtils.createBlockChain(false);
        blockChain.addListener(mock);
        ConsensusBlock<io.yggdrash.proto.PbftProto.PbftBlock> nextBlock = BlockChainTestUtils.createNextBlock();
        blockChain.addBlock(nextBlock, true);

        //FIXME 실제로 두번 실행되어야 한다.
        verify(mock, Mockito.times(1))
                .chainedBlock(nextBlock);
    }
}