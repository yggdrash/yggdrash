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
import io.yggdrash.crypto.ECKey;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.BlockChainProto.TransactionHeader;
import io.yggdrash.proto.BlockChainProto.Transaction;

public class TransactionHusk implements ProtoHusk<Transaction> {
    private Transaction transaction;

    public TransactionHusk(Transaction transaction) {
        this.transaction = transaction;
    }

    public TransactionHusk(byte[] data) throws InvalidProtocolBufferException {
        this.transaction = Transaction.parseFrom(data);
    }

    public void sign(byte[] privateKey) {
        ECKey ecKey = ECKey.fromPrivate(privateKey);
        ECKey.ECDSASignature signature = ecKey.sign(
                HashUtil.sha3(this.transaction.toByteArray()));
        ByteString sig = ByteString.copyFrom(signature.toByteArray());
        getHeader().toBuilder().setSignature(sig).build();
    }

    private TransactionHeader getHeader() {
        return this.transaction.getHeader();
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
}
