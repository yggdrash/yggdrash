package io.yggdrash.node.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.node.mock.TransactionMock;
import io.yggdrash.node.mock.TransactionReceiptMock;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@AutoJsonRpcServiceImpl
public class TransactionApiImpl implements TransactionApi {

    private final NodeManager nodeManager;

    public TransactionApiImpl(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }


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
        TransactionMock txMock = new TransactionMock(this.nodeManager);
        Transaction tx = txMock.retTxMock();
        return tx.toString();
    }

    @Override
    public String getTransactionByBlockHashAndIndex(
            String hashOfBlock, int txIndexPosition) throws IOException {
        TransactionMock txMock = new TransactionMock(this.nodeManager);
        Transaction tx = txMock.retTxMock();
        return tx.toString();
    }

    @Override
    public String getTransactionByBlockNumberAndIndex(
            int blockNumber, int txIndexPosition) throws IOException {
        TransactionMock txMock = new TransactionMock(this.nodeManager);
        Transaction tx = txMock.retTxMock();
        return tx.toString();
    }

    @Override
    public String getTransactionByBlockNumberAndIndex(
            String tag, int txIndexPosition) throws IOException {
        TransactionMock txMock = new TransactionMock(this.nodeManager);
        Transaction tx = txMock.retTxMock();
        return tx.toString();
    }

    @Override
    public String getTransactionReceipt(String hashOfTx) {
        TransactionReceiptMock txReceiptMock = new TransactionReceiptMock();
        return txReceiptMock.retTxReceiptMock();
    }

    /* send */
    @Override
    public String sendTransaction(String jsonStr) throws ParseException,JsonProcessingException {
        TransactionDto transactionDto = new TransactionDto();
        Transaction tx = transactionDto.jsonStringToTx(jsonStr);
        ObjectMapper objectMapper = new ObjectMapper();
        return "sendTransaction [success] " + objectMapper.writeValueAsString(tx);
    }

    @Override
    public String sendRawTransaction(String jsonByteArr)
            throws ParseException,JsonProcessingException {
        TransactionDto transactionDto = new TransactionDto();
        Transaction tx = transactionDto.jsonByteArrToTx(jsonByteArr);
        ObjectMapper objectMapper = new ObjectMapper();
        return "sendRawTransaction [success] " + objectMapper.writeValueAsString(tx);
    }

    @Override
    public String sendRawTransaction(byte[] bytes) throws JsonProcessingException {
        TransactionDto transactionDto = new TransactionDto();
        Transaction tx = transactionDto.byteArrToTx(bytes);
        ObjectMapper objectMapper = new ObjectMapper();
        return "sendRawTransaction [success] " + objectMapper.writeValueAsString(tx);
    }

    /* filter */
    @Override
    public int newPendingTransactionFilter() {
        return 6;
    }
}
