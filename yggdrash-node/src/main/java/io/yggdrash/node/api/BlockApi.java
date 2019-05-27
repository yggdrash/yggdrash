package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.exception.InternalErrorException;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.gateway.dto.BlockDto;

import static io.yggdrash.common.config.Constants.BLOCK_ID;
import static io.yggdrash.common.config.Constants.BRANCH_ID;

@JsonRpcService("/api/block")
public interface BlockApi {
    /**
     * Returns the number of most recent block.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.CODE)})
    long blockNumber(@JsonRpcParam(value = BRANCH_ID) String branchId);

    /**
     * Returns information about a block by hash.
     *
     * @param branchId Hash of branch
     * @param blockId Hash of block
     * @param bool        If true, it returns the full transaction objects,
     *                    if false only the hashes of the transactions.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.CODE)})
    BlockDto getBlockByHash(@JsonRpcParam(value = BRANCH_ID) String branchId,
                             @JsonRpcParam(value = BLOCK_ID) String blockId,
                             @JsonRpcParam(value = "bool") Boolean bool);

    /**
     * Returns information about a block by block number.
     *
     * @param branchId Hash of branch
     * @param numOfBlock  Number of block
     * @param bool        If true, it returns the full transaction objects,
     *                    if false only the hashes of the transactions.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.CODE)})
    BlockDto getBlockByNumber(@JsonRpcParam(value = BRANCH_ID) String branchId,
                              @JsonRpcParam(value = "numOfBlock") long numOfBlock,
                              @JsonRpcParam(value = "bool") Boolean bool);

    /**
     * Get last block
     * @return the latest block
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.CODE)})
    BlockDto getLastBlock(@JsonRpcParam(value = BRANCH_ID) String branchId);
}
