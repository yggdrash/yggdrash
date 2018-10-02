package io.yggdrash.core;

import com.google.gson.JsonArray;
import io.yggdrash.trie.Trie;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    public BlockBody(JsonArray jsonArray) {

        this.body = new ArrayList<>();

        for (int i = 0;  i < jsonArray.size(); i++) {
            this.body.add(new Transaction(jsonArray.get(i).getAsJsonObject()));
        }
    }

    public BlockBody(byte[] bodyBytes) {
        int pos = 0;
        byte[] txHeaderBytes = new byte[84];
        byte[] txSigBytes = new byte[65];
        byte[] txBodyBytes;

        TransactionHeader txHeader;
        TransactionBody txBody;
        List<Transaction> txList = new ArrayList<>();

        do {
            System.arraycopy(bodyBytes, pos, txHeaderBytes, 0, txHeaderBytes.length);
            pos += txHeaderBytes.length;
            txHeader = new TransactionHeader(txHeaderBytes);

            System.arraycopy(bodyBytes, pos, txSigBytes, 0, txSigBytes.length);
            pos += txSigBytes.length;

            //todo: change from int to long for body size.
            txBodyBytes = new byte[(int)txHeader.getBodyLength()];
            System.arraycopy(bodyBytes, pos, txBodyBytes, 0, txBodyBytes.length);
            pos += txBodyBytes.length;

            txBody = new TransactionBody(txBodyBytes);

            txList.add(new Transaction(txHeader, txSigBytes, txBody));
        } while (pos >= bodyBytes.length);

        this.body = txList;
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
     * @return block body as JsonArray
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




