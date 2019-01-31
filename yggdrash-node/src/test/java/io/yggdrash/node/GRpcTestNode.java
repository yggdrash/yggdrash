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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GRpcTestNode extends BootStrapNode {
    private static final Logger log = LoggerFactory.getLogger(GRpcTestNode.class);

    public final KademliaPeerTable peerTable;
    final int port;
    final DiscoveryConsumer consumer;
    public final PeerHandlerGroup peerHandlerGroup;
    private final PeerTask peerTask;

    GRpcTestNode(PeerHandlerFactory factory, int port) {
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

    public GRpcTestNode selfRefreshAndHealthCheck() {
        super.bootstrapping();
        peerTask.healthCheck();
        return this;
    }

    int getActivePeerCount() {
        return peerHandlerGroup.handlerCount();
    }

    void logDebugging() {
        log.info("{} => peerBucket={}, active={}",
                peerTable.getOwner(),
                String.format(
                        "%3d",
                        peerTable.getBucketsCount()),
                        getActivePeerCount());
    }
}
