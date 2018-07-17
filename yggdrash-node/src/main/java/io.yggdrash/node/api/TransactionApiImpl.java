package io.yggdrash.node.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Longs;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.node.mock.TransactionMock;
import io.yggdrash.node.mock.TransactionReceiptMock;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.spongycastle.util.Arrays;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@AutoJsonRpcServiceImpl
public class TransactionApiImpl implements TransactionApi {

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
    public String getTransactionByBlockHashAndIndex(
            String hashOfBlock, int txIndexPosition) throws IOException {
        TransactionMock txMock = new TransactionMock();
        Transaction tx = txMock.retTxMock();
        return tx.toString();
    }

    @Override
    public String getTransactionByBlockNumberAndIndex(
            int blockNumber, int txIndexPosition) throws IOException {
        TransactionMock txMock = new TransactionMock();
        Transaction tx = txMock.retTxMock();
        return tx.toString();
    }

    @Override
    public String getTransactionByBlockNumberAndIndex(
            String tag, int txIndexPosition) throws IOException {
        TransactionMock txMock = new TransactionMock();
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
        Transaction tx = converter(jsonStr);
        return tx.getHashString();
    }

    @Override
    public byte[] sendRawTransaction(byte[] bytes) throws IOException {
        Transaction tx = converter(bytes);
        return tx.getHash();
    }

    /* filter */
    @Override
    public int newPendingTransactionFilter() {
        return 6;
    }

    public Transaction converter(String jsonStr) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Transaction tx = mapper.readValue(jsonStr, Transaction.class);

        return tx;
    }

    public Transaction converter(byte[] bytes) {
        byte[] type = new byte[4];
        byte[] version = new byte[4];
        byte[] dataHash = new byte[32];
        byte[] timestamp = new byte[8];
        byte[] dataSize = new byte[8];
        byte[] signature = new byte[65];

        int typeLength = type.length;
        int versionLength = version.length;
        int dataHashLength = dataHash.length;
        int timestampLength = timestamp.length;
        int dataSizeLength = dataSize.length;
        int signatureLength = signature.length;
        int txHeaderLength;
        txHeaderLength = typeLength + versionLength + dataHashLength + timestampLength
                + dataHashLength + dataSizeLength + signatureLength;

        int sum = 0;
        type = Arrays.copyOfRange(bytes, sum, sum += typeLength);
        version = Arrays.copyOfRange(bytes, sum, sum += versionLength);
        dataHash = Arrays.copyOfRange(bytes, sum, sum += dataHashLength);
        timestamp = Arrays.copyOfRange(bytes, sum, sum += timestampLength);
        dataSize = Arrays.copyOfRange(bytes, sum, sum += dataSizeLength);
        signature = Arrays.copyOfRange(bytes, sum, sum += signatureLength);
        byte[] data = Arrays.copyOfRange(bytes, sum, txHeaderLength);

        Long timestampStr = Longs.fromByteArray(timestamp);
        Long dataSizeStr = Longs.fromByteArray(dataSize);
        String dataStr = new String(data);

        TransactionHeader txHeader;
        txHeader = new TransactionHeader(
                type, version, dataHash, timestampStr, dataSizeStr, signature);

        return new Transaction(txHeader, dataStr);
    }
}
