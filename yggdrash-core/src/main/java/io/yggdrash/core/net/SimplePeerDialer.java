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

import io.yggdrash.core.blockchain.BranchId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SimplePeerDialer implements PeerDialer {

    private static final Logger log = LoggerFactory.getLogger(SimplePeerDialer.class);

    private final Map<String, PeerHandler> handlerMap = new ConcurrentHashMap<>();

    private PeerHandlerFactory peerHandlerFactory;

    private PeerEventListener peerEventListener;

    public SimplePeerDialer(PeerHandlerFactory peerHandlerFactory) {
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
    public boolean healthCheck(BranchId branchId, Peer owner, Peer to) {
        PeerHandler peerHandler = getPeerHandler(to);
        try {
            String pong = peerHandler.ping(branchId, owner, "Ping");
            // TODO validation peer and considering expiration
            if ("Pong".equals(pong)) {
                return true;
            }
        } catch (Exception e) {
            log.warn("Fail to add to the peer handler err=" + e.getMessage());
        }
        removeHandler(peerHandler);
        return false;
    }

    @Override
    public void removeHandler(PeerHandler peerHandler) {
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
    public List<PeerHandler> getHandlerList(List<Peer> peerList) {
        if (handlerMap.isEmpty()) {
            log.trace("Active peer is empty.");
            return Collections.emptyList();
        }

        if (peerList == null || peerList.isEmpty()) {
            return Collections.emptyList();
        }
        List<PeerHandler> handlerList = new ArrayList<>(peerList.size());
        for (Peer peer : peerList) {
            PeerHandler handler = handlerMap.get(peer.toAddress());
            if (handler != null) {
                handlerList.add(handler);
            }
        }
        return handlerList;
    }

    @Override
    public synchronized PeerHandler getPeerHandler(Peer peer) {
        PeerHandler peerHandler = handlerMap.get(peer.toAddress());
        if (peerHandler == null) {
            peerHandler = peerHandlerFactory.create(peer);
            handlerMap.put(peer.toAddress(), peerHandler);
            log.debug("Added size={}, id={}, handler={}", handlerCount(), peerHandler, peer.toAddress());
        }
        return peerHandler;
    }
}
