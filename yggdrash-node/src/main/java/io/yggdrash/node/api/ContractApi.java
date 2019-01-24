package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.contract.ContractId;
import io.yggdrash.core.contract.TransactionReceipt;
import io.yggdrash.core.exception.NonExistObjectException;

import java.util.List;
import java.util.Map;

@JsonRpcService("/api/contract")
public interface ContractApi {

    /**
     * Handles all queries that are dispatched to the contract
     *
     * @param branchId branch id of contract
     * @param method query method
     * @param params query params
     * @return result of query
     * @throws Exception exception
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    Object query(@JsonRpcParam(value = "branchId") String branchId,
                 @JsonRpcParam(value = "method") String method,
                 @JsonRpcParam(value = "params") Map params) throws Exception;

    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    Object contract(@JsonRpcParam(value = "contractId") String contractId,
                    @JsonRpcParam(value = "method") String method,
                    @JsonRpcParam(value = "params") Map params) throws Exception;

}
