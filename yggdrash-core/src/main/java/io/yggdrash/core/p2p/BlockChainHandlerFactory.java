package io.yggdrash.core.p2p;

public interface BlockChainHandlerFactory {
    BlockChainHandler create(String consensusAlgorithm, Peer peer);
}
