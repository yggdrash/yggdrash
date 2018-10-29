package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.contract.TransactionReceipt;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.exception.RejectedAccessException;
import io.yggdrash.node.controller.TransactionDto;

import java.util.Map;

@JsonRpcService("/api/transaction")
public interface TransactionApi {

    /* get */
    /**
     * Returns the number of transactions in a block from a block matching the given block hash.
     *
     * @param hashOfBlock hash of block
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    int getTransactionCountByBlockHash(@JsonRpcParam(value = "branchId") String branchId,
                                       @JsonRpcParam(value = "hashOfBlock") String hashOfBlock);

    /**
     * Returns the number of transactions in a block matching the given block number.
     *
     * @param blockNumber integer of block number
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    int getTransactionCountByBlockNumber(@JsonRpcParam(value = "branchId") String branchId,
                                         @JsonRpcParam(value = "blockNumber") long blockNumber);

    /**
     * Returns the number of transactions in a block matching the given block number.
     *
     * @param tag "latest","earliest","pending"
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    int getTransactionCountByBlockNumber(@JsonRpcParam(value = "branchId") String branchId,
                                         @JsonRpcParam(value = "tag") String tag);

    /**
     * Returns the information about a transaction requested by transaction hash.
     *
     * @param hashOfTx hash of transaction
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    TransactionDto getTransactionByHash(@JsonRpcParam(value = "branchId") String branchId,
                                         @JsonRpcParam(value = "hashOfTx") String hashOfTx);

    /**
     * Returns information about a transaction by block hash and transaction index position.
     *
     * @param hashOfBlock     hash of block
     * @param txIndexPosition integer of the transaction index position.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    TransactionDto getTransactionByBlockHash(@JsonRpcParam(value = "branchId") String branchId,
                                      @JsonRpcParam(value = "hashOfBlock") String hashOfBlock,
                                      @JsonRpcParam(value = "txIndexPosition") int txIndexPosition);

    /**
     * Returns information about a transaction by block number and transaction index position.
     *
     * @param blockNumber     a block number
     * @param txIndexPosition the transaction index position.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    TransactionDto getTransactionByBlockNumber(@JsonRpcParam(value = "branchId") String branchId,
                                    @JsonRpcParam(value = "blockNumber") long blockNumber,
                                    @JsonRpcParam(value = "txIndexPosition") int txIndexPosition);

    /**
     * Returns information about a transaction by block number and transaction index position.
     *
     * @param tag             "latest","earliest","pending"
     * @param txIndexPosition the transaction index position.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    TransactionDto getTransactionByBlockNumber(@JsonRpcParam(value = "branchId") String branchId,
                                    @JsonRpcParam(value = "tag") String tag,
                                    @JsonRpcParam(value = "txIndexPosition") int txIndexPosition);


    /* send */

    /**
     * Creates new message call transaction or a contract creation,
     * if the data field contains code.
     *
     * @param tx The transaction
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = FailedOperationException.class,
                    code = FailedOperationException.code)})
    String sendTransaction(@JsonRpcParam(value = "tx") TransactionDto tx);

    /**
     * Creates new message call transaction or a contract creation for signed transactions.
     *
     * @param rawTx The signed transaction data.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = FailedOperationException.class,
                    code = FailedOperationException.code)})
    byte[] sendRawTransaction(@JsonRpcParam(value = "rawTx") byte[] rawTx);

    /**
     * Creates a filter in the node, to notify when new pending transactions arrive.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = RejectedAccessException.class,
                    code = RejectedAccessException.code)})
    int newPendingTransactionFilter(
            @JsonRpcParam(value = "branchId") String branchId);

    /**
     * Returns all TransactionReceipts
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    Map<String, TransactionReceipt> getAllTransactionReceipt(
            @JsonRpcParam(value = "branchId") String branchId);

    /**
     * Returns the TransactionReceipt of transaction hash
     *
     * @param hashOfTx  hash of transaction
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.code)})
    TransactionReceipt getTransactionReceipt(@JsonRpcParam(value = "branchId") String branchId,
                                             @JsonRpcParam(value = "hashOfTx") String hashOfTx);
}