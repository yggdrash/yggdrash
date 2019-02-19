package io.yggdrash.node;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.PeerTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.akashic.SimpleSyncManager;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.net.BlockChainConsumer;
import io.yggdrash.core.net.BlockChainServiceConsumer;
import io.yggdrash.core.net.BootStrapNode;
import io.yggdrash.core.net.DiscoveryConsumer;
import io.yggdrash.core.net.DiscoveryServiceConsumer;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.NodeStatusMock;
import io.yggdrash.core.net.PeerNetwork;
import io.yggdrash.core.p2p.PeerDialer;
import io.yggdrash.core.p2p.PeerHandlerFactory;
import io.yggdrash.core.p2p.PeerTable;
import io.yggdrash.core.p2p.PeerTableGroup;
import io.yggdrash.core.p2p.SimplePeerDialer;
import io.yggdrash.core.util.PeerTableCounter;
import io.yggdrash.node.config.NetworkConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GRpcTestNode extends BootStrapNode {
    private static final Logger log = LoggerFactory.getLogger(GRpcTestNode.class);

    // discovery specific
    public final int port;
    public DiscoveryConsumer discoveryConsumer;
    private PeerDialer peerDialer;
    public PeerTableGroup peerTableGroup;
    public PeerTask peerTask;

    // branch specific
    public BlockChainConsumer blockChainConsumer;

    public GRpcTestNode(PeerHandlerFactory factory, int port, boolean enableBranch) {
        this.port = port;

        // node configuration
        NodeStatus nodeStatus = NodeStatusMock.mock;
        setNodeStatus(nodeStatus);
        setBranchGroup(new BranchGroup());
        setSyncManager(new SimpleSyncManager());

        p2pConfiguration(factory, nodeStatus);

        branchConfiguration(enableBranch);

        networkConfiguration();
    }

    private void p2pConfiguration(PeerHandlerFactory factory, NodeStatus nodeStatus) {
        this.peerDialer = new SimplePeerDialer(factory);
        this.peerTableGroup = PeerTestUtils.createTableGroup(port, peerDialer);
        this.discoveryConsumer = new DiscoveryServiceConsumer(peerTableGroup);

        this.peerTask = new PeerTask();
        peerTask.setNodeStatus(nodeStatus);
        peerTask.setPeerDialer(peerDialer);
        peerTask.setPeerTableGroup(peerTableGroup);
    }

    private void branchConfiguration(boolean enableBranch) {
        if (!enableBranch || isSeed()) {
            return;
        }
        BlockChain bc = BlockChainTestUtils.createBlockChain(false);
        branchGroup.addBranch(bc);
        blockChainConsumer = new BlockChainServiceConsumer(branchGroup);
    }

    private void networkConfiguration() {
        NetworkConfiguration config = new NetworkConfiguration();
        PeerNetwork peerNetwork = config.peerNetwork(peerTableGroup, peerDialer, branchGroup);
        setPeerNetwork(peerNetwork);
    }

    public BranchGroup getBranchGroup() {
        return branchGroup;
    }

    public int getActivePeerCount() {
        return peerDialer.handlerCount();
    }

    public void logDebugging() {
        PeerTable peerTable = peerTableGroup.getPeerTable(TestConstants.yggdrash());
        log.info("{} => peer={}, bucket={}, active={}",
                peerTableGroup.getOwner(),
                String.format("%3d", PeerTableCounter.of(peerTable).totalPeerOfBucket()),
                peerTable.getBucketsCount(),
                getActivePeerCount());
    }

    public boolean isSeed() {
        return port == PeerTestUtils.SEED_PORT;
    }
}
