package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yggdrash.TestUtils;
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

    @Mock
    private PeerGroup peerGroup;
    private Peer peer;

    private PeerApiImpl peerApi;
    private static final PeerApi peerApiRpc = new JsonRpcConfig().peerApi();

    @Before
    public void setUp() {
        this.peer = Peer.valueOf("ynode://65bff16c@127.0.0.1:32918");
        peerApi = new PeerApiImpl(peerGroup);
    }

    @Test
    public void addTest() {
        assertThat(peerApi.add(peer)).isNotNull();
    }

    @Test
    public void getAllTest() {
        when(peerGroup.getPeers()).thenReturn(Collections.singletonList(peer));
        assertThat(peerApi.getAll().size()).isEqualTo(1);
    }

    @Test
    public void getAllActivePeerTest() {
        when(peerGroup.getActivePeerList())
                .thenReturn(Collections.singletonList(peer.getYnodeUri()));
        assertThat(peerApi.getAllActivePeer().size()).isEqualTo(1);
    }

    @Test
    public void addRpcTest() {
        try {
            ObjectMapper objectMapper = TestUtils.getMapper();
            String peerStr = objectMapper.writeValueAsString(peer);
            Peer peer = objectMapper.readValue(peerStr, Peer.class);
            peerApiRpc.add(peer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getAllRpcTest() {
        try {
            peerApiRpc.getAll();
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
