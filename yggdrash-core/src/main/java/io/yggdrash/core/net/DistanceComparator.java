package io.yggdrash.core.net;

import java.util.Comparator;

public class DistanceComparator implements Comparator<Peer> {
    private byte[] targetId;

    DistanceComparator(byte[] targetId) {
        this.targetId = targetId;
    }

    @Override
    public int compare(Peer p1, Peer p2) {
        int d1 = p1.getPeerId().distanceTo(targetId);
        int d2 = p2.getPeerId().distanceTo(targetId);

        return Integer.compare(d1, d2);
    }
}
