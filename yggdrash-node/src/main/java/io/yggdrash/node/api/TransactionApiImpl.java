package io.yggdrash.node.api;

import com.google.common.primitives.Longs;
import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.core.TransactionReceipt;
import org.spongycastle.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AutoJsonRpcServiceImpl
public class TransactionApiImpl implements TransactionApi {

    private final NodeManager nodeManager;

    @Autowired
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
    public Transaction getTransactionByHash(String hashOfTx) {
        return retTxMock();
    }

    @Override
    public Transaction getTransactionByBlockHashAndIndex(String hashOfBlock, int txIndexPosition) {
        return retTxMock();
    }

    @Override
    public Transaction getTransactionByBlockNumberAndIndex(int blockNumber, int txIndexPosition) {
        return retTxMock();
    }

    @Override
    public Transaction getTransactionByBlockNumberAndIndex(String tag, int txIndexPosition) {
        return retTxMock();
    }

    @Override
    public TransactionReceipt getTransactionReceipt(String hashOfTx) {
        return new TransactionReceipt();
    }

    /* send */
    @Override
    public String sendTransaction(Transaction tx) {
        Transaction addedTx = nodeManager.addTransaction(tx);
        return addedTx.getHashString();
    }

    @Override
    public byte[] sendRawTransaction(byte[] bytes) {
        Transaction tx = convert(bytes);
        Transaction addedTx = nodeManager.addTransaction(tx);
        return addedTx.getHash();
    }

    /* filter */
    @Override
    public int newPendingTransactionFilter() {
        return 6;
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


        long timestampStr = Longs.fromByteArray(timestamp);
        long dataSizeStr = Longs.fromByteArray(dataSize);
        String dataStr = new String(data);

        TransactionHeader txHeader;
        txHeader = new TransactionHeader(
                type, version, dataHash, timestampStr, dataSizeStr, signature);

        return new Transaction(txHeader, dataStr);
    }

    private Transaction retTxMock() {

        // Create transaction
        JsonObject txObj = new JsonObject();

        txObj.addProperty("operator", "transfer");
        txObj.addProperty("to", "0x9843DC167956A0e5e01b3239a0CE2725c0631392");
        txObj.addProperty("value", 100);

        Transaction tx = new Transaction(txObj);
        return nodeManager.signByNode(tx);
    }
}