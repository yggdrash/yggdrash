package io.yggdrash.node.api;

import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import org.springframework.stereotype.Service;

@Service
@AutoJsonRpcServiceImpl
public class TransactionAPIImpl implements TransactionAPI {
    @Override
    public int test(int a, int b) {
        return a + b;
    }

    int numOfTx = 0;
    int filterId = 0;
    String txHash = "0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf";
    String zeroHash = "0x0000000000000000000000000000000000000000";


    /* get */
    @Override
    public int getTransactionCount(String address, String tag) {
        return numOfTx;
    }

    @Override
    public int getTransactionCount(String address, int blockNumber) {
        return numOfTx;
    }

    @Override
    public int getBlockTransactionCountByHash(String hashOfBlock) {
        return numOfTx;
    }

    @Override
    public int getBlockTransactionCountByNumber(int blockNumber) {
        return numOfTx;
    }

    @Override
    public int getBlockTransactionCountByNumber(String tag) {
        return numOfTx;
    }

    @Override
    public JsonObject getTransactionByHash(String hashOfTx) {
        TransactionMock txMock = new TransactionMock();
        JsonObject tx = txMock.retTxMock();
        return tx;
    }

    @Override
    public JsonObject getTransactionByBlockHashAndIndex(String hashOfBlock, int txIndexPosition) {
        TransactionMock txMock = new TransactionMock();
        JsonObject txObj = txMock.retTxMock();
        return txObj;
    }

    @Override
    public JsonObject getTransactionByBlockNumberAndIndex(int hashOfBlock, int txIndexPosition) {
        TransactionMock txMock = new TransactionMock();
        JsonObject txObj = txMock.retTxMock();
        return txObj;
    }

    @Override
    public JsonObject getTransactionByBlockNumberAndIndex(String tag, int txIndexPosition) {
        TransactionMock txMock = new TransactionMock();
        JsonObject txObj = txMock.retTxMock();
        return txObj;
    }

    @Override
    public JsonObject getTransactionReceipt(String hashOfTx) {
        TransactionReceiptMock txReceiptMock = new TransactionReceiptMock();
        JsonObject txReceiptObj = txReceiptMock.retTxReceiptMock();
        return txReceiptObj;
    }

    /* send */
    @Override
    public String sendTransaction(JsonObject txObj) {
        return txHash;
    }

    @Override
    public String sendRawTransaction(JsonObject txObj) {
        return zeroHash;
    }

    /* filter */
    @Override
    public int newPendingTransactionFilter() {
         return filterId;
    }

}



