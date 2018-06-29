package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;

@JsonRpcService("/transaction")
public interface TransactionAPI {

    /* get */
    int getTransactionCount(@JsonRpcParam(value = "address") String address, @JsonRpcParam(value = "tag") String tag);
    int getTransactionCount(@JsonRpcParam(value = "address") String address, @JsonRpcParam(value = "blockNumber") int blockNumber);
    int getBlockTransactionCountByHash(@JsonRpcParam(value = "hashOfBlock") String hashOfBlock);
    int getBlockTransactionCountByNumber(@JsonRpcParam(value = "blockNumber") int blockNumber);
    int getBlockTransactionCountByNumber(@JsonRpcParam(value = "tag") String tag);
    String getTransactionByHash(@JsonRpcParam(value = "hashOfTx") String hashOfTx);
    String getTransactionByBlockHashAndIndex(@JsonRpcParam(value = "hashOfBlock") String hashOfBlock, @JsonRpcParam(value = "txIndexPosition") int txIndexPosition);
    String getTransactionByBlockNumberAndIndex(@JsonRpcParam(value = "blockNumber") int blockNumber, @JsonRpcParam(value = "txIndexPosition") int txIndexPosition);
    String getTransactionByBlockNumberAndIndex(@JsonRpcParam(value = "tag") String tag, @JsonRpcParam(value = "txIndexPosition") int txIndexPosition);
    String getTransactionReceipt(@JsonRpcParam(value = "hashOfTx") String hashOfTx);

    /* send */
    String sendTransaction(@JsonRpcParam(value = "tx") String tx);
    String sendRawTransaction(@JsonRpcParam(value = "rawTx") String rawTx);

    /* filter */
    int newPendingTransactionFilter();
}


/*
    getTransactionCount()
    - Returns the number of transactions sent from an address.
    - params : DATA, (20 Bytes) address
               QUANTITY|TAG - integer block number, or the string "latest", "earliest", or "pending"
    - returns : QUNATITY, integer of the number of transactions send from this addresses

    getBlockTransactionCountByHash()
    - Returns the number of transactions in a block from a block matching the given block hash.
    - params : DATA, 32 Bytes - hash of a block
    - returns : integer of the number of transactions in this block

    getBlockTransactionCountByNumber()
    - Returns the number of transactions in a block matching the given block number.
    - params : QUANTITY|TAG - integer of a block number, or the string "earliest", "latest" or "pending"
    - returns : QUANTITY - integer of the number of transactions in this block.

    getTransactionByHash()
    - Returns the information about a transaction requested by transaction hash.
    - params : DATA, 32 Bytes - hash of a transaction
    - returns : Object - A transaction object, or null when no transaction was found:

    getTransactionByBlockHashAndIndex()
    - Returns information about a transaction by block hash and transaction index position.
    - params : DATA, 32 Bytes - hash of a block.
               QUANTITY - integer of the transaction index position.
    - returns : Object - A transaction object, or null when no transaction was found:

    getTransactionByBlockNumberAndIndex()
    - Returns information about a transaction by block number and transaction index position.
    - params : QUANTITY|TAG - a block number, or the string "earliest", "latest" or "pending"
               QUANTITY - the transaction index position.
    - returns : Object - A transaction object, or null when no transaction was found:

    getTransactionReceipt()
    - Returns the receipt of a transaction by transaction hash
      (Note That the receipt is not available for pending transactions.)
    - params : DATA, 32 Bytes - hash of a transaction
    - returns : Object - A transaction receipt object, or null when no receipt was found:


    sendTransaction()
    - Creates new message call transaction or a contract creation, if the data field contains code.
    - params : Object - The transaction object
    - returns : DATA, 32 Bytes - the transaction hash, or the zero hash if the transaction is not yet available.

    sendRawTransaction()
    - Creates new message call transaction or a contract creation for signed transactions.
    - params : DATA, The signed transaction data.
    - returns : DATA, 32 Bytes - the transaction hash, or the zero hash if the transaction is not yet available


    newPendingTransactionFilter()
    - Creates a filter in the node, to notify when new pending transactions arrive.
    - Params : none
    - returns : QUANTITY - A filter id.
 */
