package io.yggdrash.core.net;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class PeerHandlerGroupTest {

    private static final int MAX_PEERS = 25;
    private static final BranchId BRANCH = TestConstants.STEM;
    private static final Peer OWNER = Peer.valueOf("ynode://75bff16c@127.0.0.1:32920");

    private PeerHandlerGroup peerHandlerGroup;

    @Before
    public void setUp() {
        this.peerHandlerGroup = new PeerHandlerGroup(PeerHandlerMock.factory);
        peerHandlerGroup.setPeerEventListener(peer -> {
            assert peer != null;
        });
    }

    /**
     * PeerHandlerMock 은 Pong 응답이 토클 됩니다.
     * 처음에는 정상적으로 Pong이 응답되서 안정적으로 채널에 추가시키기 위함
     * 이후 healthCheck 에서 null이 응답되어 피어 테이블과 채널에서 제거될 수 있게됨
     */
    @Test
    public void healthCheck() {
        Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        peerHandlerGroup.addHandler(OWNER, peer); // Pong 정상응답
        assert !peerHandlerGroup.getActivePeerList().isEmpty();

        peerHandlerGroup.healthCheck(OWNER); // Pong null 응답

        assert peerHandlerGroup.getActivePeerList().isEmpty();
    }

    @Test
    public void syncBlock() {
        addPeerHandler();
        List<BlockHusk> blockHuskList = peerHandlerGroup.syncBlock(BRANCH, 0);
        assert !blockHuskList.isEmpty();
    }

    @Test
    public void syncTransaction() {
        addPeerHandler();
        peerHandlerGroup.receivedTransaction(BlockChainTestUtils.createTransferTxHusk());
        assert !peerHandlerGroup.syncTransaction(BRANCH).isEmpty();
    }

    @Test
    public void destroy() {
        peerHandlerGroup.destroy();
    }

    @Test
    public void chainedBlock() {
        BlockHusk genesis = BlockChainTestUtils.genesisBlock();
        peerHandlerGroup.chainedBlock(BlockChainTestUtils.genesisBlock());
        for (BestBlock bestBlock : OWNER.getBestBlocks()) {
            assert !bestBlock.getBranchId().equals(genesis.getBranchId()) ||
                    bestBlock.getIndex() == genesis.getIndex();
        }
    }

    @Test
    public void getActivePeerListOf() {
        assert peerHandlerGroup.getActivePeerListOf().size() == 0;
    }

    private void addPeerHandler() {
        assert peerHandlerGroup.getActivePeerList().isEmpty();
        peerHandlerGroup.addHandler(OWNER, Peer.valueOf("ynode://75bff16c@127.0.0.1:32918"));
        assert !peerHandlerGroup.getActivePeerList().isEmpty();
    }
}