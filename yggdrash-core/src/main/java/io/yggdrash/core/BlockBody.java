package io.yggdrash.core;

import io.yggdrash.trie.Trie;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BlockBody implements Serializable {

    // <Variable>
    private List<Transaction> txs;

    // Constructor
    public BlockBody(List<Transaction> txs) {
        this.txs = txs;
    }

    // <Get_Set Method>
    public List<Transaction> getTxs() {
        return txs;
    }

    public void setTxs(List<Transaction> txs) {
        this.txs = txs;
    }

    public byte[] getMerkleRoot() {
        return Trie.getMerkleRoot(this.txs);
    }

    public long getSize() {
        return this.txs.size(); // check byte
    }

    public void printTransactions() {
        // TODO convert toString overwrite
        System.out.println("TXs");
        for (Transaction tx : this.txs) {
            tx.printTransaction();
        }
    }

}




