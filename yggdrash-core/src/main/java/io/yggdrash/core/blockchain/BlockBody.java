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
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.trie.Trie;
import io.yggdrash.core.exception.InternalErrorException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BlockBody {

    private final List<Transaction> body = new ArrayList<>();

    private byte[] binary;

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
        if (bodyBytes.length <= TransactionHeader.LENGTH + Constants.SIGNATURE_LENGTH) {
            return;
        }

        int pos = 0;
        byte[] txHeaderBytes = new byte[TransactionHeader.LENGTH];
        byte[] txSigBytes = new byte[Constants.SIGNATURE_LENGTH];
        byte[] txBodyBytes;

        TransactionHeader txHeader;
        TransactionBody txBody;

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

            body.add(new Transaction(txHeader, txSigBytes, txBody));
        } while (pos < bodyBytes.length);
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
        return Trie.getMerkleRoot(this.body);
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

    public byte[] toBinary() {
        if (binary != null) {
            return binary;
        }
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        try {
            for (Transaction tx : this.body) {
                bao.write(tx.toBinary());
            }
            binary = bao.toByteArray();
            return binary;
        } catch (IOException e) {
            throw new InternalErrorException("toBinary error");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BlockBody other = (BlockBody) o;
        if (this.body.size() != other.getBody().size()) {
            return false;
        }

        for (int i = 0; i < other.getBody().size(); i++) {
            if (!Arrays.equals(this.getBody().get(i).getHash(),
                    other.getBody().get(i).getHash())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(toBinary());
    }

    public void clear() {
        this.body.clear();
    }
}




