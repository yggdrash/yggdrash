package io.yggdrash.core.net;

public interface PeerHandlerFactory {
    PeerHandler create(Peer peer);
}
