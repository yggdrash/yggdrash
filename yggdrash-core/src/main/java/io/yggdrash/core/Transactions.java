package io.yggdrash.core;

import io.yggdrash.trie.Trie;

import java.io.Serializable;
import java.util.List;

public class Transactions implements Serializable {

    // <Variable>
    private List<Transaction> txs;

    // Constructor


    public Transactions(List<Transaction> txs) {
        this.txs = txs;
    }

    // Method
    public void addTransaction(Transaction tx) {
        this.txs.add(tx);
    }

    public void delTransaction(Transaction tx) {
        this.txs.remove(tx);
    }

    public byte[] getMerkleRoot() {
        return Trie.getMercleRoot(this);
    }

    public long getSize() {
        return this.txs.size(); // check byte
    }

    public void printTransactions() {
        System.out.println("TXs");
        for (Transaction tx : this.txs) {
            tx.printTransaction();
        }
    }

    public byte[] getMerkleRoot(Transactions txs) {
        return "merkleroot1234567890123456789012".getBytes();
    }
}




