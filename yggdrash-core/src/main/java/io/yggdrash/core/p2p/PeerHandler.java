package io.yggdrash.core.p2p;

public interface PeerHandler<T> extends DiscoveryHandler, BlockChainHandler<T> {
    Peer getPeer();

    void stop();
}
