package io.yggdrash.node.api;

import io.yggdrash.core.BranchId;
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
    private static final BranchId BRANCH = BranchId.stem();

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
    public void getPeersTest() {
        when(peerGroup.getPeers(BRANCH)).thenReturn(Collections.singletonList(peer));
        assertThat(peerApi.getPeers(BRANCH.toString()).size()).isEqualTo(1);
    }

    @Test
    public void getAllActivePeerTest() {
        when(peerGroup.getActivePeerList())
                .thenReturn(Collections.singletonList(peer.getYnodeUri()));
        assertThat(peerApi.getAllActivePeer().size()).isEqualTo(1);
    }

    @Test
    public void getPeersRpcTest() {
        try {
            peerApiRpc.getPeers(BRANCH.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
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
