package io.yggdrash.core.net;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class YggdrashNodeTest {

    @Test
    public void bootstrapping() {
        YggDrashTestNode node1 = new YggDrashTestNode("ynode://75bff16c@127.0.0.1:32918");
        node1.bootstrapping();
    }

    private class YggDrashTestNode extends YggdrashNode {

        YggDrashTestNode(String ynodeUri) {
            this.discovery = new KademliaDiscoveryMock(Peer.valueOf(ynodeUri));
            List<String> seedList = Collections.singletonList("ynode://75bff16c@127.0.0.1:32918");
            discovery.getPeerGroup().setSeedPeerList(seedList);
        }

        @Override
        protected PeerClientChannel getChannel(Peer peer) {
            return ChannelMock.dummy(peer);
        }
    }
}