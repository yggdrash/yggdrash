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

package io.yggdrash;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.p2p.BlockChainDialer;
import io.yggdrash.core.p2p.KademliaPeerTable;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerDialer;
import io.yggdrash.core.p2p.PeerHandlerMock;
import io.yggdrash.core.p2p.PeerTableGroup;
import io.yggdrash.core.p2p.PeerTableGroupBuilder;
import io.yggdrash.core.store.StoreBuilder;

import java.util.Collections;
import java.util.List;

public class PeerTestUtils {
    public static final int SEED_PORT = 32918;
    public static final int OWNER_PORT = 32920;
    public static final String NODE_URI_PREFIX = "ynode://75bff16c@127.0.0.1:";
    private static final StoreBuilder storeBuilder = StoreBuilder.newBuilder().setConfig(new DefaultConfig());

    private PeerTestUtils() {}

    public static PeerTableGroup createTableGroup() {
        return createTableGroup(OWNER_PORT, new BlockChainDialer(PeerHandlerMock.factory));
    }

    public static PeerTableGroup createTableGroup(int port, PeerDialer peerDialer) {
        return createTableGroup(port, peerDialer, Collections.singletonList(NODE_URI_PREFIX + SEED_PORT));
    }

    public static PeerTableGroup createTableGroup(int port, PeerDialer peerDialer, List<String> seedPeerList) {
        Peer owner = Peer.valueOf(NODE_URI_PREFIX + port);
        return PeerTableGroupBuilder.newBuilder()
                .setOwner(owner)
                .setStoreBuilder(storeBuilder)
                .setPeerDialer(peerDialer)
                .setSeedPeerList(seedPeerList)
                .build();
    }

    public static KademliaPeerTable createTable() {
        Peer owner = Peer.valueOf(NODE_URI_PREFIX + OWNER_PORT);
        storeBuilder.setBranchId(TestConstants.yggdrash());
        return new KademliaPeerTable(owner, storeBuilder.buildPeerStore());
    }
}
