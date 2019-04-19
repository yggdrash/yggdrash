package io.yggdrash.node.api;

import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerDialer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static io.yggdrash.node.api.JsonRpcConfig.PEER_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PeerApiMockitoTest {

    @Mock
    private PeerDialer peerDialer;
    private Peer peer;
    private PeerApiImpl peerApi;

    @Before
    public void setUp() {
        this.peer = Peer.valueOf("ynode://65bff16c@127.0.0.1:32918");
        peerApi = new PeerApiImpl(peerDialer);
    }

    @Test
    public void getAllActivePeerTest() {
        when(peerDialer.getActivePeerList())
                .thenReturn(Collections.singletonList(peer.toString()));
        assertThat(peerApi.getAllActivePeer().size()).isEqualTo(1);
    }

    @Test
    public void getAllActivePeerRpcTest() {
        try {
            Assert.assertFalse(PEER_API.getAllActivePeer().isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
