package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Longs;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.node.mock.TransactionMock;
import io.yggdrash.node.mock.TransactionReceiptMock;
import org.spongycastle.util.Arrays;
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
    public String sendTransaction(String jsonStr) throws IOException {
        Transaction tx = convert(jsonStr);
        return tx.getHashString();
    }

    @Override
    public byte[] sendRawTransaction(byte[] bytes) throws IOException {
        Transaction tx = convert(bytes);
        return tx.getHash();
    }

    /* filter */
    @Override
    public int newPendingTransactionFilter() {
        return 6;
    }

    private Transaction convert(String jsonStr) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Transaction tx = mapper.readValue(jsonStr, Transaction.class);

        return tx;
    }

    private Transaction convert(byte[] bytes) {

        int sum = 0;
        byte[] type = new byte[4];
        type = Arrays.copyOfRange(bytes, sum, sum += type.length);
        byte[] version = new byte[4];
        version = Arrays.copyOfRange(bytes, sum, sum += version.length);
        byte[] dataHash = new byte[32];
        dataHash = Arrays.copyOfRange(bytes, sum, sum += dataHash.length);
        byte[] timestamp = new byte[8];
        timestamp = Arrays.copyOfRange(bytes, sum, sum += timestamp.length);
        byte[] dataSize = new byte[8];
        dataSize = Arrays.copyOfRange(bytes, sum, sum += dataSize.length);
        byte[] signature = new byte[65];
        signature = Arrays.copyOfRange(bytes, sum, sum += signature.length);
        byte[] data = Arrays.copyOfRange(bytes, sum, bytes.length);


        Long timestampStr = Longs.fromByteArray(timestamp);
        Long dataSizeStr = Longs.fromByteArray(dataSize);
        String dataStr = new String(data);

        TransactionHeader txHeader;
        txHeader = new TransactionHeader(
                type, version, dataHash, timestampStr, dataSizeStr, signature);

        return new Transaction(txHeader, dataStr);
    }
}
