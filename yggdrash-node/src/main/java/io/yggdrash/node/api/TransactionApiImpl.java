package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionImpl;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.exception.errorcode.BusinessError;
import io.yggdrash.core.exception.errorcode.SystemError;
import io.yggdrash.gateway.dto.TransactionDto;
import io.yggdrash.gateway.dto.TransactionHeaderRawDto;
import io.yggdrash.gateway.dto.TransactionRawDto;
import io.yggdrash.gateway.dto.TransactionReceiptDto;
import io.yggdrash.gateway.dto.TransactionResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AutoJsonRpcServiceImpl
public class TransactionApiImpl implements TransactionApi {
    private static final Logger log = LoggerFactory.getLogger(TransactionApiImpl.class);

    private final BranchGroup branchGroup;

    @Autowired
    public TransactionApiImpl(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    /* get */
    @Override
    public int getTransactionCountByBlockHash(String branchId, String blockId) {
        ConsensusBlock block = branchGroup.getBlockByHash(BranchId.of(branchId), blockId);
        return block.getBody().getCount();
    }

    @Override
    public int getTransactionCountByBlockNumber(String branchId, long blockNumber) {
        ConsensusBlock block = branchGroup.getBlockByIndex(BranchId.of(branchId), blockNumber);
        return block.getBody().getCount();
    }

    @Override
    public int getTransactionCountByBlockNumber(String branchId, String tag) {
        if ("latest".equals(tag)) {
            return getTransactionCountByBlockNumber(branchId, 0);
        } else {
            return 0;
        }
    }

    @Override
    public TransactionDto getTransactionByHash(String branchId, String txId) {
        Transaction tx = branchGroup.getTxByHash(BranchId.of(branchId), txId);
        if (tx == null) {
            throw new NonExistObjectException("Transaction");
        }
        return TransactionDto.createBy(tx);
    }

    @Override
    public TransactionDto getTransactionByBlockHash(String branchId, String blockId,
                                                     int txIndexPosition) {
        ConsensusBlock block = branchGroup.getBlockByHash(BranchId.of(branchId), blockId);
        List<Transaction> txList = block.getBody().getTransactionList();
        return TransactionDto.createBy(txList.get(txIndexPosition));
    }

    @Override
    public TransactionDto getTransactionByBlockNumber(String branchId, long blockNumber,
                                                       int txIndexPosition) {
        ConsensusBlock block = branchGroup.getBlockByIndex(BranchId.of(branchId), blockNumber);
        List<Transaction> txList = block.getBody().getTransactionList();
        return TransactionDto.createBy(txList.get(txIndexPosition));
    }

    @Override
    public TransactionDto getTransactionByBlockNumber(String branchId, String tag,
                                                       int txIndexPosition) {
        if ("latest".equals(tag)) {
            long lastIndex = branchGroup.getLastIndex(BranchId.of(branchId));
            return getTransactionByBlockNumber(branchId, lastIndex, txIndexPosition);
        } else {
            return null;
        }
    }

    @Override
    public TransactionResponseDto sendTransaction(TransactionDto tx) {
        Transaction transaction = TransactionDto.of(tx);
        List<String> errorLogs = new ArrayList<>();
        int verifyResult = branchGroup.addTransaction(transaction);
        if (verifyResult > SystemError.VALID.toValue() || verifyResult > BusinessError.VALID.toValue()) {
            log.error("Error Code[{}]", verifyResult);
            errorLogs = BusinessError.errorLogs(verifyResult);
            return TransactionResponseDto.createBy(tx.txId, false, errorLogs);
        }

        return TransactionResponseDto.createBy(tx.txId, true, errorLogs);
    }

    @Override
    public byte[] sendRawTransaction(byte[] bytes) { //TODO consider return type (no error logs)
        Transaction transaction = TransactionImpl.parseFromRaw(bytes);
        if (branchGroup.getBranch(transaction.getBranchId()) != null) {
            // TODO Transaction Validate
            branchGroup.addTransaction(transaction);
            return transaction.getHash().getBytes();
        } else {
            return "No branch existed".getBytes();
        }
    }

    /* filter */
    @Override
    public int newPendingTransactionFilter(String branchId) {
        return branchGroup.getUnconfirmedTxs(BranchId.of(branchId)).size();
    }

    @Override
    public TransactionReceiptDto getTransactionReceipt(String branchId, String txId) {
        TransactionReceipt receipt = branchGroup.getTransactionReceipt(BranchId.of(branchId), txId);
        return TransactionReceiptDto.createBy(receipt);
    }

    @Override
    public String getRawTransaction(String branchId, String txId) {
        Transaction tx = branchGroup.getTxByHash(BranchId.of(branchId), txId);
        if (tx == null) {
            throw new NonExistObjectException("Transaction");
        }
        byte[] rawTransaction = tx.toRawTransaction();

        return HexUtil.toHexString(rawTransaction);
    }

    @Override
    public String getRawTransactionHeader(String branchId, String txId) {
        Transaction tx = branchGroup.getTxByHash(BranchId.of(branchId), txId);
        if (tx == null) {
            throw new NonExistObjectException("Transaction");
        }
        byte[] rawTransactionHeader = tx.getHeader().toBinary();

        return HexUtil.toHexString(rawTransactionHeader);
    }
}
