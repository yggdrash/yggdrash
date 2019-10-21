package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.ReceiptImpl;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionImpl;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.exception.errorcode.BusinessError;
import io.yggdrash.gateway.dto.TransactionDto;
import io.yggdrash.gateway.dto.TransactionReceiptDto;
import io.yggdrash.gateway.dto.TransactionResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        try {
            Transaction transaction = TransactionDto.of(tx);
            return addTx(transaction);
        } catch (Exception e) {
            log.debug("SendTransaction Exception : {}", e.getMessage());
            return TransactionResponseDto.createBy(
                    BusinessError.getErrorLogsMap(BusinessError.INVALID_DATA_FORMAT.toValue()));
        }
    }

    @Override
    public TransactionResponseDto sendRawTransaction(byte[] bytes) {
        try {
            Transaction transaction = TransactionImpl.parseFromRaw(bytes);
            return addTx(transaction);
        } catch (Exception e) {
            log.debug("SendRawTransaction Exception : {}", e.getMessage());
            return TransactionResponseDto.createBy(
                    BusinessError.getErrorLogsMap(BusinessError.INVALID_DATA_FORMAT.toValue()));
        }
    }

    private TransactionResponseDto addTx(Transaction transaction) {
        try {
            if (!branchGroup.getBranch(transaction.getBranchId()).isFullSynced()) {
                log.debug("SendRawTransaction is failed. Not yet fullSynced. {}", transaction.getBranchId().toString());
                return TransactionResponseDto.createBy(transaction.getHash().toString(), false,
                        BusinessError.getErrorLogsMap(BusinessError.UNDEFINED_ERROR.toValue()
                        ));
            }
        } catch (Exception e) {
            log.debug("SendTx failed. {}", e.getMessage());
            return TransactionResponseDto.createBy(transaction.getHash().toString(), false,
                    BusinessError.getErrorLogsMap(BusinessError.UNDEFINED_ERROR.toValue()));
        }

        Map<String, List<String>> errorLogs = branchGroup.addTransaction(transaction);

        if (errorLogs.size() > 0) {
            log.warn("AddTx Error : {}", errorLogs);
        }

        return errorLogs.size() > 0
                ? TransactionResponseDto.createBy(transaction.getHash().toString(), false, errorLogs)
                : TransactionResponseDto.createBy(transaction.getHash().toString(), true, errorLogs);
    }

    /* filter */
    @Override
    public int newPendingTransactionFilter(String branchId) {
        return branchGroup.getUnconfirmedTxs(BranchId.of(branchId)).size();
    }

    @Override
    public List<String> getPendingTransactionList(String branchId) {
        return branchGroup.getUnconfirmedTxs(BranchId.of(branchId))
                .stream().map(tx -> tx.getHash().toString()).collect(Collectors.toList());
    }

    @Override
    public int getPendingTransactionCount(String branchId) {
        return branchGroup.getUnconfirmedTxs((BranchId.of(branchId))).size();
    }

    @Override
    public TransactionReceiptDto getTransactionReceipt(String branchId, String txId) {
        Receipt receipt;
        try {
            receipt = branchGroup.getReceipt(BranchId.of(branchId), txId);
        } catch (NullPointerException ex) {
            receipt = new ReceiptImpl();
            log.debug("Null Receipt : {}", txId);
        }
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
        byte[] rawTransactionBinary = tx.getHeader().getBinaryForSigning();
        byte[] transactionSign = tx.getSignature();

        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        try {
            bao.write(rawTransactionBinary);
            bao.write(transactionSign);
        } catch (IOException e) {
            throw new NotValidateException();
        }

        byte[] rawTransactionHeader = bao.toByteArray();

        return HexUtil.toHexString(rawTransactionHeader);
    }
}
