package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.Transaction;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@AutoJsonRpcServiceImpl
public class TransactionAPIImpl implements TransactionAPI {

    String txHash = "0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf";
    String zeroHash = "0x0000000000000000000000000000000000000000";

    /* get */
    @Override
    public int getTransactionCount(String address, String tag) {
        return 1;
    }

    @Override
    public int getTransactionCount(String address, int blockNumber) {
        return 2;
    }

    @Override
    public int getBlockTransactionCountByHash(String hashOfBlock) {
        return 3;
    }

    @Override
    public int getBlockTransactionCountByNumber(int blockNumber) {
        return 4;
    }

    @Override
    public int getBlockTransactionCountByNumber(String tag) {
        return 5;
    }

    @Override
    public String getTransactionByHash(String hashOfTx) throws IOException {
        TransactionMock txMock = new TransactionMock();
        Transaction tx = txMock.retTxMock();
        return tx.toString();
    }

    @Override
    public String getTransactionByBlockHashAndIndex(String hashOfBlock, int txIndexPosition)  throws IOException {
        TransactionMock txMock = new TransactionMock();
        Transaction tx = txMock.retTxMock();
        return tx.toString();
    }

    @Override
    public String getTransactionByBlockNumberAndIndex(int blockNumber, int txIndexPosition) throws IOException {
        TransactionMock txMock = new TransactionMock();
        Transaction tx = txMock.retTxMock();
        return tx.toString();
    }

    @Override
    public String getTransactionByBlockNumberAndIndex(String tag, int txIndexPosition)  throws IOException {
        TransactionMock txMock = new TransactionMock();
        Transaction tx = txMock.retTxMock();
        return tx.toString();
    }

    @Override
    public String getTransactionReceipt(String hashOfTx) {
        TransactionReceiptMock txReceiptMock = new TransactionReceiptMock();
        String txReceiptObj = txReceiptMock.retTxReceiptMock();
        return txReceiptObj;
    }

    /* send */
    @Override
    public String sendTransaction(String tx) {
        String ret = "{[get : " + tx + "][result : {txhash : " + txHash + "}]}";
        return ret;
    }

    @Override
    public String sendRawTransaction(String rawTx) {
        String ret = "{[get : " + rawTx + "][result : {txhash : " + zeroHash + "}]}";
        return ret;
    }

    /* filter */
    @Override
    public int newPendingTransactionFilter() {
         return 6;
    }

}

/*
 curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"getTransactionCount","params":{"address":"0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf", "tag":"latest"}}' http://localhost:8080/transaction
 > 1
 curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"getTransactionCount","params":{"address":"0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf", "blockNumber":1}}' http://localhost:8080/transaction
 > 2
 curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"getBlockTransactionCountByHash","params":{"hashOfBlock":"0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf"}}' http://localhost:8080/transaction
 > 3
 curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"getBlockTransactionCountByNumber","params":{"blockNumber":1}}' http://localhost:8080/transaction
 > 4
 curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"getBlockTransactionCountByNumber","params":{"tag":"latest"}}' http://localhost:8080/transaction
 > 5

 curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"getTransactionByHash","params":{"hashOfTx":"0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf"}}' http://localhost:8080/transaction
 > txObj
 curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"getTransactionByBlockHashAndIndex","params":{"hashOfBlock":"0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf", "txIndexPosition":1}}' http://localhost:8080/transaction
 > txObj
 curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"getTransactionByBlockNumberAndIndex","params":{"blockNumber":1, "txIndexPosition":1}}' http://localhost:8080/transaction
 > txObj
 curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"getTransactionByBlockNumberAndIndex","params":{"tag":"latest", "txIndexPosition":1}}' http://localhost:8080/transaction
 > txObj
 curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"getTransactionReceipt","params":{"hashOfTx":"0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf"}}' http://localhost:8080/transaction
 > txReceiptObj

 curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"sendTransaction","params":{"hello"}}' http://localhost:8080/transaction
 curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"sendRawTransaction","params":{"hello"}}' http://localhost:8080/transaction

 curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"newPendingTransactionFilter","params":{}}' http://localhost:8080/transaction
 > 6
 */