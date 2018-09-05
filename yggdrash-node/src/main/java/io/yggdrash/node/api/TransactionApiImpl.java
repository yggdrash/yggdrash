package io.yggdrash.node.api;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.TransactionReceipt;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.node.controller.TransactionDto;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@AutoJsonRpcServiceImpl
public class TransactionApiImpl implements TransactionApi {

    private static final Logger log = LoggerFactory.getLogger(TransactionApiImpl.class);

    private final NodeManager nodeManager;
    private final TransactionReceiptStore txReceiptStore;

    @Autowired
    public TransactionApiImpl(NodeManager nodeManager, TransactionReceiptStore txReceiptStore) {
        this.nodeManager = nodeManager;
        this.txReceiptStore = txReceiptStore;
    }

    public int getCount(String address, List<TransactionHusk> txList) {
        int cnt = 0;
        for (TransactionHusk tx : txList) {
            if (address.equals(tx.getAddress().toString())) {
                cnt += 1;
            }
        }
        return cnt;
    }

    /* get */
    @Override
    public int getTransactionCount(String address, String tag) {
        int blockNumber;
        if ("latest".equals(tag)) {
            blockNumber = 1;
        } else {
            blockNumber = -1;
        }
        BlockHusk block = nodeManager.getBlockByIndexOrHash(String.valueOf(blockNumber));
        return getCount(address, block.getBody());
    }

    @Override
    public int getTransactionCount(String address, int blockNumber) {
        BlockHusk block = nodeManager.getBlockByIndexOrHash(String.valueOf(blockNumber));
        return getCount(address, block.getBody());
    }

    @Override
    public int getBlockTransactionCountByHash(String hashOfBlock) {
        BlockHusk block = nodeManager.getBlockByIndexOrHash(hashOfBlock);
        return block.getBody().size();
    }

    @Override
    public int getBlockTransactionCountByNumber(int blockNumber) {
        return getBlockTransactionCountByHash(String.valueOf(blockNumber));
    }

    @Override
    public int getBlockTransactionCountByNumber(String tag) {
        if ("latest".equals(tag)) {
            return getBlockTransactionCountByNumber(0);
        } else {
            return 0;
        }
    }

    @Override
    public TransactionHusk getTransactionByHash(String hashOfTx) {
        TransactionHusk tx = nodeManager.getTxByHash(hashOfTx);
        if (tx == null) {
            throw new NonExistObjectException("Transaction");
        }
        return tx;
    }

    @Override
    public TransactionHusk getTransactionByBlockHashAndIndex(
            String hashOfBlock, int txIndexPosition) {
        BlockHusk block = nodeManager.getBlockByIndexOrHash(hashOfBlock);
        return block.getBody().get(txIndexPosition);
    }

    @Override
    public TransactionHusk getTransactionByBlockNumberAndIndex(
            int blockNumber, int txIndexPosition) {
        BlockHusk block = nodeManager.getBlockByIndexOrHash(String.valueOf(blockNumber));
        return block.getBody().get(txIndexPosition);
    }

    @Override
    public TransactionHusk getTransactionByBlockNumberAndIndex(String tag, int txIndexPosition) {
        if ("latest".equals(tag)) {
            return getTransactionByBlockNumberAndIndex(0, txIndexPosition);
        } else {
            return null;
        }
    }

    /* send */
    @Override
    public String sendTransaction(TransactionDto tx) {
        TransactionHusk addedTx = nodeManager.addTransaction(TransactionDto.of(tx));
        return addedTx.getHash().toString();
    }

    @Override
    public byte[] sendRawTransaction(byte[] bytes) {
        TransactionHusk tx = convert(bytes);
        TransactionHusk addedTx = nodeManager.addTransaction(tx);
        return addedTx.getHash().getBytes();
    }

    /* filter */
    @Override
    public int newPendingTransactionFilter() {
        return 6;
    }

    private TransactionHusk convert(byte[] bytes) {

        //todo: change method to transaction class method

        int sum = 0;

        byte[] chain = new byte[20];
        chain = Arrays.copyOfRange(bytes, sum, sum += chain.length);
        byte[] version = new byte[8];
        version = Arrays.copyOfRange(bytes, sum, sum += version.length);
        byte[] type = new byte[8];
        type = Arrays.copyOfRange(bytes, sum, sum += type.length);
        byte[] timestamp = new byte[8];
        timestamp = Arrays.copyOfRange(bytes, sum, sum += timestamp.length);
        byte[] bodyHash = new byte[32];
        bodyHash = Arrays.copyOfRange(bytes, sum, sum += bodyHash.length);
        byte[] bodyLength = new byte[8];
        bodyLength = Arrays.copyOfRange(bytes, sum, sum += bodyLength.length);
        byte[] signature = new byte[65];
        signature = Arrays.copyOfRange(bytes, sum, sum += signature.length);
        byte[] body = Arrays.copyOfRange(bytes, sum, bytes.length);

        Proto.Transaction.Header transactionHeader = Proto.Transaction.Header.newBuilder()
                .setChain(ByteString.copyFrom(chain))
                .setVersion(ByteString.copyFrom(version))
                .setType(ByteString.copyFrom(type))
                .setTimestamp(ByteString.copyFrom(timestamp))
                .setBodyHash(ByteString.copyFrom(bodyHash))
                .setBodyLength(ByteString.copyFrom(bodyLength))
                .build();

        Proto.Transaction tx = Proto.Transaction.newBuilder()
                .setHeader(transactionHeader)
                .setSignature(ByteString.copyFrom(signature))
                .setBody(ByteString.copyFrom(body))
                .build();

        return new TransactionHusk(tx);
    }

    @Override
    public Map<String, TransactionReceipt> getAllTransactionReceipt() {
        return txReceiptStore.getTxReceiptStore();
    }

    @Override
    public TransactionReceipt getTransactionReceipt(String hashOfTx) {
        return txReceiptStore.get(hashOfTx);
    }
}