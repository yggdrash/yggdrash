package io.yggdrash.core.net;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.PeerTestUtils;
import io.yggdrash.core.blockchain.BlockHusk;
import org.junit.Before;
import org.junit.Test;

public class PeerNetworkTest {
    private PeerNetwork peerNetwork;
    private BlockHusk genesis = BlockChainTestUtils.genesisBlock();

    @Before
    public void setUp() {
        PeerHandlerGroup peerHandlerGroup = new SimplePeerHandlerGroup(PeerHandlerMock.factory);
        PeerTableGroup peerTableGroup = PeerTestUtils.createTableGroup();
        peerNetwork = new KademliaPeerNetwork(peerTableGroup, peerHandlerGroup);
        peerNetwork.init(genesis.getBranchId());
        peerTableGroup.addPeer(genesis.getBranchId(), Peer.valueOf("ynode://75bff16c@127.0.0.1:32919"));
        peerNetwork.init(genesis.getBranchId());
    }

    @Test
    public void getHandlerList() {
        assert peerNetwork.getHandlerList(genesis.getBranchId()).size() == 1;
    }

    @Test
    public void chainedBlock() {
        peerNetwork.chainedBlock(BlockChainTestUtils.genesisBlock());
    }

    @Test
    public void receivedTransaction() {
        peerNetwork.receivedTransaction(BlockChainTestUtils.createTransferTxHusk());
    }
}