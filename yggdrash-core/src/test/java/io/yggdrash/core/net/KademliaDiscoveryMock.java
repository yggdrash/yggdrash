package io.yggdrash.core.net;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.store.StoreBuilder;

public class KademliaDiscoveryMock extends KademliaDiscovery {
    private static final int MAX_PEERS = 25;
    private static final StoreBuilder BUILDER = new StoreBuilder(new DefaultConfig());

    protected KademliaDiscoveryMock(Peer owner) {
        setPeerGroup(new PeerGroup(owner, BUILDER.buildPeerStore(), MAX_PEERS));
    }

    @Override
    public PeerClientChannel getClient(Peer peer) {
        return ChannelMock.dummy(peer);
    }
}