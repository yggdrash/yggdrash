package io.yggdrash.core.p2p;

public interface PeerHandlerFactory {
    PeerHandler create(String consensusAlgorithm, Peer peer);
}
