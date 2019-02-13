/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.node.discovery;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.net.KademliaOptions;
import io.yggdrash.core.net.KademliaPeerTable;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerHandlerFactory;
import io.yggdrash.core.net.PeerHandlerMock;
import io.yggdrash.core.store.StoreBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class KademliaDhtTest {
    private static final String NODE_URI_PREFIX = "ynode://75bff16c@127.0.0.1:";
    private static final Peer OWNER = Peer.valueOf(NODE_URI_PREFIX + 32920);
    private static final Peer SEED = Peer.valueOf(NODE_URI_PREFIX + 32918);
    private static final Peer TARGET = Peer.valueOf(NODE_URI_PREFIX + 32933);
    private static final StoreBuilder storeBuilder = new StoreBuilder(new DefaultConfig());

    private KademliaPeerTable kademliaPeerTable;

    @Before
    public void setUp() {
        KademliaOptions.BUCKET_SIZE = 5;

        PeerHandlerFactory peerHandlerFactory = PeerHandlerMock.factory;
        kademliaPeerTable = new KademliaPeerTable(
                OWNER, storeBuilder.buildPeerStore(), peerHandlerFactory);
        List<String> seedList = Collections.singletonList(SEED.getYnodeUri());
        kademliaPeerTable.setSeedPeerList(seedList);
    }

    /*
    6 peers will be returned after selfRefresh
    11 peers are existed on peerTable including peer and owner
    */
    @Test
    public void selfRefresh() {
        kademliaPeerTable.selfRefresh();
        Assert.assertEquals(kademliaPeerTable.getAllPeerAddressList().size(), 8);
    }

    /*
    Try refresh with TARGET
    The size of closestPeers will be 0, so selfRefresh will be executed.
    selfLookup is done after loadSeedNodes.
    lookup will proceed with the target after selfRefresh done.
    A total of 13 peers will be existed in the bucket which includes 6 peers received after lookup,
    5 peers received after lookup by target, owner, and seed.
     */
    @Test
    public void refresh() {
        kademliaPeerTable.refresh(TARGET);
        Assert.assertEquals(kademliaPeerTable.getAllPeerAddressList().size(), 13);
    }
}
