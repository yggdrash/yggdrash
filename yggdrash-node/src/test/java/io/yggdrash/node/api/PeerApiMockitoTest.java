package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerChannelGroup;
import io.yggdrash.core.net.PeerClientChannel;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.node.GRpcClientChannel;
import io.yggdrash.node.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PeerApiMockitoTest {

    @Mock
    private PeerChannelGroup peerChannelGroup;

    private Peer peer1;
    private Peer peer2;
    private PeerGroup peerGroup;
    private PeerApiImpl peerApi;
    private ArrayList<String> peerList;
    private Map<String, PeerClientChannel> peerChannel = new ConcurrentHashMap<>();
    private static final PeerApi peerApiRpc = new JsonRpcConfig().peerApi();
    private static final Logger log = LoggerFactory.getLogger(PeerApiMockitoTest.class);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        String id1 = "ynode://75bff16c@127.0.0.1:9090";
        String id2 = "ynode://75bff16c@30.30.30.30:9090";
        peer1 = Peer.valueOf(id1);
        peer2 = Peer.valueOf(id2);
        peerGroup = new PeerGroup();
        peerGroup.addPeer(peer1);
        peerGroup.addPeer(peer2);
        PeerClientChannel client1 = new GRpcClientChannel(peer1);
        PeerClientChannel client2 = new GRpcClientChannel(peer2);
        peerChannel.put(client1.getPeer().getYnodeUri(), client1);
        peerChannel.put(client2.getPeer().getYnodeUri(), client2);
        peerApi = new PeerApiImpl(peerGroup, peerChannelGroup);
        peerList = new ArrayList<>(peerChannel.keySet());
    }

    @Test
    public void addTest() {
        Peer peer = Peer.valueOf("ynode://65bff16c@127.0.0.1:9090");
        assertThat(peerApi.add(peer)).isNotNull();
    }

    @Test
    public void getAllTest() {
        assertThat(peerApi.getAll().size()).isEqualTo(2);

    }

    @Test
    public void getAllActivePeerTest() {
        when(peerChannelGroup.getActivePeerList()).thenReturn(peerList);
        assertThat(peerApi.getAllActivePeer().size()).isEqualTo(2);
    }

    @Test
    public void addRpcTest() {
        try {
            ObjectMapper objectMapper = TestUtils.getMapper();
            String peerStr = objectMapper.writeValueAsString(peer1);
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
