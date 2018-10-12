package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@AutoJsonRpcServiceImpl
public class PeerApiImpl implements PeerApi {

    private final PeerGroup peerGroup;

    @Autowired
    public PeerApiImpl(PeerGroup peerGroup) {
        this.peerGroup = peerGroup;
    }

    @Override
    public Collection<Peer> getPeers(String branchId) {
        return peerGroup.getPeers(BranchId.of(branchId));
    }

    @Override
    public List<String> getAllActivePeer() {
        return peerGroup.getActivePeerList();
    }
}
