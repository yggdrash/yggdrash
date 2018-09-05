package io.yggdrash.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.trie.Trie;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;

public class BlockBody implements Cloneable {

    private List<Transaction> body;

    /**
     * Constructor for BlockBody class.
     *
     * @param transactionList the transaction list
     */
    public BlockBody(List<Transaction> transactionList) {

        this.body = transactionList;
    }

    public BlockBody(JsonArray jsonArray) throws SignatureException {

        this.body = new ArrayList<>();

        for (int i = 0;  i < jsonArray.size(); i++) {
            this.body.add(new Transaction(jsonArray.get(i).getAsJsonObject()));
        }
    }

    public List<Transaction> getBody() {
        return this.body;
    }

    public long getBodyCount() {
        return this.body.size();
    }

    /**
     * Get the length of BlockBody.
     *
     * @return the BlockBody length.
     */
    public long length() throws IOException {

        long length = 0;

        for (Transaction tx : this.body) {
            length += tx.length();
        }

        return length;
    }

    public byte[] getMerkleRoot() throws IOException {
        return Trie.getMerkleRoot(this.body);
    }

    /**
     * Covert BlockBody.class to JsonArray
     *
     * @return blockbody as JsonArray
     */
    public JsonArray toJsonArray() {
        JsonArray jsonArray = new JsonArray();

        for (Transaction tx : this.body) {
            jsonArray.add(tx.toJsonObject());
        }

        return jsonArray;
    }

    public String toString() {
        return this.toJsonArray().toString();
    }

    public byte[] toBinary() throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        for (Transaction tx : this.body) {
            bao.write(tx.toBinary());
        }

        return bao.toByteArray();
    }

    @Override
    public BlockBody clone() throws CloneNotSupportedException {

        BlockBody bb = (BlockBody) super.clone();

        List<Transaction> txs = new ArrayList<>();

        for (Transaction tx : this.body) {
            txs.add(tx.clone());
        }

        bb.body = txs;

        return bb;
    }
}




