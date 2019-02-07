package io.yggdrash.core.util;

import io.yggdrash.core.net.KademliaPeerTable;
import io.yggdrash.core.net.PeerBucket;

public class PeerTableCounter {

    private KademliaPeerTable table;

    public PeerTableCounter() {
    }

    private PeerTableCounter(KademliaPeerTable table) {
        this.table = table;
    }

    public PeerTableCounter use(KademliaPeerTable table) {
        this.table = table;
        return this;
    }

    public int totalPeerOfBucket() {
        int cnt = 0;
        for (PeerBucket b : table.getBuckets()) {
            cnt += b.getPeersCount();
        }
        return cnt;
    }

    public static PeerTableCounter of(KademliaPeerTable table) {
        return new PeerTableCounter(table);
    }
}
