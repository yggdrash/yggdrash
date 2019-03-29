package io.yggdrash.core.p2p;

import io.yggdrash.common.util.Utils;
import org.junit.Assert;
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
        Assert.assertTrue(earlyJoinPeer < latestJoinPeer);
        Assert.assertTrue(timeComparator.compare(p1, p2) > 0);

        ArrayList<Peer> peerArrayList = new ArrayList<>();
        peerArrayList.add(p1);
        peerArrayList.add(p2);
        peerArrayList.sort(timeComparator);
        Assert.assertEquals(p2, peerArrayList.get(0));
    }
}