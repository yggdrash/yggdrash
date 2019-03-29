package io.yggdrash.core.p2p;

import io.yggdrash.TestConstants;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.yggdrash.PeerTestUtils.SEED_PORT;

public class PeerTableLargeTest {
    private static final Logger log = LoggerFactory.getLogger(PeerTableLargeTest.class);
    private static final String NODE_URI_PREFIX = "ynode://d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95@172.16.10.159:";

    private static final Peer SEED = Peer.valueOf(NODE_URI_PREFIX + SEED_PORT);
    private final List<Peer> peerList = new ArrayList<>();
    private final Map<Peer, PeerTable> peerTableMap = new HashMap<>();

    @Test
    public void peerTableTest() {
        TestConstants.SlowTest.apply();
        targetSizePeerTableTest(100, 5, 20); // 6s
        //targetSizePeerTableTest(100, 8, 30); // 7s
        //targetSizePeerTableTest(100, 16, 45); // 8s

        //targetSizePeerTableTest(300, 5, 25); // 2m fail (278)
        //targetSizePeerTableTest(300, 5, 30); // 3.5m <- (default options)
        //targetSizePeerTableTest(800, 5, 30); // ?h fail (805)

        //targetSizePeerTableTest(300, 8, 35); // 3m fail (274)
        //targetSizePeerTableTest(400, 8, 40); // 4m fail (442)

        //targetSizePeerTableTest(300, 16, 60); // 4m fail (289)
        //targetSizePeerTableTest(300, 16, 65); // 4.5m  fail (377)

    }

    private void targetSizePeerTableTest(int maxNode, int bucketSize, int limit) {
        KademliaOptions.BUCKET_SIZE = bucketSize;
        for (int i = 2; i <= maxNode; i++) {
            test(i, limit);
        }
    }

    private void test(int nodeCount, int limit) {
        initTable(nodeCount);

        int totalVisitCount = 0;
        for (PeerTable table : peerTableMap.values()) {
            List<PeerTable> visitTarget = new ArrayList<>(peerTableMap.values());
            Assert.assertEquals(visitTarget.size(), nodeCount - 1); // exclude seed
            totalVisitCount = recursiveVisit(table, visitTarget, limit);
            Assert.assertTrue(visitTarget.isEmpty());
            //break; // test first node only
        }

        log.debug("=== nodeCount={}, totalVisitCount={}", nodeCount, totalVisitCount);
        peerList.clear();
        peerTableMap.clear();
    }

    private int recursiveVisit(PeerTable table, List<PeerTable> visitTarget, int limit) {
        if (visitTarget.isEmpty()) {
            return -1;
        }
        if (!visitTarget.contains(table)) {
            return 1;
        }
        visitTarget.remove(table);
        List<Peer> closest = closest(table, limit, false);
        int count = 1;
        for (Peer peer : closest) {
            PeerTable targetTable = peerTableMap.get(peer);
            int visitCount = recursiveVisit(targetTable, visitTarget, limit);
            if (visitCount == -1) {
                return count;
            }
            count = count + visitCount + 1;
        }
        return count;
    }

    private void initTable(int nodeCount) {
        for (int i = 0; i < nodeCount; i++) {
            Peer owner = Peer.valueOf(NODE_URI_PREFIX + (SEED_PORT + i));
            peerList.add(owner);
            if (i > 0) {
                // exclude seed table
                peerTableMap.put(owner, new KademliaPeerTable(owner, null));
            }
        }

        for (PeerTable table : peerTableMap.values()) {
            // fill all peers in table
            for (Peer peer : peerList) {
                // exclude owner
                if (table.getOwner().equals(peer)) {
                    continue;
                }
                table.addPeer(peer);
            }
        }
    }

    private List<Peer> closest(PeerTable table, int limit, boolean debugClosest) {
        if (SEED.equals(table.getOwner())) {
            return Collections.emptyList();
        }
        List<Peer> closest = table.getClosestPeers(table.getOwner(), limit);
        closest.remove(SEED); // exclude seed
        if (debugClosest) {
            String closestStr = table.getOwner().getPort() + " -> ";
            for (Peer peer : closest) {
                closestStr = closestStr.concat(" " + peer.getPort());
            }
            log.debug("closest: {}", closestStr);
        }
        return closest;
    }
}