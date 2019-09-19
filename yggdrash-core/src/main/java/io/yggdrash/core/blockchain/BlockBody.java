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
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.trie.Trie;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.proto.Proto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockBody implements ProtoObject<Proto.TransactionList> {

    private final Proto.TransactionList transactionList;

    private transient List<Transaction> body;

    public BlockBody(byte[] bytes) {
        this(toProto(bytes));
    }

    public BlockBody(Proto.TransactionList transactionList) {
        this.transactionList = transactionList;
    }

    /**
     * Constructor for BlockBody class.
     *
     * @param transactionList the transaction list
     */
    public BlockBody(List<Transaction> transactionList) {
        this.body = transactionList;
        this.transactionList = toProtoTransactionList(transactionList);
    }

    public BlockBody(JsonArray jsonArray) {
        this(toTransactionList(jsonArray));
    }

    private void setTransactionList() {
        this.body = new ArrayList<>();
        for (Proto.Transaction protoTransaction : transactionList.getTransactionsList()) {
            body.add(new TransactionImpl(protoTransaction));
        }
    }

    public List<Transaction> getTransactionList() {
        if (body == null) {
            setTransactionList();
        }
        return this.body;
    }

    public int getCount() {
        return transactionList.getTransactionsCount();
    }

    /**
     * Get the length of BlockBody.
     *
     * @return the BlockBody length.
     */
    public long getLength() {

        long length = 0;

        for (Transaction tx : getTransactionList()) {
            length += tx.getLength();
        }

        return length;
    }

    public byte[] getMerkleRoot() {
        return Trie.getMerkleRoot(getTransactionList());
    }

    public byte[] getStateRoot() {
        // TODO: change method for stateRoot.
        return Constants.EMPTY_HASH;
    }

    @Override
    public byte[] toBinary() {
        return transactionList.toByteArray();
    }

    @Override
    public Proto.TransactionList getInstance() {
        return transactionList;
    }

    /**
     * Covert BlockBody.class to JsonArray
     *
     * @return block body as JsonArray
     */
    JsonArray toJsonArray() {
        JsonArray jsonArray = new JsonArray();

        for (Transaction tx : getTransactionList()) {
            jsonArray.add(tx.toJsonObject());
        }

        return jsonArray;
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
        return Arrays.equals(toBinary(), other.toBinary());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(toBinary());
    }

    @Override
    public String toString() {
        return toJsonArray().toString();
    }


    private Proto.TransactionList toProtoTransactionList(List<Transaction> transactionList) {
        Proto.TransactionList.Builder builder = Proto.TransactionList.newBuilder();
        for (Transaction transaction : transactionList) {
            builder.addTransactions(transaction.getInstance());
        }
        return builder.build();
    }

    private static List<Transaction> toTransactionList(JsonArray jsonArray) {
        List<Transaction> body = new ArrayList<>();
        for (int i = 0;  i < jsonArray.size(); i++) {
            body.add(new TransactionImpl(jsonArray.get(i).getAsJsonObject()));
        }
        return body;
    }

    private static Proto.TransactionList toProto(byte[] bytes) {
        try {
            return Proto.TransactionList.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new NotValidateException(e);
        }
    }
}
