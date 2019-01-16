package io.yggdrash.node.grpc;

import io.yggdrash.core.net.KademliaDiscoveryMock;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerHandler;
import io.yggdrash.node.GRpcPeerHandler;

public class GRpcKademliaDiscoveryMock extends KademliaDiscoveryMock {

    GRpcKademliaDiscoveryMock(Peer owner) {
        super(owner);
    }

    @Override
    public PeerHandler getPeerHandler(Peer peer) {
        return new GRpcPeerHandler(peer);
    }
}