package io.yggdrash.core.akashic;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerHandlerGroup;
import io.yggdrash.core.net.PeerHandlerMock;
import io.yggdrash.core.net.SimplePeerHandlerGroup;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleSyncManagerTest {

    private static final Peer OWNER = Peer.valueOf("ynode://75bff16c@127.0.0.1:32920");

    private BranchGroup branchGroup;
    private PeerHandlerGroup peerHandlerGroup;

    @Before
    public void setUp() {
        this.branchGroup = BlockChainTestUtils.createBranchGroup();
        this.peerHandlerGroup = new SimplePeerHandlerGroup(PeerHandlerMock.factory);
        peerHandlerGroup.setPeerEventListener(peer -> {
            assert peer != null;
        });
        peerHandlerGroup.addHandler(OWNER, OWNER);
        assert peerHandlerGroup.handlerCount() == 1;
    }

    @Test
    public void syncBlockAndTransaction() {
        // arrange
        BranchId branchId = (BranchId)branchGroup.getAllBranchId().toArray()[0];
        SimpleSyncManager syncManager = new SimpleSyncManager(branchGroup, peerHandlerGroup);

        // act
        syncManager.syncBlockAndTransaction();

        // assert
        assertThat(branchGroup.getLastIndex(branchId)).isEqualTo(1);
        assertThat(branchGroup.getUnconfirmedTxs(branchId)).isNotEmpty();
    }
}