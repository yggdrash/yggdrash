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
import io.yggdrash.core.net.KademliaPeerNetwork;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.NodeStatusMock;
import io.yggdrash.core.net.PeerHandlerFactory;
import io.yggdrash.core.net.PeerHandlerGroup;
import io.yggdrash.core.net.PeerNetwork;
import io.yggdrash.core.net.PeerTable;
import io.yggdrash.core.net.PeerTableGroup;
import io.yggdrash.core.net.SimplePeerHandlerGroup;
import io.yggdrash.core.util.PeerTableCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GRpcTestNode extends BootStrapNode {
    private static final Logger log = LoggerFactory.getLogger(GRpcTestNode.class);

    public final PeerTableGroup peerTableGroup;
    public final int port;

    // discovery specific
    public final DiscoveryConsumer discoveryConsumer;
    private final PeerHandlerGroup peerHandlerGroup;
    public final PeerTask peerTask;

    // blockchain specific
    public BlockChainConsumer blockChainConsumer;

    public GRpcTestNode(PeerHandlerFactory factory, int port) {
        this.port = port;
        this.peerTableGroup = PeerTestUtils.createTableGroup(port, factory);
        if (!isSeed()) {
            peerTableGroup.createTable(TestConstants.STEM);
        }
        this.discoveryConsumer = new DiscoveryServiceConsumer(peerTableGroup);
        this.peerHandlerGroup = new SimplePeerHandlerGroup(factory);

        this.peerTask = new PeerTask();
        NodeStatus nodeStatus = NodeStatusMock.mock;
        setNodeStatus(nodeStatus);
        peerTask.setNodeStatus(nodeStatus);
        peerTask.setPeerHandlerGroup(peerHandlerGroup);
        peerTask.setPeerTableGroup(peerTableGroup);

        setPeerNetwork(new KademliaPeerNetwork(peerTableGroup, peerHandlerGroup));

        setBranchGroup(new BranchGroup());
        setSyncManager(new SimpleSyncManager());
    }

    public void activateBlockChainService(boolean activate) {
        if (!activate || isSeed()) {
            return;
        }
        BlockChain bc = BlockChainTestUtils.createBlockChain(false);
        branchGroup.addBranch(bc, peerNetwork);
        blockChainConsumer = new BlockChainServiceConsumer(branchGroup);
    }

    public BranchGroup getBranchGroup() {
        return branchGroup;
    }

    public PeerNetwork getPeerNetwork() {
        return peerNetwork;
    }

    public int getActivePeerCount() {
        return peerHandlerGroup.handlerCount();
    }

    public void logDebugging() {
        PeerTable peerTable = peerTableGroup.getPeerTable(TestConstants.STEM);
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
