package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;

import java.io.IOException;

@JsonRpcService("/block")
public interface BlockApi {
    int blockNumber();

    String getBlockByHash(@JsonRpcParam(value = "address") String address,
                          @JsonRpcParam(value = "tag") String tag) throws IOException;

    String getBlockByNumber(@JsonRpcParam(value = "hashOfBlock") String hashOfBlock,
                            @JsonRpcParam(value = "bool") Boolean bool) throws IOException;

    int newBlockFilter();
}

