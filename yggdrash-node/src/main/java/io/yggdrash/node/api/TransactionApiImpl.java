package io.yggdrash.node.api;

import com.google.common.primitives.Longs;
import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionHeader;
import io.yggdrash.core.TransactionReceipt;
import io.yggdrash.node.exception.NonExistObjectException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.spongycastle.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SignatureException;

@Service
@AutoJsonRpcServiceImpl
public class TransactionApiImpl implements TransactionApi {

    private final NodeManager nodeManager;

    @Autowired
    public TransactionApiImpl(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public int getCount(String address, BlockBody blockBody) {
        Integer cnt = 0;
        for (Transaction tx : blockBody.getTransactionList()) {
            try {
                if (Arrays.areEqual(Hex.decodeHex(address), tx.getHeader().getAddress())) {
                    cnt += 1;
                }
            } catch (DecoderException e) {
                e.printStackTrace();
            }
        }
        return cnt;
    }

    /* get */
    @Override
    public int getTransactionCount(String address, String tag) {
        Integer blockNumber;
        if ("latest".equals(tag)) {
            blockNumber = 0;
        } else {
            blockNumber = -1;
        }
        Block block = nodeManager.getBlockByIndexOrHash(String.valueOf(blockNumber));
        return getCount(address, block.getData());
    }

    @Override
    public int getTransactionCount(String address, int blockNumber) {
        Block block = nodeManager.getBlockByIndexOrHash(String.valueOf(blockNumber));
        return getCount(address, block.getData());
    }

    @Override
    public int getBlockTransactionCountByHash(String hashOfBlock) {
        Block block = nodeManager.getBlockByIndexOrHash(hashOfBlock);
        BlockBody txList = block.getData();
        return txList.getTransactionList().size();
    }

    @Override
    public int getBlockTransactionCountByNumber(int blockNumber) {
        Block block = nodeManager.getBlockByIndexOrHash(String.valueOf(blockNumber));
        BlockBody txList = block.getData();
        return txList.getTransactionList().size();
    }

    @Override
    public int getBlockTransactionCountByNumber(String tag) {
        if ("latest".equals(tag)) {
            Block block = nodeManager.getBlockByIndexOrHash(String.valueOf(0));
            BlockBody txList = block.getData();
            return txList.getTransactionList().size();
        }
        return 0;
    }

    @Override
    public Transaction getTransactionByHash(String hashOfTx) {
        Transaction tx = nodeManager.getTxByHash(hashOfTx);
        if (tx == null) {
            throw new NonExistObjectException("Transaction");
        }
        return tx;
    }

    @Override
    public Transaction getTransactionByBlockHashAndIndex(
            String hashOfBlock, int txIndexPosition) throws IOException {
        Block block = nodeManager.getBlockByIndexOrHash(hashOfBlock);
        BlockBody txList = block.getData();
        return txList.getTransactionList().get(txIndexPosition);
    }

    @Override
    public Transaction getTransactionByBlockNumberAndIndex(
            int blockNumber, int txIndexPosition) throws IOException {
        Block block = nodeManager.getBlockByIndexOrHash(String.valueOf(blockNumber));
        BlockBody txList = block.getData();
        return txList.getTransactionList().get(txIndexPosition);
    }

    @Override
    public Transaction getTransactionByBlockNumberAndIndex(String tag, int txIndexPosition)
            throws IOException {
        if ("latest".equals(tag)) {
            int blockNumber = 0;
            Block block = nodeManager.getBlockByIndexOrHash(String.valueOf(blockNumber));
            BlockBody txList = block.getData();
            return txList.getTransactionList().get(txIndexPosition);
        }
        return null;
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

        return new Transaction(nodeManager.getWallet(), txObj);
    }
}