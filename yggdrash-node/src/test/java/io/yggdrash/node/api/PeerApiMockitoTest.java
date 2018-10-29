package io.yggdrash.node.api;

import io.yggdrash.core.BranchId;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerGroup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collection;
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
        Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        when(peerGroup.getPeers(BRANCH, peer)).thenReturn(new ArrayList<>());
        PeerDto requester = PeerDto.valueOf(BRANCH.toString(), peer);
        Collection<String> peerListWithoutRequester
                = peerApi.getPeers(requester);
        assertThat(peerListWithoutRequester).isEmpty();
    }

    @Test
    public void getAllActivePeerTest() {
        when(peerGroup.getActivePeerList())
                .thenReturn(Collections.singletonList(peer.toString()));
        assertThat(peerApi.getAllActivePeer().size()).isEqualTo(1);
    }

    @Test
    public void getPeersRpcTest() {
        try {
            Peer peer = Peer.valueOf(
                    "ynode://9ea9225f0b7db3c697c0a2e09cdd65046899058d16f73378c1559d61aa3e10cd5d"
                            + "c9337142728f5a02faadafab2b926e2998d5bc2b62c2183fab75ca996de2ce@l"
                            + "ocalhost:8082");
            PeerDto requester = PeerDto.valueOf(BRANCH.toString(), peer);
            Collection<String> peerListWithoutRequester =
                    peerApiRpc.getPeers(requester);
            assertThat(peerListWithoutRequester.size()).isNotZero();
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
