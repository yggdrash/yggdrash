package io.yggdrash.core.net;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class PeerGroupTest {

    PeerGroup group;

    @Before
    public void setUp() {
        group = new PeerGroup();
    }

    @Test
    public void addPeerTest() {
        assert group.isEmpty();
        group.addPeer(Peer.valueOf("ynode://75bff16c@127.0.0.1:9090"));
        assert group.count() == 1;
        assert !group.getPeers().isEmpty();
        assert !group.isEmpty();
        group.clear();
        assert group.isEmpty();
    }

    @Test
    public void removePeerTest() {
        group.addPeer(Peer.valueOf("ynode://75bff16c@127.0.0.1:9090"));
        assert group.contains("ynode://75bff16c@127.0.0.1:9090");
        assert !group.contains("wrong");
    }

    @Test
    public void getSeedPeerList() {
        assert group.getSeedPeerList() == null;
        group.setSeedPeerList(Collections.singletonList("ynode://75bff16c@127.0.0.1:9090"));
        assert !group.getSeedPeerList().isEmpty();
    }
}