package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.net.PeerHandlerGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AutoJsonRpcServiceImpl
public class PeerApiImpl implements PeerApi {

    private final PeerHandlerGroup peerHandlerGroup;

    @Autowired
    public PeerApiImpl(PeerHandlerGroup peerHandlerGroup) {
        this.peerHandlerGroup = peerHandlerGroup;
    }

    @Override
    public List<String> getAllActivePeer() {
        return peerHandlerGroup.getActivePeerList();
    }
}
