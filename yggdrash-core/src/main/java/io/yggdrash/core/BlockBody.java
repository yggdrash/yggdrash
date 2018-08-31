package io.yggdrash.core;

import com.google.gson.JsonArray;
import io.yggdrash.trie.Trie;

import java.io.IOException;
import java.util.List;

public class BlockBody implements Cloneable {

    private List<Transaction> transactionList;

    /**
     * Constructor for BlockBody class.
     *
     * @param transactionList the transaction list
     */
    public BlockBody(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }

    public List<Transaction> getTransactionList() {
        return transactionList;
    }

    public byte[] getMerkleRoot() throws IOException {
            return Trie.getMerkleRoot(this.transactionList);
    }

    /**
     * Get the length of BlockBody.
     *
     * @return the BlockBody length.
     */
    public long length() {

        long length = 0;

        for (Transaction tx : this.transactionList) {
            length += tx.length();
        }

        return length;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("transactionList=>");
        for (Transaction tx : this.transactionList) {
            buffer.append(tx.toString());
        }
        return buffer.toString();
    }

    /**
     * Covert BlockBody.class to JsonArray
     * @return blockbody as JsonArray
     */
    public JsonArray toJsonArray() {
        JsonArray jsonArray = new JsonArray();

        for (Transaction tx : this.transactionList) {
            jsonArray.add(tx.toJsonObject());
        }

        return jsonArray;
    }

    @Override
    public BlockBody clone() throws CloneNotSupportedException {

        BlockBody bb = (BlockBody) super.clone();

        for(Transaction tx : this.transactionList) {
            bb.transactionList.add(tx.clone());
        }

        return bb;
    }
}




