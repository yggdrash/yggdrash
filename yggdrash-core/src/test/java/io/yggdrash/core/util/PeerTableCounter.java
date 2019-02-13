package io.yggdrash.core.util;

import io.yggdrash.core.net.KademliaPeerTable;
import io.yggdrash.core.net.PeerBucket;
import io.yggdrash.core.net.PeerTable;

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
