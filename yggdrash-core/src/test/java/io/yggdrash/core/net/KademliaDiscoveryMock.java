package io.yggdrash.core.net;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.store.StoreBuilder;

public class KademliaDiscoveryMock extends KademliaDiscovery {
    private static final int MAX_PEERS = 25;
    private static final StoreBuilder BUILDER = new StoreBuilder(new DefaultConfig());

    public KademliaDiscoveryMock(Peer owner) {
        PeerGroup peerGroup = new PeerGroup(owner, BUILDER.buildPeerStore(), MAX_PEERS);
        peerGroup.setPeerHandlerFactory(PeerHandlerMock.factory);
        this.setPeerGroup(peerGroup);
    }
}