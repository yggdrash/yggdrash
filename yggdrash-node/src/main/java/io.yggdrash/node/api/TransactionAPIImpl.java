package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import org.springframework.stereotype.Service;

@Service
@AutoJsonRpcServiceImpl
public class TransactionAPIImpl implements TransactionAPI {
    @Override
    public int test(int a, int b) {
        return a + b;
    }

    /* get */
    @Override
    public int getTransactionCount(String address, String tag) {
        return 0;
    }

    @Override
    public int getTransactionCount(String address, int blockNumber) {
        return 100;
    }

    @Override
    public int getBlockTransactionCountByHash(String hashOfBlock) {
        return 1;
    }

    @Override
    public int getBlockTransactionCountByNumber(int blockNumber) {
        return 2;
    }

    @Override
    public int getBlockTransactionCountByNumber(String tag) {
        return 2;
    }

    @Override
    public TransactionDto getTransactionByHash(String hashOfTx) {
        TransactionDto transactionDto = new TransactionDto();
        return transactionDto;
    }

    @Override
    public TransactionDto getTransactionByBlockHashAndIndex(String hashOfBlock, int txIndexPosition) {
        TransactionDto transactionDto = new TransactionDto();
        return transactionDto;
    }

    @Override
    public TransactionDto getTransactionByBlockNumberAndIndex(int hashOfBlock, int txIndexPosition) {
        TransactionDto transactionDto = new TransactionDto();
        return transactionDto;
    }

    @Override
    public TransactionDto getTransactionByBlockNumberAndIndex(String tag, int txIndexPosition) {
        TransactionDto transactionDto = new TransactionDto();
        return transactionDto;
    }

    @Override
    public TransactionReceiptDto getTransactionReceipt(String hashOfTx) {
        TransactionReceiptDto transactionReceiptDto = new TransactionReceiptDto();
        return transactionReceiptDto;
    }

    /* send */
    @Override
    public String sendTransaction(TransactionDto transactionDto) {
        return "3";
    }

    @Override
    public String sendRawTransaction(TransactionDto transactionDto) {
        return "4";
    }

    /* filter */
    @Override
    public int newPendingTransactionFilter() {
         return 9;
    }

}



