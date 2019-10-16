package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.core.exception.NonExistObjectException;
import io.yggdrash.core.exception.RejectedAccessException;
import io.yggdrash.gateway.dto.TransactionDto;
import io.yggdrash.gateway.dto.TransactionReceiptDto;
import io.yggdrash.gateway.dto.TransactionResponseDto;

import java.util.List;

import static io.yggdrash.common.config.Constants.BLOCK_ID;
import static io.yggdrash.common.config.Constants.BRANCH_ID;
import static io.yggdrash.common.config.Constants.TX_ID;

@JsonRpcService("/api/transaction")
public interface TransactionApi {

    /* get */
    /**
     * Returns the number of transactions in a block from a block matching the given block hash.
     *
     * @param blockId hash of block
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.CODE)})
    int getTransactionCountByBlockHash(@JsonRpcParam(value = BRANCH_ID) String branchId,
                                       @JsonRpcParam(value = BLOCK_ID) String blockId);

    /**
     * Returns the number of transactions in a block matching the given block number.
     *
     * @param blockNumber integer of block number
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.CODE)})
    int getTransactionCountByBlockNumber(@JsonRpcParam(value = BRANCH_ID) String branchId,
                                         @JsonRpcParam(value = "blockNumber") long blockNumber);

    /**
     * Returns the information about a transaction requested by transaction hash.
     *
     * @param txId hash of transaction
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.CODE)})
    TransactionDto getTransactionByHash(@JsonRpcParam(value = BRANCH_ID) String branchId,
                                         @JsonRpcParam(value = TX_ID) String txId);

    /**
     * Returns information about a transaction by block hash and transaction index position.
     *
     * @param blockId     hash of block
     * @param txIndexPosition integer of the transaction index position.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.CODE)})
    TransactionDto getTransactionByBlockHash(@JsonRpcParam(value = BRANCH_ID) String branchId,
                                      @JsonRpcParam(value = BLOCK_ID) String blockId,
                                      @JsonRpcParam(value = "txIndexPosition") int txIndexPosition);

    /**
     * Returns information about a transaction by block number and transaction index position.
     *
     * @param blockNumber     a block number
     * @param txIndexPosition the transaction index position.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.CODE)})
    TransactionDto getTransactionByBlockNumber(@JsonRpcParam(value = BRANCH_ID) String branchId,
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
                    code = NonExistObjectException.CODE)})
    TransactionDto getTransactionByBlockNumber(@JsonRpcParam(value = BRANCH_ID) String branchId,
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
                    code = FailedOperationException.CODE)})
    TransactionResponseDto sendTransaction(@JsonRpcParam(value = "tx") TransactionDto tx);

    /**
     * Creates new message call transaction or a contract creation for signed transactions.
     *
     * @param rawTx The signed transaction data.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = FailedOperationException.class,
                    code = FailedOperationException.CODE)})
    TransactionResponseDto sendRawTransaction(@JsonRpcParam(value = "rawTx") byte[] rawTx);

    /**
     * Creates a filter in the node, to notify when new pending transactions arrive.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = RejectedAccessException.class,
                    code = RejectedAccessException.CODE)})
    int newPendingTransactionFilter(@JsonRpcParam(value = BRANCH_ID) String branchId);

    /**
     * Returns the list of pending transactions in the current blockChain
     *
     * @param branchId branchId
     * @return The pending transaction hash list
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.CODE)})
    List<String> getPendingTransactionList(@JsonRpcParam(value = BRANCH_ID) String branchId);

    /**
     * Returns the list of pending transactions in the current blockChain
     *
     * @param branchId branchId
     * @return The pending transaction size
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.CODE)})
    int getPendingTransactionCount(@JsonRpcParam(value = BRANCH_ID) String branchId);


    /**
     * Returns the Receipt of transaction hash
     *
     * @param txId  hash of transaction
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.CODE)})
    TransactionReceiptDto getTransactionReceipt(@JsonRpcParam(value = BRANCH_ID) String branchId,
                                                @JsonRpcParam(value = TX_ID) String txId);

    /**
     * Returns the information about a raw transaction requested by transaction hash.
     *
     * @param txId hash of transaction
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.CODE)})
    String getRawTransaction(@JsonRpcParam(value = BRANCH_ID) String branchId,
                             @JsonRpcParam(value = TX_ID) String txId);

    /**
     * Returns the information about a raw transaction header requested by transaction hash.
     *
     * @param txId hash of transaction
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                    code = NonExistObjectException.CODE)})
    String getRawTransactionHeader(@JsonRpcParam(value = BRANCH_ID) String branchId,
                                   @JsonRpcParam(value = TX_ID) String txId);

}
