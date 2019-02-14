package io.yggdrash.core.net;

import io.yggdrash.PeerTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BranchId;
import org.junit.Test;

public class DiscoveryServiceConsumerTest {
    private BranchId yggdrash = TestConstants.yggdrash();

    @Test
    public void peerAddedByPingTest() {
        // arrange
        Peer from = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        Peer to = Peer.valueOf("ynode://aaaaaaaa@127.0.0.1:32920");

        KademliaPeerTableGroup peerTableGroup = PeerTestUtils.createTableGroup();
        peerTableGroup.createTable(yggdrash);
        DiscoveryConsumer discoveryConsumer = new DiscoveryServiceConsumer(peerTableGroup);
        assert discoveryConsumer.findPeers(yggdrash, from).size() == 0;

        // act
        // add peer if address matched
        discoveryConsumer.ping(yggdrash, from, to, "Ping");

        // assert
        assert peerTableGroup.getPeerTable(yggdrash).getBucketsCount() == 1;
    }
}