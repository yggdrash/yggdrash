package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.gateway.dto.BranchDto;

import java.util.Map;

@JsonRpcService("/api/branch")
public interface BranchApi {

    /**
     * Returns the spec of all running branches in the node
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    Map<String, BranchDto> getBranches();
}
