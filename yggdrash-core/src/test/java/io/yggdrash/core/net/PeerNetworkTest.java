package io.yggdrash.core.net;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.PeerTestUtils;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.p2p.BlockChainDialer;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerDialer;
import io.yggdrash.core.p2p.PeerHandlerMock;
import io.yggdrash.core.p2p.PeerTableGroup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PeerNetworkTest {
    private KademliaPeerNetwork peerNetwork;
    private final ConsensusBlock genesis = BlockChainTestUtils.genesisBlock();

    @Before
    public void setUp() {
        PeerDialer peerDialer = new BlockChainDialer(PeerHandlerMock.factory);
        PeerTableGroup peerTableGroup = PeerTestUtils.createTableGroup();
        peerNetwork = new KademliaPeerNetwork(peerTableGroup, peerDialer);
        peerNetwork.addNetwork(genesis.getBranchId(), "pbft");
        peerTableGroup.addPeer(genesis.getBranchId(), Peer.valueOf("ynode://75bff16c@127.0.0.1:32919"));
        peerNetwork.init();
    }

    @Test
    public void getHandlerList() {
        Assert.assertFalse(peerNetwork.getHandlerList(genesis.getBranchId()).isEmpty());
    }

    @Test
    public void chainedBlock() {
        peerNetwork.chainedBlock(genesis);
    }

    @Test
    public void receivedTransaction() {
        peerNetwork.receivedTransaction(BlockChainTestUtils.createTransferTx());
    }
}