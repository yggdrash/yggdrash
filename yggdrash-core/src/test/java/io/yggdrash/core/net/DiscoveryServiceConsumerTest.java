package io.yggdrash.core.net;

import io.yggdrash.PeerTestUtils;
import io.yggdrash.TestConstants;
import org.junit.Test;

public class DiscoveryServiceConsumerTest {

    @Test
    public void peerAddedByPingTest() {
        // arrange
        Peer from = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        Peer to = Peer.valueOf("ynode://aaaaaaaa@127.0.0.1:32920");

        KademliaPeerTableGroup peerTableGroup = PeerTestUtils.createTableGroup();
        peerTableGroup.createTable(TestConstants.STEM);
        DiscoveryConsumer discoveryConsumer = new DiscoveryServiceConsumer(peerTableGroup);
        assert discoveryConsumer.findPeers(TestConstants.STEM, from).size() == 0;

        // act
        // add peer if address matched
        discoveryConsumer.ping(TestConstants.STEM, from, to, "Ping");

        // assert
        assert peerTableGroup.getPeerTable(TestConstants.STEM).getBucketsCount() == 1;
    }
}