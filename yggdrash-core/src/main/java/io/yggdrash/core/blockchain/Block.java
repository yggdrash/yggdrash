package io.yggdrash.core.blockchain;

import java.io.Serializable;
import java.util.Arrays;


/**
 * Block
 */
public class Block implements Serializable {

    private String hash;
    private int height;
    private long timestamp;
    private Transaction[] transactions;
    private String mercleTreeHash;

    // ADD transactions
    public void addTransactions(Transaction[] transactions) {
        System.arraycopy(transactions, 0, this.transactions, 0, transactions.length);

        // rebuild mercleTreeHash
        this.makeMercleTreeHash();

    }

    private void makeMercleTreeHash() {
        // make mercleTree
    }

    public String getMercleTreeHash() {
        return this.mercleTreeHash;
    }



    public boolean validate(Block block) {
        return true;
    }
}
