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
import java.util.List;

public class PeerGroupTest {
    private static final Logger log = LoggerFactory.getLogger(BlockStore.class);

    private static final int MAX_PEERS = 25;
    private static final BranchId BRANCH = BranchId.stem();
    private static final BranchId OTHER_BRANCH = BranchId.yeed();

    private PeerGroup peerGroup;
    private TransactionHusk tx;

    @Before
    public void setUp() {
        this.peerGroup = new PeerGroup(MAX_PEERS);
        this.tx = TestUtils.createTransferTxHusk();
        peerGroup.newPeerChannel(BRANCH, ChannelMock.dummy());
    }

    @Test
    public void addPeerTest() {
        assert peerGroup.isEmpty(BRANCH);
        peerGroup.addPeer(BRANCH, Peer.valueOf("ynode://75bff16c@127.0.0.1:32918"));
        peerGroup.addPeer(BRANCH, Peer.valueOf("ynode://75bff16c@127.0.0.1:32919"));
        peerGroup.addPeer(OTHER_BRANCH, Peer.valueOf("ynode://75bff16c@127.0.0.1:32918"));
        assert peerGroup.count(BRANCH) == 2;
        assert peerGroup.count(OTHER_BRANCH) == 1;
        assert !peerGroup.getPeers(BRANCH).isEmpty();
        assert !peerGroup.isEmpty(BRANCH);
    }

    @Test
    public void addPeerByYnodeUriTest() {
        assert peerGroup.isEmpty(BRANCH);
        peerGroup.addPeerByYnodeUri(BRANCH, "ynode://75bff16c@127.0.0.1:32918");
        peerGroup.addPeerByYnodeUri(OTHER_BRANCH, "ynode://75bff16c@127.0.0.1:32918");
        assert peerGroup.count(BRANCH) == 1;
        assert peerGroup.count(OTHER_BRANCH) == 1;
        peerGroup.addPeerByYnodeUri(BRANCH,
                Collections.singletonList("ynode://75bff16c@127.0.0.1:32919"));
        peerGroup.addPeerByYnodeUri(OTHER_BRANCH,
                Collections.singletonList("ynode://75bff16c@127.0.0.1:32919"));
        assert peerGroup.count(BRANCH) == 2;
        assert peerGroup.count(OTHER_BRANCH) == 2;
    }

    @Test
    public void addMaxPeerTest() {
        int testCount = MAX_PEERS + 5;
        for (int i = 0; i < testCount; i++) {
            int port = i + 32918;
            peerGroup.addPeerByYnodeUri(BRANCH, "ynode://75bff16c@localhost:" + port);
            peerGroup.addPeerByYnodeUri(OTHER_BRANCH, "ynode://75bff16c@localhost:" + port);
        }
        assert MAX_PEERS == peerGroup.getPeers(BRANCH).size();
        assert MAX_PEERS == peerGroup.getPeers(OTHER_BRANCH).size();
    }

    @Test
    public void getSeedPeerList() {
        assert peerGroup.getSeedPeerList() == null;
        peerGroup.setSeedPeerList(Collections.singletonList("ynode://75bff16c@127.0.0.1:8080"));
        assert !peerGroup.getSeedPeerList().isEmpty();
    }

    @Test
    public void getPeerUriListTest() {
        assert peerGroup.getPeerUriList(BRANCH).isEmpty();
        assert peerGroup.getPeerUriList(OTHER_BRANCH).isEmpty();
        peerGroup.addPeer(BRANCH, Peer.valueOf("ynode://75bff16c@127.0.0.1:32918"));
        peerGroup.addPeer(OTHER_BRANCH, Peer.valueOf("ynode://75bff16c@127.0.0.1:32918"));
        assert peerGroup.getPeerUriList(BRANCH).contains("ynode://75bff16c@127.0.0.1:32918");
        assert peerGroup.getPeerUriList(OTHER_BRANCH).contains("ynode://75bff16c@127.0.0.1:32918");
    }

    @Test
    public void healthCheck() {
        peerGroup.healthCheck();
        assert !peerGroup.getActivePeerList().isEmpty();
    }

    @Test
    public void syncBlock() {
        List<BlockHusk> blockHuskList = peerGroup.syncBlock(BRANCH, 0);
        assert !blockHuskList.isEmpty();
    }

    @Test
    public void syncTransaction() {
        peerGroup.receivedTransaction(tx);
        assert !peerGroup.syncTransaction(BRANCH).isEmpty();
    }

    @Test
    public void addActivePeer() {
        int testCount = MAX_PEERS + 5;
        for (int i = 0; i < testCount; i++) {
            int port = i + 32918;
            ChannelMock channel = new ChannelMock("ynode://75bff16c@localhost:" + port);
            peerGroup.newPeerChannel(BRANCH, channel);
        }
        assert MAX_PEERS == peerGroup.getActivePeerList().size();
    }
}