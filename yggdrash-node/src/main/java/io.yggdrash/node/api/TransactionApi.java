package io.yggdrash.node.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;
import io.yggdrash.core.Transaction;
import io.yggdrash.node.exception.FailedOperationException;
import io.yggdrash.node.exception.NonExistObjectException;
import io.yggdrash.node.exception.RejectedAccessException;
import org.json.simple.parser.ParseException;
import org.junit.internal.runners.statements.Fail;

import java.io.IOException;

@JsonRpcService("/api/transaction")
public interface TransactionApi {

    /* get */
    
    /**
     *  Returns the number of transactions sent from an address.
     *
     * @param address     account address
     * @param tag         "latest","earlest","pending"
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                          code = NonExistObjectException.code)})
    int getTransactionCount(
            @JsonRpcParam(value = "address") String address,
            @JsonRpcParam(value = "tag") String tag);

    /**
     *  Returns information about a block by hash.
     *
     * @param address     account address
     * @param blockNumber integer of block number
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                          code = NonExistObjectException.code)})
    int getTransactionCount(
            @JsonRpcParam(value = "address") String address,
            @JsonRpcParam(value = "blockNumber") int blockNumber);

    /**
     *  Returns the number of transactions in a block from a block matching the given block hash.
     *
     * @param hashOfBlock hash of block
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                          code = NonExistObjectException.code)})
    int getBlockTransactionCountByHash(
            @JsonRpcParam(value = "hashOfBlock") String hashOfBlock);

    /**
     *  Returns the number of transactions in a block matching the given block number.
     *
     * @param blockNumber integer of block number
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                          code = NonExistObjectException.code)})
    int getBlockTransactionCountByNumber(
            @JsonRpcParam(value = "blockNumber") int blockNumber);

    /**
     *  Returns the number of transactions in a block matching the given block number.
     *
     * @param tag         "latest","earlest","pending"
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                          code = NonExistObjectException.code)})
    int getBlockTransactionCountByNumber(
            @JsonRpcParam(value = "tag") String tag);

    /**
     *  Returns the information about a transaction requested by transaction hash.
     *
     * @param hashOfTx    hash of transaction
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                          code = NonExistObjectException.code)})
    String getTransactionByHash(
            @JsonRpcParam(value = "hashOfTx") String hashOfTx) throws IOException;

    /**
     *  Returns information about a transaction by block hash and transaction index position.
     *
     * @param hashOfBlock       hash of block
     * @param txIndexPosition   integer of the transaction index position.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                          code = NonExistObjectException.code)})
    String getTransactionByBlockHashAndIndex(
            @JsonRpcParam(value = "hashOfBlock") String hashOfBlock,
            @JsonRpcParam(value = "txIndexPosition") int txIndexPosition) throws IOException;

    /**
     *  Returns information about a transaction by block number and transaction index position.
     *
     * @param blockNumber       a block number
     * @param txIndexPosition   the transaction index position.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                          code = NonExistObjectException.code)})
    String getTransactionByBlockNumberAndIndex(
            @JsonRpcParam(value = "blockNumber") int blockNumber,
            @JsonRpcParam(value = "txIndexPosition") int txIndexPosition) throws IOException;

    /**
     *   Returns information about a transaction by block number and transaction index position.
     *
     * @param tag               "latest","earlest","pending"
     * @param txIndexPosition   the transaction index position.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = NonExistObjectException.class,
                          code = NonExistObjectException.code)})
    String getTransactionByBlockNumberAndIndex(
            @JsonRpcParam(value = "tag") String tag,
            @JsonRpcParam(value = "txIndexPosition") int txIndexPosition) throws IOException;

    /**
     *  Returns the receipt of a transaction by transaction hash.
     *
     * @param hashOfTx    hash of a transaction
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = FailedOperationException.class,
                          code = FailedOperationException.code)})
    String getTransactionReceipt(
            @JsonRpcParam(value = "hashOfTx") String hashOfTx);

    /* send */

    /**
     *  Creates new message call transaction or a contract creation,
     *  if the data field contains code.
     *
     * @param tx          The transaction object
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = FailedOperationException.class,
                          code = FailedOperationException.code)})
    String sendTransaction(
            @JsonRpcParam(value = "tx") String tx) throws IOException;

    /**
     *  Creates new message call transaction or a contract creation for signed transactions.
     *
     * @param rawTx     The signed transaction data.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = FailedOperationException.class,
                          code = FailedOperationException.code)})
    byte[] sendRawTransaction(
            @JsonRpcParam(value = "rawTx") byte[] rawTx) throws IOException;

    /**
     *  Creates a filter in the node, to notify when new pending transactions arrive.
     */
    @JsonRpcErrors({
            @JsonRpcError(exception = RejectedAccessException.class,
                          code = RejectedAccessException.code)})
    int newPendingTransactionFilter();
}


