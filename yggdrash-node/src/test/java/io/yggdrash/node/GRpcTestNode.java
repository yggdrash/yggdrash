package io.yggdrash.node;

import io.yggdrash.PeerTestUtils;
import io.yggdrash.core.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GRpcTestNode extends BootStrapNode {
    private static final Logger log = LoggerFactory.getLogger(GRpcTestNode.class);

    private static final int MAX_PEERS = 25;

    final PeerTable peerTable;
    final int port;
    final DiscoveryConsumer consumer;

    GRpcTestNode(PeerHandlerFactory factory, int port) {
        this.port = port;
        this.peerHandlerGroup = new SimplePeerHandlerGroup(factory);
        this.peerTable = PeerTestUtils.createPeerTable(port);
        this.consumer = new DiscoveryServiceConsumer(peerTable);
    }

    void bootstrapping() {
        super.bootstrapping(new KademliaDiscovery(peerTable), MAX_PEERS);
    }

    int getActivePeerCount() {
        return peerHandlerGroup.handlerCount();
    }

    void logDebugging() {
        log.info("{} => peerTable={}, active={}", peerTable.getOwner(),
                String.format("%3d", peerTable.count()), getActivePeerCount());
    }
}
