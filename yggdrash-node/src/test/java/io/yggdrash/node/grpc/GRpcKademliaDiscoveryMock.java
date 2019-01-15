package io.yggdrash.node.grpc;

import io.yggdrash.core.net.KademliaDiscoveryMock;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerClientChannel;
import io.yggdrash.node.GRpcClientChannel;

public class GRpcKademliaDiscoveryMock extends KademliaDiscoveryMock {

    GRpcKademliaDiscoveryMock(Peer owner) {
        super(owner);
    }

    @Override
    public PeerClientChannel getClient(Peer peer) {
        return new GRpcClientChannel(peer);
    }
}