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
import io.yggdrash.core.net.KademliaPeerNetwork;
import io.yggdrash.core.net.KademliaPeerTable;
import io.yggdrash.core.net.KademliaPeerTableGroup;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerHandlerFactory;
import io.yggdrash.core.net.PeerHandlerGroup;
import io.yggdrash.core.net.PeerHandlerMock;
import io.yggdrash.core.net.SimplePeerHandlerGroup;
import io.yggdrash.core.store.StoreBuilder;

import java.util.Collections;
import java.util.List;

public class PeerTestUtils {
    public static final int SEED_PORT = 32918;
    private static final int OWNER_PORT = 32920;
    private static final String NODE_URI_PREFIX = "ynode://75bff16c@127.0.0.1:";
    private static final StoreBuilder storeBuilder = new StoreBuilder(new DefaultConfig());

    private PeerTestUtils() {}

    public static KademliaPeerNetwork createNetwork() {
        PeerHandlerGroup peerHandlerGroup = new SimplePeerHandlerGroup(PeerHandlerMock.factory);
        return new KademliaPeerNetwork(createTableGroup(), peerHandlerGroup);
    }

    public static KademliaPeerTableGroup createTableGroup() {
        return createTableGroup(OWNER_PORT, PeerHandlerMock.factory);
    }

    public static KademliaPeerTableGroup createTableGroup(int port, PeerHandlerFactory factory) {
        Peer owner = Peer.valueOf(NODE_URI_PREFIX + port);
        KademliaPeerTableGroup tableGroup = new KademliaPeerTableGroup(owner, storeBuilder, factory);
        List<String> seedList = Collections.singletonList(NODE_URI_PREFIX + SEED_PORT);
        tableGroup.setSeedPeerList(seedList);
        return tableGroup;
    }

    public static KademliaPeerTable createTable() {
        Peer owner = Peer.valueOf(NODE_URI_PREFIX + OWNER_PORT);
        return new KademliaPeerTable(owner, storeBuilder.buildPeerStore(TestConstants.yggdrash()));
    }
}
