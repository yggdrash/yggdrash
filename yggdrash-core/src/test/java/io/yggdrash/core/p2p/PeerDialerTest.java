package io.yggdrash.core.p2p;

import io.yggdrash.TestConstants;
import org.junit.Before;
import org.junit.Test;

public class PeerDialerTest {

    private static final Peer OWNER = Peer.valueOf("ynode://75bff16c@127.0.0.1:32920");
    private static final Peer TARGET = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
    private PeerDialer peerDialer;

    @Before
    public void setUp() {
        this.peerDialer = new SimplePeerDialer(PeerHandlerMock.factory);
        peerDialer.setPeerEventListener(peer -> {
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
        peerDialer.healthCheck(TestConstants.yggdrash(), OWNER, TARGET); // Pong null 응답
        assert peerDialer.getActivePeerList().isEmpty();
    }

    @Test
    public void destroyAll() {
        healthCheckForAddHandler();
        peerDialer.destroyAll();
    }

    @Test
    public void getActivePeerListOf() {
        assert peerDialer.getActiveAddressList().size() == 0;
    }

    private void healthCheckForAddHandler() {
        assert peerDialer.getActivePeerList().isEmpty();
        peerDialer.healthCheck(TestConstants.yggdrash(), OWNER, TARGET);
        assert !peerDialer.getActivePeerList().isEmpty();
    }
}