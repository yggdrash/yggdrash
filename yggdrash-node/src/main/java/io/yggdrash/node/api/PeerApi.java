package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.exception.NonExistObjectException;

import java.util.Collection;
import java.util.List;

@JsonRpcService("/api/peer")
public interface PeerApi {

    /**
     * Returns peers by branchId
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    Collection<String> getPeers(@JsonRpcParam(value = "peer") PeerDto peerDto);

    /**
     * Returns all active peers
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    List<String> getAllActivePeer();
}
