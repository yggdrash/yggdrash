package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;

import java.util.ArrayList;

@JsonRpcService("/account")
public interface AccountAPI {
    String createAccount();
    ArrayList<String> accounts();
    int getBalance(@JsonRpcParam(value = "address") String address, @JsonRpcParam(value = "blockNumber") int blockNumber);
    int getBalance(@JsonRpcParam(value = "address") String address, @JsonRpcParam(value = "tag") String tag);
}

