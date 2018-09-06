package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerChannelGroup;
import io.yggdrash.core.net.PeerGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@AutoJsonRpcServiceImpl
public class PeerApiImpl implements PeerApi {

    private final PeerGroup peerGroup;
    private final PeerChannelGroup peerChannelGroup;

    @Autowired
    public PeerApiImpl(PeerGroup peerGroup, PeerChannelGroup peerChannelGroup) {
        this.peerGroup = peerGroup;
        this.peerChannelGroup = peerChannelGroup;
    }

    @Override
    public Peer add(Peer peer) {
        return peerGroup.addPeer(peer);
    }

    @Override
    public Collection<Peer> getAll() {
        return peerGroup.getPeers();
    }

    @Override
    public List<String> getAllActivePeer() {
        return peerChannelGroup.getActivePeerList();
    }
}
