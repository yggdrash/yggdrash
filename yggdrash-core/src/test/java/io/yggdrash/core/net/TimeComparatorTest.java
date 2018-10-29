package io.yggdrash.core.net;

import io.yggdrash.common.util.Utils;
import org.junit.Test;

import java.util.ArrayList;

public class TimeComparatorTest {

    @Test
    public void compareTest() {
        TimeComparator timeComparator = new TimeComparator();

        Peer p1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32919");
        Utils.sleep(1);
        Peer p2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32922");

        long earlyJoinPeer = p1.getModified();
        long latestJoinPeer = p2.getModified();
        assert earlyJoinPeer < latestJoinPeer;
        assert timeComparator.compare(p1, p2) > 0;

        ArrayList<Peer> peerArrayList = new ArrayList<>();
        peerArrayList.add(p1);
        peerArrayList.add(p2);
        peerArrayList.sort(timeComparator);
        assert peerArrayList.get(0).equals(p2);
    }
}