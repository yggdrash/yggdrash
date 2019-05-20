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

package io.yggdrash.core.store;

import io.yggdrash.StoreTestUtils;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.common.store.datasource.LevelDbDataSource;
import io.yggdrash.core.p2p.Peer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PeerStoreTest {
    private PeerStore peerStore;

    @Before
    public void setUp() {
        peerStore = new PeerStore(new HashMapDbSource());
    }

    @AfterClass
    public static void destroy() {
        StoreTestUtils.clearTestDb();
    }

    @Test
    public void shouldBeGotPeer() {
        peerStore = new PeerStore(new LevelDbDataSource(StoreTestUtils.getTestPath(), "peers"));

        Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        peerStore.put(peer.getPeerId(), peer);

        assertThat(peerStore.contains(peer.getPeerId())).isTrue();

        Peer foundPeer = peerStore.get(peer.getPeerId());
        assertThat(foundPeer).isEqualTo(peer);
    }

    @Test
    public void shouldBeGotAllPeer() {
        Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        peerStore.put(peer.getPeerId(), peer);
        assertThat(peerStore.getAll().size()).isEqualTo(1);

        // add exist peer
        peerStore.put(peer.getPeerId(), peer);
        assertThat(peerStore.getAll().size()).isEqualTo(1);
    }


    @Test
    public void shouldBeRemovedPeer() {
        // arrange
        Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        peerStore.put(peer.getPeerId(), peer);
        assertThat(peerStore.size()).isEqualTo(1);
        assertThat(peerStore.getAll().size()).isEqualTo(peerStore.size());
        // act
        peerStore.remove(peer.getPeerId());
        // assert
        assertThat(peerStore.size()).isEqualTo(0);
        assertThat(peerStore.getAll().size()).isEqualTo(peerStore.size());
    }

    @Test
    public void overwrite() {
        Peer p1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        Peer p2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32919");
        peerStore.put(p1.getPeerId(), p1);
        peerStore.put(p2.getPeerId(), p2);

        assertEquals(2, peerStore.getAll().size());
        assertTrue(peerStore.contains(p1.getPeerId()));
        assertTrue(peerStore.contains(p2.getPeerId()));

        Peer p3 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32920");
        Peer p4 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32921");
        Peer p5 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32922");
        List<Peer> peerList = new ArrayList<>();
        peerList.add(p3);
        peerList.add(p4);
        peerList.add(p5);
        peerStore.overwrite(peerList);

        assertEquals(3, peerStore.getAll().size());
        assertFalse(peerStore.contains(p1.getPeerId()));
        assertFalse(peerStore.contains(p2.getPeerId()));
        assertTrue(peerStore.contains(p3.getPeerId()));
        assertTrue(peerStore.contains(p4.getPeerId()));
        assertTrue(peerStore.contains(p5.getPeerId()));
    }
}