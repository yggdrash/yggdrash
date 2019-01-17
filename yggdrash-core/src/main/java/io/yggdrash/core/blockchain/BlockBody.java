/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.blockchain;

import com.google.gson.JsonArray;
import io.yggdrash.common.trie.Trie;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockBody implements Cloneable {

    private static final int TX_HEADER_LENGTH = 84;
    private static final int SIGNATURE_LENGTH = 65;

    private List<Transaction> body = new ArrayList<>();

    /**
     * Constructor for BlockBody class.
     *
     * @param transactionList the transaction list
     */
    public BlockBody(List<Transaction> transactionList) {
        this.body.addAll(transactionList);
    }

    public BlockBody(JsonArray jsonArray) {
        for (int i = 0;  i < jsonArray.size(); i++) {
            this.body.add(new Transaction(jsonArray.get(i).getAsJsonObject()));
        }
    }

    public BlockBody(byte[] bodyBytes) {
        if (bodyBytes.length <= TX_HEADER_LENGTH + SIGNATURE_LENGTH) {
            return;
        }

        int pos = 0;
        byte[] txHeaderBytes = new byte[TX_HEADER_LENGTH];
        byte[] txSigBytes = new byte[SIGNATURE_LENGTH];
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
            txBodyBytes = new byte[(int) txHeader.getBodyLength()];
            System.arraycopy(bodyBytes, pos, txBodyBytes, 0, txBodyBytes.length);
            pos += txBodyBytes.length;

            txBody = new TransactionBody(txBodyBytes);

            txList.add(new Transaction(txHeader, txSigBytes, txBody));
        } while (pos < bodyBytes.length);

        this.body.addAll(txList);
    }

    public List<Transaction> getBody() {
        return this.body;
    }

    long getBodyCount() {
        return this.body.size();
    }

    /**
     * Get the length of BlockBody.
     *
     * @return the BlockBody length.
     */
    public long length() {

        long length = 0;

        for (Transaction tx : this.body) {
            length += tx.length();
        }

        return length;
    }

    public byte[] getMerkleRoot() {
        byte[] merkleRoot = Trie.getMerkleRoot(this.body);
        return merkleRoot == null ? new byte[32] : merkleRoot;
    }

    /**
     * Covert BlockBody.class to JsonArray
     *
     * @return block body as JsonArray
     */
    JsonArray toJsonArray() {
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

    public boolean equals(BlockBody newBlockBody) {
        if (this.body.size() != newBlockBody.getBody().size()) {
            return false;
        }

        for (int i = 0; i < newBlockBody.getBody().size(); i++) {
            if (!Arrays.equals(this.getBody().get(i).getHash(),
                    newBlockBody.getBody().get(i).getHash())) {
                return false;
            }
        }

        return true;
    }
}




