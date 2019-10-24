package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.exception.DecodeException;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.gateway.dto.BranchDto;

import java.util.Map;
import java.util.Set;

@JsonRpcService("/api/branch")
public interface BranchApi {

    /**
     * Returns the spec of all running branches in the node
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class, code = NonExistObjectException.CODE)})
    Map<String, BranchDto> getBranches();

    /**
     * Returns the validators of all running branches in the node
     */
    @JsonRpcErrors({@JsonRpcError(exception = NonExistObjectException.class, code = NonExistObjectException.CODE),
            @JsonRpcError(exception = DecodeException.class, code = DecodeException.CODE)})
    Set<String> getValidators(@JsonRpcParam(value = "branchId") String branchId);

}
