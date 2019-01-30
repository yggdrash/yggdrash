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

package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SimplePeerHandlerGroup implements PeerHandlerGroup {

    private static final Logger log = LoggerFactory.getLogger(SimplePeerHandlerGroup.class);

    private final Map<String, PeerHandler> handlerMap = new ConcurrentHashMap<>();

    private PeerHandlerFactory peerHandlerFactory;

    private PeerEventListener peerEventListener;

    public SimplePeerHandlerGroup(PeerHandlerFactory peerHandlerFactory) {
        this.peerHandlerFactory = peerHandlerFactory;
    }

    @Override
    public void setPeerEventListener(PeerEventListener peerEventListener) {
        this.peerEventListener = peerEventListener;
    }

    @Override
    public void destroyAll() {
        handlerMap.values().forEach(PeerHandler::stop);
    }

    @Override
    public boolean healthCheck(Peer owner, List<Peer> closestPeerList) {
        if (closestPeerList.isEmpty()) {
            log.trace("Closest peer is empty to health check peer");
            return false;
        }

        boolean success = true;
        for (Peer to : closestPeerList) {
            PeerHandler peerHandler = handlerMap.get(to.toAddress());
            if (peerHandler == null) {
                peerHandler = peerHandlerFactory.create(to);
                handlerMap.put(to.toAddress(), peerHandler);
                log.info("Added size={}, handler={}", handlerCount(), to.toAddress());
            }
            if (!play(owner, peerHandler)) {
                success = false;
                log.warn("Health check fail. peer={}", to.toAddress());
                removeHandler(peerHandler);
            }
        }
        return success;
    }

    private boolean play(Peer owner, PeerHandler peerHandler) {
        try {
            String pong = peerHandler.ping(owner, "Ping");
            // TODO validation peer
            if ("Pong".equals(pong)) {
                return true;
            }
        } catch (Exception e) {
            log.warn("Fail to add to the peer handler err=" + e.getMessage());
        }
        return false;
    }

    @Override
    public void receivedTransaction(TransactionHusk tx) {
        if (handlerMap.isEmpty()) {
            log.trace("Active peer is empty to broadcast transaction");
            return;
        }
        for (PeerHandler peerHandler : handlerMap.values()) {
            try {
                peerHandler.broadcastTransaction(tx);
            } catch (Exception e) {
                removeHandler(peerHandler);
            }
        }
    }

    @Override
    public void chainedBlock(BlockHusk block) {
        if (handlerMap.isEmpty()) {
            log.trace("Active peer is empty to broadcast block");
            return;
        }
        for (PeerHandler peerHandler : handlerMap.values()) {
            try {
                peerHandler.broadcastBlock(block);
            } catch (Exception e) {
                removeHandler(peerHandler);
            }
        }
    }

    private void removeHandler(PeerHandler peerHandler) {
        peerHandler.stop();
        handlerMap.remove(peerHandler.getPeer().toAddress());
        if (peerEventListener != null) {
            peerEventListener.peerDisconnected(peerHandler.getPeer());
        }
        log.debug("Removed handler size={}", handlerCount());
    }

    @Override
    public int handlerCount() {
        return handlerMap.size();
    }

    @Override
    public List<String> getActivePeerList() {
        return handlerMap.values().stream()
                .map(handler -> handler.getPeer().toString())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getActiveAddressList() {
        return new ArrayList<>(handlerMap.keySet());
    }

    @Override
    public List<PeerHandler> getHandlerList(BranchId branchId) {
        if (handlerMap.isEmpty()) {
            log.trace("Active peer is empty.");
            return Collections.emptyList();
        }
        // TODO sync peer selection policy
        String key = (String) handlerMap.keySet().toArray()[0];
        PeerHandler peerHandler = handlerMap.get(key);
        return Collections.singletonList(peerHandler);
    }
}
