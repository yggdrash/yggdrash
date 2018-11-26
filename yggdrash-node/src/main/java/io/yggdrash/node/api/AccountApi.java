package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.exception.NonExistObjectException;

import java.util.ArrayList;

@JsonRpcService("/api/wallet")
public interface AccountApi {

    /**
     * Create a new wallet
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
     * Returns the balance of the wallet of given address.
     *
     * @param data wallet address
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    String balanceOf(@JsonRpcParam(value = "data") String data);

    /**
     * Returns the balance of the wallet of given address.
     *
     * @param address     wallet address
     * @param blockNumber block number
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    long getBalance(@JsonRpcParam(value = "address") String address,
                   @JsonRpcParam(value = "blockNumber") int blockNumber);

    /**
     * Returns the balance of the wallet of given address.
     *
     * @param address wallet address*
     * @param tag     "latest","earliest","pending"
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    long getBalance(@JsonRpcParam(value = "address") String address,
                   @JsonRpcParam(value = "tag") String tag);
}