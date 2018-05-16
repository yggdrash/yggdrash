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

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("transactionList=>");
        for (Transaction tx : this.txs) {
            buffer.append(tx.toString());
        }
        return buffer.toString();
    }

}




