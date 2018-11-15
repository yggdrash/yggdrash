package io.yggdrash.core.net;

import io.yggdrash.TestUtils;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.store.StoreBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PeerGroupTest {

    private static final int MAX_PEERS = 25;
    private static final BranchId BRANCH = TestUtils.STEM;
    private static final BranchId OTHER_BRANCH = TestUtils.YEED;
    private static final Peer OWNER = Peer.valueOf("ynode://75bff16c@127.0.0.1:32920");
    private static final StoreBuilder storeBuilder = new StoreBuilder(new DefaultConfig());

    private PeerGroup peerGroup;
    private TransactionHusk tx;

    @Before
    public void setUp() {
        this.peerGroup = new PeerGroup(OWNER, MAX_PEERS);
        this.tx = TestUtils.createTransferTxHusk();
        peerGroup.addPeerTable(BRANCH, storeBuilder.buildPeerStore(BRANCH));
        peerGroup.addPeerTable(OTHER_BRANCH, storeBuilder.buildPeerStore(OTHER_BRANCH));
    }

    @Test
    public void addPeerTest() {
        assert !peerGroup.isPeerEmpty(BRANCH);
        assert peerGroup.getPeerTable(BRANCH).getPeersCount() == 1;
        peerGroup.addPeer(BRANCH, Peer.valueOf("ynode://75bff16c@127.0.0.1:32918"));
        peerGroup.addPeer(BRANCH, Peer.valueOf("ynode://75bff16c@127.0.0.1:32919"));
        peerGroup.addPeer(OTHER_BRANCH, Peer.valueOf("ynode://75bff16c@127.0.0.1:32918"));
        assert peerGroup.count(BRANCH) == 3; // addPeer 시 owner 추가됨
        assert peerGroup.count(OTHER_BRANCH) == 2;
        assert !peerGroup.getPeers(BRANCH, OWNER).isEmpty();
        assert !peerGroup.isPeerEmpty(BRANCH);
    }

    @Test
    public void getPeerTest() {
        Peer requester = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        Collection<String> peerListWithoutRequester = peerGroup.getPeers(BRANCH, requester);
        assert peerListWithoutRequester.isEmpty();
        // requester 가 peer 목록 조회 후에는 peerTable 에 등록되어 있다
        assert peerGroup.containsPeer(BRANCH, requester);
    }

    @Test
    public void addPeerByYnodeUriTest() {
        assert !peerGroup.isPeerEmpty(BRANCH);
        assert peerGroup.getPeerTable(BRANCH).getPeersCount() == 1;
        peerGroup.addPeerByYnodeUri(BRANCH, "ynode://75bff16c@127.0.0.1:32918");
        peerGroup.addPeerByYnodeUri(OTHER_BRANCH, "ynode://75bff16c@127.0.0.1:32918");
        assert peerGroup.count(BRANCH) == 2;
        assert peerGroup.count(OTHER_BRANCH) == 2;
        peerGroup.addPeerByYnodeUri(BRANCH,
                Collections.singletonList("ynode://75bff16c@127.0.0.1:32919"));
        peerGroup.addPeerByYnodeUri(OTHER_BRANCH,
                Collections.singletonList("ynode://75bff16c@127.0.0.1:32919"));
        assert peerGroup.count(BRANCH) == 3;
        assert peerGroup.count(OTHER_BRANCH) == 3;
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

    /**
     * ChannelMock 은 Pong 응답이 토클 됩니다.
     * 처음에는 정상적으로 Pong이 응답되서 안정적으로 채널에 추가시키기 위함
     * 이후 healthCheck 에서 null이 응답되어 피어 테이블과 채널에서 제거될 수 있게됨
     */
    @Test
    public void healthCheck() {
        PeerClientChannel peerClientChannel = ChannelMock.dummy();

        peerGroup.newPeerChannel(BRANCH, peerClientChannel); // Pong 정상응답
        assert !peerGroup.isChannelEmpty(BRANCH);

        peerGroup.addPeer(BRANCH, peerClientChannel.getPeer());
        assert peerGroup.containsPeer(BRANCH, peerClientChannel.getPeer());

        peerGroup.healthCheck(); // Pong null 응답

        assert peerGroup.isChannelEmpty(BRANCH);
        assert !peerGroup.containsPeer(BRANCH, peerClientChannel.getPeer());
    }

    @Test
    public void syncBlock() {
        addPeerChannel();
        List<BlockHusk> blockHuskList = peerGroup.syncBlock(BRANCH, 0);
        assert !blockHuskList.isEmpty();
    }

    @Test
    public void syncTransaction() {
        addPeerChannel();
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

    private void addPeerChannel() {
        assert peerGroup.isChannelEmpty(BRANCH);
        peerGroup.newPeerChannel(BRANCH, ChannelMock.dummy());
        assert !peerGroup.isChannelEmpty(BRANCH);
    }
}