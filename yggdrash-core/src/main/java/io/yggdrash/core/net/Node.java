/*
 * Copyright 2019 Akashic Foundation
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

import io.yggdrash.core.akashic.SyncManager;

public abstract class Node implements BootStrap {

    protected PeerListener peerListener;
    protected PeerHandlerGroup peerHandlerGroup;
    protected SyncManager syncManager;

    public void bootstrapping(Discovery discovery, int maxPeer) {
        PeerTable peerTable = discovery.discover(peerHandlerGroup.getPeerHandlerFactory());
        for (Peer peer : peerTable.getClosestPeers(peerTable.getOwner(), maxPeer)) {
            if (peerHandlerGroup.handlerCount() >= maxPeer) {
                break;
            }
            peerHandlerGroup.addHandler(peerTable.getOwner(), peer);
        }
    }

    public void setPeerListener(PeerListener peerListener) {
        this.peerListener = peerListener;
    }

    public void setPeerHandlerGroup(PeerHandlerGroup peerHandlerGroup) {
        this.peerHandlerGroup = peerHandlerGroup;
    }

    public void setSyncManager(SyncManager syncManager) {
        this.syncManager = syncManager;
    }

    public void start(String host, int port) {
        peerListener.start(host, port);
    }

    /**
     * Stop serving requests and shutdown resources.
     */
    public void stop() {
        peerListener.stop();
    }

}
