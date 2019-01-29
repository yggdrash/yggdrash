package io.yggdrash.node;

import io.yggdrash.PeerTestUtils;
import io.yggdrash.core.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GRpcTestNode extends BootStrapNode {
    private static final Logger log = LoggerFactory.getLogger(GRpcTestNode.class);

    final PeerTable peerTable;
    final int port;
    final DiscoveryConsumer consumer;
    private final PeerHandlerGroup peerHandlerGroup;

    GRpcTestNode(PeerHandlerFactory factory, int port) {
        this.port = port;
        this.peerTable = PeerTestUtils.createTable(port, factory);
        this.consumer = new DiscoveryServiceConsumer(peerTable);
        this.peerHandlerGroup = new SimplePeerHandlerGroup(factory);
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
