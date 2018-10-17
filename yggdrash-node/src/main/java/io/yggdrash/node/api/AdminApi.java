package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.node.controller.AdminDto;

@JsonRpcService("/api/admin")
public interface AdminApi {

    /**
     * Client send a nodeHello, node return a clientHello message.
     *
     * @param command The command data
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = FailedOperationException.class,
                    code = FailedOperationException.code)})
    String nodeHello(@JsonRpcParam(value = "command") AdminDto command);
}
