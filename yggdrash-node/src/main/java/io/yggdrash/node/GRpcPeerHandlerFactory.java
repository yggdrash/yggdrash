package io.yggdrash.node;

import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerHandler;
import io.yggdrash.core.net.PeerHandlerFactory;

public class GRpcPeerHandlerFactory implements PeerHandlerFactory {
    @Override
    public PeerHandler create(Peer peer) {
        return new GRpcPeerHandler(peer);
    }
}
