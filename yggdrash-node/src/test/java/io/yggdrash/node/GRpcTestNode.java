package io.yggdrash.node;

import io.yggdrash.PeerTestUtils;
import io.yggdrash.core.net.BootStrapNode;
import io.yggdrash.core.net.DiscoveryConsumer;
import io.yggdrash.core.net.DiscoveryServiceConsumer;
import io.yggdrash.core.net.KademliaPeerTable;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.NodeStatusMock;
import io.yggdrash.core.net.PeerHandlerFactory;
import io.yggdrash.core.net.PeerHandlerGroup;
import io.yggdrash.core.net.SimplePeerHandlerGroup;
import io.yggdrash.core.util.PeerTableCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GRpcTestNode extends BootStrapNode {
    private static final Logger log = LoggerFactory.getLogger(GRpcTestNode.class);

    public final KademliaPeerTable peerTable;
    public final int port;
    public final DiscoveryConsumer consumer;
    public final PeerHandlerGroup peerHandlerGroup;
    public final PeerTask peerTask;

    public GRpcTestNode(PeerHandlerFactory factory, int port) {
        this.port = port;
        this.peerTable = PeerTestUtils.createTable(port, factory);
        setDht(peerTable);
        this.consumer = new DiscoveryServiceConsumer(peerTable);
        this.peerHandlerGroup = new SimplePeerHandlerGroup(factory);

        this.peerTask = new PeerTask();
        NodeStatus nodeStatus = NodeStatusMock.mock;
        peerTask.setNodeStatus(nodeStatus);
        peerTask.setPeerHandlerGroup(peerHandlerGroup);
        peerTask.setPeerTable(peerTable);
    }

    public int getActivePeerCount() {
        return peerHandlerGroup.handlerCount();
    }

    public void logDebugging() {
        log.info("{} => peer={}, bucket={}, active={}",
                peerTable.getOwner(),
                String.format("%3d", PeerTableCounter.of(peerTable).totalPeerOfBucket()),
                peerTable.getBucketsCount(),
                getActivePeerCount());
    }
}
