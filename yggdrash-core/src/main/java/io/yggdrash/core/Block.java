package io.yggdrash.core;

import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.Transactions;

import java.io.Serializable;

public class Block implements Serializable {

    // Header
    private BlockHeader header;

    // Data
    private Transactions data;


    // Constructor

    public Block(BlockHeader header, Transactions data) {
        this.header = header;
        this.data = data;
    }

//    public Block() {
//        this.index = getNextBlockIndex();
//        this.timestamp = getCurrentTime();
//        this.pre_block_hash = getPreBlockHash();
//        this.author = getAccount();
//
//        this.transactions = getNewTransactions();
//        this.merkle_root = getMerkleRoot();
//        this.data_size = this.transactions.size();
//        this.signature = getSignature();
//    }


    // Method


//    public boolenan makeBlock(byte[] header, Transactions data){
//
//
//
//        return ture;
//    }



}
