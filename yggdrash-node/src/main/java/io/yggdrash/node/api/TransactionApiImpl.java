package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.TransactionReceipt;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.node.controller.TransactionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@AutoJsonRpcServiceImpl
public class TransactionApiImpl implements TransactionApi {

    private final BranchGroup branchGroup;

    @Autowired
    public TransactionApiImpl(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    /* get */
    @Override
    public int getTransactionCount(String branchId, String address, String tag) {
        int blockNumber;
        if ("latest".equals(tag)) {
            blockNumber = 1;
        } else {
            blockNumber = -1;
        }
        BlockHusk block = branchGroup.getBlockByIndex(BranchId.of(branchId), blockNumber);
        return getCount(address, block.getBody());
    }

    @Override
    public int getTransactionCount(String branchId, String address, long blockNumber) {
        BlockHusk block = branchGroup.getBlockByIndex(BranchId.of(branchId), blockNumber);
        return getCount(address, block.getBody());
    }

    @Override
    public int getBlockTransactionCountByHash(String branchId, String hashOfBlock) {
        BlockHusk block = branchGroup.getBlockByHash(BranchId.of(branchId), hashOfBlock);
        return block.getBody().size();
    }

    @Override
    public int getBlockTransactionCountByNumber(String branchId, long blockNumber) {
        BlockHusk block = branchGroup.getBlockByIndex(BranchId.of(branchId), blockNumber);
        return block.getBody().size();
    }

    @Override
    public int getBlockTransactionCountByNumber(String branchId, String tag) {
        if ("latest".equals(tag)) {
            return getBlockTransactionCountByNumber(branchId, 0);
        } else {
            return 0;
        }
    }

    @Override
    public TransactionHusk getTransactionByHash(String branchId, String hashOfTx) {
        TransactionHusk tx = branchGroup.getTxByHash(BranchId.of(branchId), hashOfTx);
        if (tx == null) {
            throw new NonExistObjectException("Transaction");
        }
        return tx;
    }

    @Override
    public TransactionHusk getTransactionByBlockHash(String branchId, String hashOfBlock,
                                                     int txIndexPosition) {
        BlockHusk block = branchGroup.getBlockByHash(BranchId.of(branchId), hashOfBlock);
        return block.getBody().get(txIndexPosition);
    }

    @Override
    public TransactionHusk getTransactionByBlockNumber(String branchId, long blockNumber,
                                                       int txIndexPosition) {
        BlockHusk block = branchGroup.getBlockByIndex(BranchId.of(branchId), blockNumber);
        return block.getBody().get(txIndexPosition);
    }

    @Override
    public TransactionHusk getTransactionByLatestBlock(String branchId, String tag,
                                                       int txIndexPosition) {
        if ("latest".equals(tag)) {
            long lastIndex = branchGroup.getLastIndex(BranchId.of(branchId));
            return getTransactionByBlockNumber(branchId, lastIndex, txIndexPosition);
        } else {
            return null;
        }
    }

    /* send */
    @Override
    public String sendTransaction(TransactionDto tx) {
        TransactionHusk addedTx = branchGroup.addTransaction(TransactionDto.of(tx));
        return addedTx.getHash().toString();
    }

    @Override
    public byte[] sendRawTransaction(byte[] bytes) {
        Transaction tx = new Transaction(bytes);
        TransactionHusk transaction = new TransactionHusk(tx);
        TransactionHusk addedTx = branchGroup.addTransaction(transaction);
        return addedTx.getHash().getBytes();
    }

    /* filter */
    @Override
    public int newPendingTransactionFilter() {
        return 6;
    }

    private int getCount(String address, List<TransactionHusk> txList) {
        int cnt = 0;
        for (TransactionHusk tx : txList) {
            if (address.equals(tx.getAddress().toString())) {
                cnt += 1;
            }
        }
        return cnt;
    }

    @Override
    public Map<String, TransactionReceipt> getAllTransactionReceipt(String branchId) {
        return branchGroup.getTransactionReceiptStore(BranchId.of(branchId)).getTxReceiptStore();
    }

    @Override
    public TransactionReceipt getTransactionReceipt(String branchId, String hashOfTx) {
        return branchGroup.getTransactionReceiptStore(BranchId.of(branchId)).get(hashOfTx);
    }
}