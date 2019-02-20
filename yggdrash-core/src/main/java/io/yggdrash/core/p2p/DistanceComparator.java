package io.yggdrash.core.p2p;

import java.util.Comparator;

class DistanceComparator implements Comparator<Peer> {
    private final byte[] targetId;

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
