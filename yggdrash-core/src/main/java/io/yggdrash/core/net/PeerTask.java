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

package io.yggdrash.core.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class PeerTask {

    private static final Logger log = LoggerFactory.getLogger(PeerTask.class);

    private KademliaPeerTable peerTable;

    private PeerHandlerGroup peerHandlerGroup;

    private NodeStatus nodeStatus;

    public void setPeerTable(KademliaPeerTable peerTable) {
        this.peerTable = peerTable;
    }

    public void setPeerHandlerGroup(PeerHandlerGroup peerHandlerGroup) {
        this.peerHandlerGroup = peerHandlerGroup;
    }

    public void setNodeStatus(NodeStatus nodeStatus) {
        this.nodeStatus = nodeStatus;
    }

    public void healthCheck() {
        if (!nodeStatus.isUpStatus()) {
            return;
        }
        List<Peer> closestPeerList = peerTable.getClosestPeers(peerTable.getOwner(), KademliaOptions.BROADCAST_SIZE);
        peerHandlerGroup.healthCheck(peerTable.getOwner(), closestPeerList);
    }

    public void refresh() {
        // The Kademlia paper specifies that the bucket refresh should perform a lookup
        // in the least recently used bucket. We cannot adhere to this because
        // the findnode target is a 512bit value (not hash-sized) and it is not easily possible
        // to generate a sha3 preimage that falls into a chosen bucket.
        // We perform a few lookups with a random target instead.
        Peer randomTarget = randomTargetGeneration();
        peerTable.refresh(randomTarget);
    }

    private Peer randomTargetGeneration() {
        // Ethereum generates 3 random pubKeys to perform the lookup.
        // Should we create random peers to do the same?
        // Maybe it can relate to "resolve" function
        String pubKey = Hex.toHexString(UUID.randomUUID().toString().getBytes());
        return Peer.valueOf(pubKey, "localhost", 32918);
    }

    public void revalidate() {
        Peer last = peerToRevalidate();
        if (last == null || last.equals(peerTable.getOwner())) {
            return;
        }
        // Ping the selected node and wait for a pong (set last.id to ping msg)
        log.debug("[revalidate] last ynodeUri => " + last.getYnodeUri());
        if (peerHandlerGroup.healthCheck(peerTable.getOwner(), Collections.singletonList(last))) {
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

    public void copyNode() {
        long minTableTime = 30000;      //30 seconds
        peerTable.copyLiveNode(minTableTime);
    }

    // returns the last node in a random, non-empty bucket
    private Peer peerToRevalidate() {
        Random r = new Random();
        int cnt = 1;
        int startIndex = r.nextInt(KademliaOptions.BINS);

        for (; cnt < KademliaOptions.BINS; startIndex++, cnt++) {
            if (startIndex == 0 || startIndex == KademliaOptions.BINS) {
                startIndex = 1;
            }

            //log.debug("peerTask :: bucketIndex => " + startIndex);

            PeerBucket bucket = peerTable.getBucketByIndex(startIndex);

            if (!bucket.getReplacements().isEmpty()) {
                return bucket.getLastPeer();
            }
        }
        return null;
    }
}
