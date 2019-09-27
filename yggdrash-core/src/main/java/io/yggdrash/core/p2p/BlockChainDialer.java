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

package io.yggdrash.core.p2p;

import io.yggdrash.core.blockchain.BranchId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BlockChainDialer implements PeerDialer {

    private static final Logger log = LoggerFactory.getLogger(BlockChainDialer.class);

    private final Map<String, BlockChainHandler> handlerMap = new ConcurrentHashMap<>();

    private final Map<BranchId, String> consensusMap = new HashMap<>();

    private final BlockChainHandlerFactory peerHandlerFactory;

    private PeerEventListener peerEventListener;

    public BlockChainDialer(BlockChainHandlerFactory peerHandlerFactory) {
        this.peerHandlerFactory = peerHandlerFactory;
    }

    @Override
    public void setPeerEventListener(PeerEventListener peerEventListener) {
        this.peerEventListener = peerEventListener;
    }

    @Override
    public void destroyAll() {
        handlerMap.values().forEach(BlockChainHandler::stop);
        handlerMap.clear();
        consensusMap.clear();
    }

    @Override
    public void addConsensus(BranchId branchId, String consensus) {
        consensusMap.put(branchId, consensus);
    }

    @Override
    public long healthCheck(BranchId branchId, Peer owner, Peer to) {
        BlockChainHandler peerHandler = getPeerHandler(branchId, to);
        try {
            long peerBlockIndex = peerHandler.pingPong(branchId, owner, "Ping");
            if (peerBlockIndex >= 0) {
                return peerBlockIndex;
            }
        } catch (Exception e) {
            log.trace("healthCheck failed {} {}", to.toAddress(), e.getMessage());
        }

        return -1L;
    }

    @Override
    public void removeHandler(BlockChainHandler peerHandler) {
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
    public Map<String, String> getActivePeerListWithStatus() {
        return handlerMap.values().stream()
                .collect(Collectors.toMap(
                        h -> h.getPeer().toString(), BlockChainHandler::gerConnectivityState, (a, b) -> b));
    }

    @Override
    public List<String> getActiveAddressList() {
        return new ArrayList<>(handlerMap.keySet());
    }

    @Override
    public List<BlockChainHandler> getHandlerList(BranchId branchId, List<Peer> peerList) {
        if (peerList == null || peerList.isEmpty()) {
            return Collections.emptyList();
        }
        List<BlockChainHandler> handlerList = new ArrayList<>();
        for (Peer peer : peerList) {
            BlockChainHandler handler = getPeerHandler(branchId, peer);
            handlerList.add(handler);
        }
        return handlerList;
    }

    @Override
    public synchronized BlockChainHandler getPeerHandler(BranchId branchId, Peer peer) {
        BlockChainHandler peerHandler = handlerMap.get(peer.toAddress());
        if (peerHandler != null) {
            return peerHandler;
        }

        if (!consensusMap.containsKey(branchId)) {
            // BS Node only
            peerHandler = peerHandlerFactory.create("discovery", peer);
        } else {
            peerHandler = peerHandlerFactory.create(consensusMap.get(branchId), peer);
        }
        handlerMap.put(peer.toAddress(), peerHandler);
        log.debug("Added size={}, id={}, handler={}", handlerCount(), peerHandler, peer.getYnodeUri());

        return peerHandler;
    }
}
