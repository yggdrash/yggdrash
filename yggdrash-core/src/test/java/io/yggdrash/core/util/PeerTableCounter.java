package io.yggdrash.core.util;

import io.yggdrash.core.p2p.KademliaPeerTable;
import io.yggdrash.core.p2p.PeerBucket;
import io.yggdrash.core.p2p.PeerTable;

public class PeerTableCounter {

    private KademliaPeerTable table;

    public PeerTableCounter() {
    }

    private PeerTableCounter(KademliaPeerTable table) {
        this.table = table;
    }

    public PeerTableCounter use(PeerTable table) {
        this.table = (KademliaPeerTable)table;
        return this;
    }

    public int totalPeerOfBucket() {
        int cnt = 0;
        for (PeerBucket b : table.getBuckets()) {
            cnt += b.getPeersCount();
        }
        return cnt;
    }

    public static PeerTableCounter of(PeerTable table) {
        return new PeerTableCounter((KademliaPeerTable)table);
    }
}
