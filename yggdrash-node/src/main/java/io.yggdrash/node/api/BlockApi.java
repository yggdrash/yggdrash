package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;

import java.io.IOException;

@JsonRpcService("/api/block")
public interface BlockApi {
    /**
     * Returns the number of most recent block.
     */
    int blockNumber();

    /**
     *  Returns information about a block by hash.
     *
     * @param address     account address
     * @param tag         "latest","earlest","pending"
     */
    String getBlockByHash(@JsonRpcParam(value = "address") String address,
                          @JsonRpcParam(value = "tag") String tag) throws IOException;

    /**
     *  Returns information about a block by block number.
     *
     * @param hashOfBlock hash of block
     * @param bool        If true, it returns the full transaction objects, if false only the hashes of the transactions.
     */
    String getBlockByNumber(@JsonRpcParam(value = "hashOfBlock") String hashOfBlock,
                            @JsonRpcParam(value = "bool") Boolean bool) throws IOException;

    /**
     * Creates a filter in the node, to notify when a new block arrives.
     */
    int newBlockFilter();
}