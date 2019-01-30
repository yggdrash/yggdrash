package io.yggdrash.core.net;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.blockchain.BlockHusk;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class SimplePeerHandlerGroupTest {

    private static final Peer OWNER = Peer.valueOf("ynode://75bff16c@127.0.0.1:32920");
    private static final Peer TARGET = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
    private PeerHandlerGroup peerHandlerGroup;

    @Before
    public void setUp() {
        this.peerHandlerGroup = new SimplePeerHandlerGroup(PeerHandlerMock.factory);
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
        healthCheckForAddHandler();
        peerHandlerGroup.healthCheck(OWNER, Collections.singletonList(TARGET)); // Pong null 응답
        assert peerHandlerGroup.getActivePeerList().isEmpty();
    }

    @Test
    public void destroyAll() {
        healthCheckForAddHandler();
        peerHandlerGroup.destroyAll();
    }

    @Test
    public void chainedBlock() {
        BlockHusk genesis = BlockChainTestUtils.genesisBlock();
        peerHandlerGroup.chainedBlock(BlockChainTestUtils.genesisBlock());
        for (BestBlock bestBlock : OWNER.getBestBlocks()) {
            assert !bestBlock.getBranchId().equals(genesis.getBranchId())
                    || bestBlock.getIndex() == genesis.getIndex();
        }
    }

    @Test
    public void getActivePeerListOf() {
        assert peerHandlerGroup.getActiveAddressList().size() == 0;
    }

    private void healthCheckForAddHandler() {
        assert peerHandlerGroup.getActivePeerList().isEmpty();
        peerHandlerGroup.healthCheck(OWNER, Collections.singletonList(TARGET));
        assert !peerHandlerGroup.getActivePeerList().isEmpty();
    }
}