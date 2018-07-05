package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.Transaction;
import io.yggdrash.node.mock.TransactionMock;
import io.yggdrash.node.mock.TransactionPoolMock;
import io.yggdrash.node.mock.TransactionReceiptMock;
import org.json.simple.JSONObject;
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
        Gson gson = new Gson();
        String js = gson.toJson(tx);
        System.out.println("js :: " + js);
        //js :: {"header":{"type":[0,0,0,0],"version":[0,0,0,0],"dataHash":[101,127,-30,38,108,107,-23,3,-69,29,80,-91,-93,108,88,-1,-19,-65,96,68,37,39,-93,114,51,-65,18,-23,-55,-119,-48,-120],"timestamp":20395381177213,"dataSize":463,"signature":[28,-83,27,-62,-42,-118,-24,-7,82,-47,-97,-26,48,-73,-24,20,-55,-79,86,118,-31,-34,-105,-87,-48,68,-49,-13,91,-59,-119,86,12,45,124,-82,3,-2,30,-99,108,-35,-76,72,-115,110,16,74,85,102,62,114,-13,-52,-51,32,-4,-15,11,71,-72,7,97,73,71]},"data":"{\"version\":\"0\",\"type\":\"00000000000000\",\"timestamp\":\"155810745733540\",\"from\":\"a6cf59d72cb6c253b3cfe10d498ac8615453689b\",\"dataHash\":\"ba5f3ea40e95f49bce11942f375ebd3882eb837976eda5c0cb78b9b99ca7b485\",\"dataSize\":\"13\",\"signature\":\"b86e02880e12c575e56c5d15e1f491595219295076721a5bfb6042463d6a2d768331691db0b8de852390305c0f2b218e596e4a59bf54029cf6a8b9afdbb274104\",\"transactionHash\":\"c6b5e583ec18891e9de0e29c3f0358a5c99c474bc3ee78e90c618db72193c0\",\"transactionData\":\"{}\"}"}
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

    /* test */
    @Override
    public String getJsonObj(String tx) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        TransactionMock transaction = mapper.reader()
                .forType(TransactionMock.class).readValue(tx);

        System.out.println(transaction);

        TransactionPoolMock transactionPoolMock = new TransactionPoolMock();
        transactionPoolMock.addTx(transaction);

        return transaction.toString();
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

 curl -sH "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"getTransactionByHash","params":{"hashOfTx":"0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf"}}' http://localhost:8080/transaction | python -m json.tool
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

 curl --request POST \
  --url 'http://localhost:8080/transaction' \
  --header 'Content-Type: application/json' \
  --data '{
        "jsonrpc":"2.0",
        "method":"getJsonObj",
        "params":[{type : 00000000, version : 00000000, dataHash : 657fe2266c6be903bb1d50a5a36c58ffedbf60442527a37233bf12e9c989d088, timestamp : 17537868825465, dataSize : 463, signature : 1c7f6f9204c4b1a486c34de8a5581fe5bd7c116c539be4778ef4c560f3b07212820e22d452719a6eb624521fab1faee1b0bd05232bfa076423be663d08771b1a47, transactionDta : {\"version\":\"0\",\"type\":\"00000000000000\",\"timestamp\":\"155810745733540\",\"from\":\"a6cf59d72cb6c253b3cfe10d498ac8615453689b\",\"dataHash\":\"ba5f3ea40e95f49bce11942f375ebd3882eb837976eda5c0cb78b9b99ca7b485\",\"dataSize\":\"13\",\"signature\":\"b86e02880e12c575e56c5d15e1f491595219295076721a5bfb6042463d6a2d768331691db0b8de852390305c0f2b218e596e4a59bf54029cf6a8b9afdbb274104\",\"transactionHash\":\"c6b5e583ec18891e9de0e29c3f0358a5c99c474bc3ee78e90c618db72193c0\",\"transactionData\":\"{}\"}}]
        "id":1
}' | python -m json.tool
 */


