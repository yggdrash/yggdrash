package io.yggdrash.core.p2p;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DistanceComparatorTest {
    private static final Logger log = LoggerFactory.getLogger(DistanceComparatorTest.class);
    private static final Peer OWNER = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");

    @Test
    public void compareTest() {
        DistanceComparator distanceComparator =
                new DistanceComparator(OWNER.getPeerId().getBytes());

        Peer p1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32919");
        Peer p2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32922");

        int shortDistance = OWNER.getPeerId().distanceTo(p1.getPeerId().getBytes());
        int longDistance = OWNER.getPeerId().distanceTo(p2.getPeerId().getBytes());
        assert shortDistance < longDistance;
        assert distanceComparator.compare(p1, p2) < 0;

        ArrayList<Peer> peerArrayList = new ArrayList<>();
        peerArrayList.add(p2);
        peerArrayList.add(p1);
        peerArrayList.sort(distanceComparator);
        assert peerArrayList.get(0).equals(p1);
        Assert.assertEquals(shortDistance,peerArrayList.get(0).getPeerId().distanceTo(OWNER.getPeerId().getBytes()));
    }

    @Test
    public void compareTestWith20Peers() {
        String uri = "ynode://9ea9225f0b7db3c697c0a2e09cdd65046899058d16f73378c1559d61aa3e10cd5dc"
                + "9337142728f5a02faadafab2b926e2998d5bc2b62c2183fab75ca996de2ce@localhost:";
        int seedPort = 32918;
        Peer seedPeer = Peer.valueOf(uri + seedPort);

        Map<String, Integer> result = new HashMap<>();
        ArrayList<Peer> peerArrayList = new ArrayList<>();

        for (int i = seedPort + 1; i < seedPort + 20; i++) {
            Peer peer = Peer.valueOf(uri + i);
            peerArrayList.add(peer);
            int distance = seedPeer.getPeerId().distanceTo(peer.getPeerId().getBytes()) - 1;
            result.put(String.valueOf(peer.getPort()), distance);
            log.debug("peer port => " + peer.getPort() + ", distance => " + distance);
        }

        DistanceComparator distanceComparator =
                new DistanceComparator(seedPeer.getPeerId().getBytes());

        peerArrayList.sort(distanceComparator);
        peerArrayList.forEach(peer -> log.debug("[Sorted] Peer port => " + peer.getPort()
                + ", Distance => " + result.get(String.valueOf(peer.getPort()))));

        assert peerArrayList.get(0).getPort() == 32933;
        assert result.get(String.valueOf(32933)).equals(152);

        assert peerArrayList.get(1).getPort() == 32925;
        assert result.get(String.valueOf(32925)).equals(156);

        assert peerArrayList.get(2).getPort() == 32932;
        assert result.get(String.valueOf(32932)).equals(156);

        assert peerArrayList.get(3).getPort() == 32927;
        assert result.get(String.valueOf(32927)).equals(158);

        assert peerArrayList.get(4).getPort() == 32928;
        assert result.get(String.valueOf(32928)).equals(158);

        assert peerArrayList.get(5).getPort() == 32919;
        Assert.assertEquals(159, (int) result.get(String.valueOf(32919)));
    }
}