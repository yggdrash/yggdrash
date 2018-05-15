package io.yggdrash.core;

import io.yggdrash.trie.Trie;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Transactions implements Serializable {

    // <Variable>
    private List<Transaction> txs;

    // Constructor


    public Transactions(List<Transaction> txs) {
        this.txs = txs;
    }

    // generate Txs for testing
    public Transactions(String data) throws IOException {
        // TODO 제거
        List<Transaction> txs_list;
        txs_list = new ArrayList<Transaction>();
        txs_list.add(new Transaction(data));

        this.txs = txs_list;
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




