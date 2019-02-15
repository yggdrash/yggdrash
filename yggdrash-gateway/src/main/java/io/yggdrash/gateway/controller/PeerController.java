/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.gateway.controller;

import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.p2p.PeerDialer;
import io.yggdrash.core.p2p.PeerTableGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static io.yggdrash.common.config.Constants.BRANCH_ID;

@RestController
@RequestMapping("peers")
class PeerController {

    private final PeerTableGroup peerTableGroup;

    private final PeerDialer peerDialer;

    @Autowired
    public PeerController(PeerTableGroup peerTableGroup, PeerDialer peerDialer) {
        this.peerTableGroup = peerTableGroup;
        this.peerDialer = peerDialer;
    }

    @GetMapping("/network")
    public ResponseEntity getNetwork() {
        return ResponseEntity.ok(peerTableGroup.getAllBrancheId().stream().map(BranchId::toString));
    }

    @GetMapping("/active")
    public ResponseEntity getAllActivePeer() {
        return ResponseEntity.ok(peerDialer.getActivePeerList());
    }

    @GetMapping("/channels")
    public ResponseEntity getChannels() {
        return ResponseEntity.ok(peerDialer.getActiveAddressList());
    }

    @GetMapping("/{branchId}/buckets")
    public ResponseEntity getBuckets(@PathVariable(name = BRANCH_ID) String branchId) {
        return ResponseEntity.ok(peerTableGroup.getPeerTable(BranchId.of(branchId)).getBucketIdAndPeerList());
    }

    @GetMapping("/{branchId}/buckets/allPeers")
    public ResponseEntity getPeersFromBuckets(@PathVariable(name = BRANCH_ID) String branchId) {
        return ResponseEntity.ok(peerTableGroup.getPeerTable(BranchId.of(branchId)).getAllPeerAddressList());
    }

    @GetMapping("/{branchId}/latestPeers")
    public ResponseEntity getLatestPeers(@PathVariable(name = BRANCH_ID) String branchId,
                                         @RequestParam(value = "reqTime") long reqTime) {
        return ResponseEntity.ok(peerTableGroup.getPeerTable(BranchId.of(branchId)).getLatestPeers(reqTime));
    }
}
