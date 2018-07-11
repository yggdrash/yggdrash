package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;

import java.util.ArrayList;

@JsonRpcService("/api/account")
public interface AccountApi {

    /**
     * Create a new account
     */
    String createAccount();

    /**
     * Returns a list of addresses owned by client.
     */
    ArrayList<String> accounts();

    /**
     *  Returns the balance of the account of given address.
     *
     * @param address     account address
     * @param blockNumber block number
     */
    int getBalance(@JsonRpcParam(value = "address") String address,
                   @JsonRpcParam(value = "blockNumber") int blockNumber);

    /**
     *  Returns the balance of the account of given address.
     *
     * @param address     account address*
     * @param tag         "latest","earliest","pending"
     */
    int getBalance(@JsonRpcParam(value = "address") String address,
                   @JsonRpcParam(value = "tag") String tag);
}