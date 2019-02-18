package io.yggdrash.node;

import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerHandler;
import io.yggdrash.core.p2p.PeerHandlerFactory;

public class GRpcPeerHandlerFactory implements PeerHandlerFactory {
    @Override
    public PeerHandler create(Peer peer) {
        return new GRpcPeerHandler(peer);
    }
}
