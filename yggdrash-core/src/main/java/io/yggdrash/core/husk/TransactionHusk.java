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
import io.yggdrash.core.Wallet;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.Proto;
import io.yggdrash.util.TimeUtils;

import java.nio.ByteBuffer;
import java.security.SignatureException;
import java.util.Objects;

public class TransactionHusk implements ProtoHusk<Proto.TransactionV2> {
    private Proto.TransactionV2 transaction;

    public TransactionHusk(Proto.TransactionV2 transaction) {
        this.transaction = transaction;
    }

    public TransactionHusk(byte[] data) throws InvalidProtocolBufferException {
        this.transaction = Proto.TransactionV2.parseFrom(data);
    }

    public TransactionHusk(String body) {
        Proto.TransactionV2.Header transactionHeader = Proto.TransactionV2.Header.newBuilder()
                .setRawData(Proto.TransactionV2.Header.Raw.newBuilder()
                        .setType(ByteString.copyFrom(ByteBuffer.allocate(4).putInt(1).array()))
                        .setVersion(ByteString.copyFrom(ByteBuffer.allocate(4).putInt(1).array()))
                        .setDataHash(ByteString.copyFrom(HashUtil.sha3(body.getBytes())))
                        .setDataSize(body.getBytes().length)
                        .build())
                .build();
        this.transaction = Proto.TransactionV2.newBuilder()
                .setHeader(transactionHeader)
                .setBody(body)
                .build();
    }

    public void sign(Wallet wallet) {
        Proto.TransactionV2.Header.Raw updatedRawData = Proto.TransactionV2.Header.Raw
                .newBuilder(getHeader().getRawData())
                .setTimestamp(TimeUtils.time()).build();
        byte[] signature = wallet.sign(HashUtil.sha3(updatedRawData.toByteArray()));
        this.transaction = Proto.TransactionV2.newBuilder(transaction)
                .setHeader(
                        Proto.TransactionV2.Header.newBuilder()
                                .setRawData(updatedRawData)
                                .setSignature(ByteString.copyFrom(signature))
                                .build())
                .build();
    }

    private Proto.TransactionV2.Header getHeader() {
        return this.transaction.getHeader();
    }

    public Sha3Hash getHash() {
        return new Sha3Hash(getHeader().toByteArray());
    }

    public String getBody() {
        return this.transaction.getBody();
    }

    @Override
    public byte[] getData() {
        return this.transaction.toByteArray();
    }

    @Override
    public Proto.TransactionV2 getInstance() {
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

    public boolean isSigned() {
        return !getHeader().getSignature().isEmpty();
    }

    public boolean verify() throws SignatureException {
        if (!isSigned()) {
            return false;
        }

        byte[] hashedRawData = new Sha3Hash(getHeader().getRawData().toByteArray()).getBytes();
        byte[] signatureBin = getHeader().getSignature().toByteArray();

        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(signatureBin);
        ECKey key = ECKey.signatureToKey(hashedRawData, signatureBin);
        return key.verify(hashedRawData, ecdsaSignature);
    }
}
