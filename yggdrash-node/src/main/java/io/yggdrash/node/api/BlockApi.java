package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.exception.InternalErrorException;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.node.api.dto.BlockDto;

@JsonRpcService("/api/block")
public interface BlockApi {
    /**
     * Returns the number of most recent block.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    long blockNumber(@JsonRpcParam(value = "branchId") String branchId);

    /**
     * Returns information about a block by hash.
     *
     * @param hashOfBlock Hash of block
     * @param bool        If true, it returns the full transaction objects,
     *                    if false only the hashes of the transactions.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    BlockDto getBlockByHash(@JsonRpcParam(value = "branchId") String branchId,
                             @JsonRpcParam(value = "hashOfBlock") String hashOfBlock,
                             @JsonRpcParam(value = "bool") Boolean bool);

    /**
     * Returns information about a block by block number.
     *
     * @param numOfBlock  Number of block
     * @param bool        If true, it returns the full transaction objects,
     *                    if false only the hashes of the transactions.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    BlockDto getBlockByNumber(@JsonRpcParam(value = "branchId") String branchId,
                              @JsonRpcParam(value = "numOfBlock") long numOfBlock,
                              @JsonRpcParam(value = "bool") Boolean bool);

    /**
     * Creates a filter in the node, to notify when a new block arrives.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = InternalErrorException.class,
                    code = InternalErrorException.code)})
    int newBlockFilter();

    /**
     * Get last block
     * @return the latest block
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    BlockDto getLastBlock(@JsonRpcParam(value = "branchId") String branchId);
}