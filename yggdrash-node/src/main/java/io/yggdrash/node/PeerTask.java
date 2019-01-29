/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node;

import io.yggdrash.core.net.KademliaOptions;
import io.yggdrash.core.net.KademliaPeerTable;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerBucket;
import io.yggdrash.core.net.PeerHandlerGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Random;

public class PeerTask {

    private static final Logger log = LoggerFactory.getLogger(PeerTask.class);

    @Autowired
    private KademliaPeerTable peerTable;
    @Autowired
    private PeerHandlerGroup peerHandlerGroup;
    @Autowired
    private NodeStatus nodeStatus;

    @Scheduled(cron = "*/10 * * * * *")
    public void healthCheck() {
        if (!nodeStatus.isUpStatus()) {
            return;
        }
        peerHandlerGroup.healthCheck(peerTable.getOwner());
    }

    // refresh performs a lookup for a random target to keep buckets full.
    // seed nodes are inserted if the table is empty (initial bootstrap or discarded faulty peers).
    @Scheduled(cron = "*/30 * * * * *")
    public void lookup() {
        // Run self lookup to discover new neighbor nodes.


        // The Kademlia paper specifies that the bucket refresh should perform a lookup
        // in the least recently used bucket. We cannot adhere to this because
        // the findnode target is a 512bit value (not hash-sized) and it is not easily possible
        // to generate a sha3 preimage that falls into a chosen bucket.
        // We perform a few lookups with a random target instead.
        Peer randomTarget = randomTargetGeneration();
        peerTable.refresh(randomTarget);
    }

    private Peer randomTargetGeneration() {
        // TODO randomLookup
        // Ethereum generates 3 random pubKeys to perform the lookup.
        // Should we create random peers to do the same?
        // Maybe it can relate to "resolve" function
        return null;
    }

    // revalidate checks that the last node in a random bucket is still live
    // and replaces or deletes the node if it isn't
    @Scheduled(cron = "*/10 * * * * *")
    public void revalidate() {
        Peer last = peerToRevalidate();
        if (last != null) {
            // Ping the selected node and wait for a pong (set last.id to ping msg)
            log.debug("[revalidate] last peerId => " + last.getPeerId());
            if (peerHandlerGroup.isPingSucceed(peerTable.getOwner(), last.getPeerId())) {
                // The peer responded, move it to the front
                peerTable.getBucketByPeer(last).bump(last);
            } else {
                // No reply received, pick a replacement or delete the node
                // if there aren't any replacement
                Peer result = peerTable.pickReplacement(last);
                if (result != null) {
                    log.debug("Replaced dead peer '{}' to '{}'",
                            last.getPeerId(), result.getPeerId());
                } else {
                    log.debug("Removed dead peer '{}'", last.getPeerId());
                }
            }
        }
    }

    // copyNode adds peers from the table to the database if they have been in the table
    // longer then minTableTime.
    @Scheduled(cron = "*/30 * * * * *")
    public void copyNode() {
        long minTableTime = 30000;      //30 seconds
        peerTable.copyLiveNode(minTableTime);
    }

    // returns the last node in a random, non-empty bucket
    private Peer peerToRevalidate() {
        Random r = new Random();
        PeerBucket bucket;
        int cnt;

        do {
            bucket = peerTable.getBucketByIndex(r.nextInt(KademliaOptions.BUCKET_SIZE));
            cnt = bucket.getPeersCount();
        } while (cnt < 1);

        return bucket.getLastPeer();
    }
}
