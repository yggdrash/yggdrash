/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.husk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.BlockChainProto.Transaction;
import io.yggdrash.proto.BlockChainProto.TransactionHeader;
import io.yggdrash.util.TimeUtils;

import java.nio.ByteBuffer;
import java.util.Objects;

public class TransactionHusk implements ProtoHusk<Transaction> {
    private Transaction transaction;

    public TransactionHusk(Transaction transaction) {
        this.transaction = transaction;
    }

    public TransactionHusk(byte[] data) throws InvalidProtocolBufferException {
        this.transaction = Transaction.parseFrom(data);
    }

    public TransactionHusk(String body) {
        TransactionHeader txHeaderPrototype = TransactionHeader.newBuilder()
                .setType(ByteString.copyFrom(ByteBuffer.allocate(4).putInt(1).array()))
                .setVersion(ByteString.copyFrom(ByteBuffer.allocate(4).putInt(1).array()))
                .setDataHash(ByteString.copyFrom(HashUtil.sha3(body.getBytes())))
                .setTimestamp(TimeUtils.time())
                .setDataSize(body.getBytes().length)
//                .setSignature()
                .build();
        this.transaction = Transaction.newBuilder()
                .setHeader(txHeaderPrototype)
                .setData(body).build();
    }

    private TransactionHeader getHeader() {
        return this.transaction.getHeader();
    }

    public Sha3Hash getHash() {
        return new Sha3Hash(getHeader().toByteArray());
    }

    public String getBody() {
        return this.transaction.getData();
    }

    @Override
    public byte[] getData() {
        return this.transaction.toByteArray();
    }

    @Override
    public Transaction getInstance() {
        return this.transaction;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TransactionHusk{");
        sb.append("transaction=").append(transaction);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransactionHusk that = (TransactionHusk) o;
        return Objects.equals(transaction, that.transaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transaction);
    }
}
