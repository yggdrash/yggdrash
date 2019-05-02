/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.p2p;

import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.core.store.StoreBuilder;

import java.util.List;

public class PeerTableGroupBuilder {

    private Peer owner;
    private StoreBuilder storeBuilder;
    private PeerDialer peerDialer;
    private List<String> seedPeerList;
    private String type = null;

    public PeerTableGroupBuilder setOwner(Peer owner) {
        this.owner = owner;
        return this;
    }

    public PeerTableGroupBuilder setStoreBuilder(StoreBuilder storeBuilder) {
        this.storeBuilder = storeBuilder;
        return this;
    }

    public PeerTableGroupBuilder setPeerDialer(PeerDialer peerDialer) {
        this.peerDialer = peerDialer;
        return this;
    }

    public PeerTableGroupBuilder setSeedPeerList(List<String> seedPeerList) {
        this.seedPeerList = seedPeerList;
        return this;
    }

    public void setType(String type) {
        this.type = type;
    }

    public PeerTableGroup build() {
        PeerTableGroup peerTableGroup;
        if ("dht".equals(type)) {
            throw new FailedOperationException("Not implemented");
        } else {
            peerTableGroup = new KademliaPeerTableGroup(owner, storeBuilder, peerDialer);
        }
        peerDialer.setPeerEventListener(peerTableGroup);
        if (seedPeerList != null) {
            peerTableGroup.setSeedPeerList(seedPeerList);
        }
        return peerTableGroup;
    }

    public static PeerTableGroupBuilder newBuilder() {
        return new PeerTableGroupBuilder();
    }

}
