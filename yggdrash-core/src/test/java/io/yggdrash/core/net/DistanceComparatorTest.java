package io.yggdrash.core.net;

import org.junit.Test;

import java.util.ArrayList;

public class DistanceComparatorTest {
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
        assert peerArrayList.get(0).getPeerId().distanceTo(OWNER.getPeerId().getBytes())
                == shortDistance;
    }
}