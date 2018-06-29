package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;

@JsonRpcService("/transaction")
public interface TransactionAPI {

    /* get */
    int getTransactionCount(@JsonRpcParam(value = "address") String address, @JsonRpcParam(value = "tag") String tag);
    int getTransactionCount(@JsonRpcParam(value = "address") String address, @JsonRpcParam(value = "blockNumber") int blockNumber);
    int getBlockTransactionCountByHash(@JsonRpcParam(value = "hashOfBlock") String hashOfBlock);
    int getBlockTransactionCountByNumber(@JsonRpcParam(value = "blockNumber") int blockNumber);
    int getBlockTransactionCountByNumber(@JsonRpcParam(value = "tag") String tag);
    String getTransactionByHash(@JsonRpcParam(value = "hashOfTx") String hashOfTx);
    String getTransactionByBlockHashAndIndex(@JsonRpcParam(value = "hashOfBlock") String hashOfBlock, @JsonRpcParam(value = "txIndexPosition") int txIndexPosition);
    String getTransactionByBlockNumberAndIndex(@JsonRpcParam(value = "blockNumber") int blockNumber, @JsonRpcParam(value = "txIndexPosition") int txIndexPosition);
    String getTransactionByBlockNumberAndIndex(@JsonRpcParam(value = "tag") String tag, @JsonRpcParam(value = "txIndexPosition") int txIndexPosition);
    String getTransactionReceipt(@JsonRpcParam(value = "hashOfTx") String hashOfTx);

    /* send */
    String sendTransaction(@JsonRpcParam(value = "tx") String tx);
    String sendRawTransaction(@JsonRpcParam(value = "rawTx") String rawTx);

    /* filter */
    int newPendingTransactionFilter();
}


