package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.node.exception.NonExistObjectException;

import java.util.ArrayList;

@JsonRpcService("/api/account")
public interface AccountApi {

    /**
     * Create a new account
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                          code = NonExistObjectException.code)})
    String createAccount();

    /**
     * Returns a list of addresses owned by client.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                          code = NonExistObjectException.code)})
    ArrayList<String> accounts();

    /**
     *  Returns the balance of the account of given address.
     *
     * @param address     account address
     * @param blockNumber block number
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                          code = NonExistObjectException.code)})
    int getBalance(@JsonRpcParam(value = "address") String address,
                   @JsonRpcParam(value = "blockNumber") int blockNumber);

    /**
     *  Returns the balance of the account of given address.
     *
     * @param address     account address*
     * @param tag         "latest","earliest","pending"
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                          code = NonExistObjectException.code)})
    int getBalance(@JsonRpcParam(value = "address") String address,
                   @JsonRpcParam(value = "tag") String tag);
}