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

import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchEventListener;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerDialer;
import io.yggdrash.core.p2p.PeerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ValidatorBlockBroadcaster implements BranchEventListener {
    private static final Logger log = LoggerFactory.getLogger(ValidatorBlockBroadcaster.class);

    private final PeerDialer peerDialer;
    private final List<Peer> broadcastPeerList;

    public ValidatorBlockBroadcaster(PeerDialer peerDialer, List<Peer> broadcastPeerList) {
        this.peerDialer = peerDialer;
        this.broadcastPeerList = broadcastPeerList;
        log.debug("Broadcast peerSize={}", broadcastPeerList.size());
    }

    @Override
    public void chainedBlock(BlockHusk block) {

        for (Peer peer : broadcastPeerList) {
            PeerHandler peerHandler = peerDialer.getPeerHandler(peer);
            peerHandler.broadcastBlock(block);
        }
    }

    @Override
    public void receivedTransaction(TransactionHusk tx) {
        // ignore handle tx
    }
}
