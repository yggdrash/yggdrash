package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.exception.NonExistObjectException;

import java.util.List;

@JsonRpcService("/api/peer")
public interface PeerApi {

    /**
     * Returns all active peers
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.CODE)})
    List<String> getAllActivePeer();
}
