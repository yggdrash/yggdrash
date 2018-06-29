package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;

import java.lang.reflect.Array;

@JsonRpcService("/account")
public interface AccountAPI {
    Array accounts();
    int getBalance(@JsonRpcParam(value = "address") String address, @JsonRpcParam(value = "blockNumber") int blockNumber);
    int getBalance(@JsonRpcParam(value = "address") String address, @JsonRpcParam(value = "tag") String tag);
}

/*
    accounts()
    - Returns a list of addresses owned by client.
    - params : none
    - returns : Array of DATA, 20 Bytes - addresses owned by the client.

    getBalance()
    - Returns the balance of the account of given address.
    - params : DATA, 20 Bytes - address to check for balance.
               QUANTITY|TAG - integer block number, or the string "latest", "earliest" or "pending",
    - returns : QUANTITY - integer of the current balance in wei.
 */