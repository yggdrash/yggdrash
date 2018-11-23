package io.yggdrash.node.api;

import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerGroup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PeerApiMockitoTest {

    private static final PeerApi peerApiRpc = new JsonRpcConfig().peerApi();

    @Mock
    private PeerGroup peerGroup;
    private Peer peer;
    private PeerApiImpl peerApi;

    @Before
    public void setUp() {
        this.peer = Peer.valueOf("ynode://65bff16c@127.0.0.1:32918");
        peerApi = new PeerApiImpl(peerGroup);
    }

    @Test
    public void getAllActivePeerTest() {
        when(peerGroup.getActivePeerList())
                .thenReturn(Collections.singletonList(peer.toString()));
        assertThat(peerApi.getAllActivePeer().size()).isEqualTo(1);
    }

    @Test
    public void getAllActivePeerRpcTest() {
        try {
            peerApiRpc.getAllActivePeer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
