package io.yggdrash.core.net;

import io.yggdrash.TestUtils;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.mock.ChannelMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class PeerGroupTest {
    private static final Logger log = LoggerFactory.getLogger(BlockStore.class);

    private static final int MAX_PEERS = 25;

    private PeerGroup peerGroup;
    private TransactionHusk tx;
    private BlockHusk block;

    @Before
    public void setUp() {
        this.peerGroup = new PeerGroup(MAX_PEERS);
        this.tx = TestUtils.createTransferTxHusk();
        this.block = TestUtils.createGenesisBlockHusk();
        peerGroup.setListener(peer -> log.debug(peer.getYnodeUri() + " disconnected"));
        peerGroup.newPeerChannel(ChannelMock.dummy());
    }

    @Test
    public void addPeerTest() {
        assert peerGroup.isEmpty();
        peerGroup.addPeer(Peer.valueOf("ynode://75bff16c@127.0.0.1:32918"));
        assert peerGroup.count() == 1;
        assert !peerGroup.getPeers().isEmpty();
        assert !peerGroup.isEmpty();
        peerGroup.clear();
        assert peerGroup.isEmpty();
    }

    @Test
    public void addPeerByYnodeUriTest() {
        assert peerGroup.isEmpty();
        peerGroup.addPeerByYnodeUri("ynode://75bff16c@127.0.0.1:32918");
        assert peerGroup.count() == 1;
        peerGroup.addPeerByYnodeUri(Collections.singletonList("ynode://75bff16c@127.0.0.1:32919"));
        assert peerGroup.count() == 2;
    }

    @Test
    public void addMaxPeerTest() {
        int testCount = MAX_PEERS + 5;
        for (int i = 0; i < testCount; i++) {
            int port = i + 32918;
            peerGroup.addPeerByYnodeUri("ynode://75bff16c@localhost:" + port);
        }
        assert MAX_PEERS == peerGroup.getPeers().size();
    }

    @Test
    public void removePeerTest() {
        peerGroup.addPeer(Peer.valueOf("ynode://75bff16c@127.0.0.1:32918"));
        assert peerGroup.contains("ynode://75bff16c@127.0.0.1:32918");
    }

    @Test
    public void getSeedPeerList() {
        assert peerGroup.getSeedPeerList() == null;
        peerGroup.setSeedPeerList(Collections.singletonList("ynode://75bff16c@127.0.0.1:32918"));
        assert !peerGroup.getSeedPeerList().isEmpty();
    }

    @Test
    public void getPeerUriListTest() {
        assert peerGroup.getPeerUriList().isEmpty();
        peerGroup.addPeer(Peer.valueOf("ynode://75bff16c@127.0.0.1:32918"));
        assert peerGroup.getPeerUriList().contains("ynode://75bff16c@127.0.0.1:32918");
    }

    @Test
    public void healthCheck() {
        peerGroup.healthCheck();
        assert !peerGroup.getActivePeerList().isEmpty();
    }

    @Test
    public void syncBlock() {
        peerGroup.chainedBlock(block);
        assert !peerGroup.syncBlock(BranchId.stem(), 0).isEmpty();
    }

    @Test
    public void syncTransaction() {
        peerGroup.receivedTransaction(tx);
        assert !peerGroup.syncTransaction(BranchId.stem()).isEmpty();
    }

    @Test
    public void addActivePeer() {
        int testCount = MAX_PEERS + 5;
        for (int i = 0; i < testCount; i++) {
            int port = i + 32918;
            ChannelMock channel = new ChannelMock("ynode://75bff16c@localhost:" + port);
            peerGroup.newPeerChannel(channel);
        }
        assert MAX_PEERS == peerGroup.getActivePeerList().size();
    }
}