package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.Transaction;
import io.yggdrash.node.mock.TransactionMock;
import io.yggdrash.node.mock.TransactionPoolMock;
import io.yggdrash.node.mock.TransactionReceiptMock;
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

    /* test */
    @Override
    public String getJsonObj(String tx) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        mapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);

        TransactionMock transaction = mapper.reader()
                .forType(TransactionMock.class).readValue(tx);

        System.out.println(transaction);

        TransactionPoolMock transactionPoolMock = new TransactionPoolMock();
        transactionPoolMock.addTx(transaction);

        return transaction.toString();
    }

}
