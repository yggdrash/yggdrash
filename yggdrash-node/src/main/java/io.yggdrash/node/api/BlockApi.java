package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.Block;
import io.yggdrash.node.exception.InternalErrorException;
import io.yggdrash.node.exception.NonExistObjectException;

@JsonRpcService("/api/block")
public interface BlockApi {
    /**
     * Returns the number of most recent block.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = InternalErrorException.class,
                          code = InternalErrorException.code)})
    int blockNumber();

    /**
     * Returns information about a block by hash.
     *
     * @param address     account address
     * @param bool        If true, it returns the full transaction objects,
     *                    if false only the hashes of the transactions.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                          code = NonExistObjectException.code)})
    Block getBlockByHash(@JsonRpcParam(value = "address") String address,
                         @JsonRpcParam(value = "bool") Boolean bool);

    /**
     * Returns information about a block by block number.
     *
     * @param hashOfBlock hash of block
     * @param bool        If true, it returns the full transaction objects,
     *                    if false only the hashes of the transactions.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                          code = NonExistObjectException.code)})
    Block getBlockByNumber(@JsonRpcParam(value = "hashOfBlock") String hashOfBlock,
                           @JsonRpcParam(value = "bool") Boolean bool);

    /**
     * Creates a filter in the node, to notify when a new block arrives.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = InternalErrorException.class,
                          code = InternalErrorException.code)})
    int newBlockFilter();
}