package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.p2p.PeerDialer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AutoJsonRpcServiceImpl
public class PeerApiImpl implements PeerApi {

    private final PeerDialer peerDialer;

    @Autowired
    public PeerApiImpl(PeerDialer peerDialer) {
        this.peerDialer = peerDialer;
    }

    @Override
    public List<String> getAllActivePeer() {
        return peerDialer.getActivePeerList();
    }
}
