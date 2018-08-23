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

package io.yggdrash.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.crypto.ECKey;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.proto.Proto;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SignatureException;
import java.util.Objects;

public class TransactionHusk implements ProtoHusk<Proto.Transaction>, Comparable<TransactionHusk> {

    private static final Logger log = LoggerFactory.getLogger(TransactionHusk.class);

    private Proto.Transaction transaction;

    public TransactionHusk(Proto.Transaction transaction) {
        this.transaction = transaction;
    }

    public TransactionHusk(byte[] data) throws InvalidProtocolBufferException {
        this.transaction = Proto.Transaction.parseFrom(data);
    }

    public TransactionHusk(JsonObject jsonObject) {
        String body = jsonObject.toString();
        Proto.Transaction.Header transactionHeader = Proto.Transaction.Header.newBuilder()
                .setRawData(Proto.Transaction.Header.Raw.newBuilder()
                        .setType(ByteString.copyFrom(ByteBuffer.allocate(4).putInt(1).array()))
                        .setVersion(ByteString.copyFrom(ByteBuffer.allocate(4).putInt(1).array()))
                        .setDataHash(ByteString.copyFrom(HashUtil.sha3(body.getBytes())))
                        .setDataSize(body.getBytes().length)
                        .build())
                .build();
        this.transaction = Proto.Transaction.newBuilder()
                .setHeader(transactionHeader)
                .setBody(body)
                .build();
    }

    public TransactionHusk sign(Wallet wallet) {
        Proto.Transaction.Header.Raw updatedRawData = Proto.Transaction.Header.Raw
                .newBuilder(getHeader().getRawData())
                .setTimestamp(TimeUtils.time()).build();

        this.transaction = Proto.Transaction.newBuilder(transaction)
                .setHeader(
                        Proto.Transaction.Header.newBuilder()
                                .setRawData(updatedRawData)
                                .build())
                .build();

        byte[] dataForSign = getDataForSigning();

        byte[] signature = wallet.sign(dataForSign);

        this.transaction = Proto.Transaction.newBuilder(transaction)
                .setHeader(
                        Proto.Transaction.Header.newBuilder()
                                .setRawData(updatedRawData)
                                .setSignature(ByteString.copyFrom(signature))
                                .build())
                .build();
        return this;
    }

    private Proto.Transaction.Header getHeader() {
        return this.transaction.getHeader();
    }

    public Sha3Hash getHash() {
        ByteArrayOutputStream transaction = new ByteArrayOutputStream();

        try {
            transaction.write(getHeader().getRawData().getType().toByteArray());
            transaction.write(getHeader().getRawData().getVersion().toByteArray());
            transaction.write(getHeader().getRawData().getDataHash().toByteArray());
            transaction.write(ByteUtil.longToBytes(getHeader().getRawData().getTimestamp()));
            transaction.write(ByteUtil.longToBytes(getHeader().getRawData().getDataSize()));
            transaction.write(getHeader().getSignature().toByteArray());
        } catch (IOException e) {
            throw new NotValidateException(e);
        }

        return new Sha3Hash(transaction.toByteArray());
    }

    public String getBody() {
        return this.transaction.getBody();
    }

    @Override
    public byte[] getData() {
        return this.transaction.toByteArray();
    }

    @Override
    public Proto.Transaction getInstance() {
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

    public boolean verify() {
        try {
            if (!isSigned()) {
                return false;
            }

            byte[] dataHashForSigning = getDataHashForSigning();
            byte[] signatureBin = getHeader().getSignature().toByteArray();

            ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(signatureBin);
            ECKey key = ECKey.signatureToKey(dataHashForSigning, ecdsaSignature);

            log.debug("verfiy data= " + Hex.toHexString(getDataForSigning()));
            log.debug("verfiy dataHash= " + Hex.toHexString(dataHashForSigning));
            log.debug("verfiy sig= " + Hex.toHexString(ecdsaSignature.toBinary()));
            log.debug("verfiy keyAddress= " + Hex.toHexString(key.getAddress()));

            return key.verify(dataHashForSigning, ecdsaSignature);
        } catch (SignatureException e) {
            log.debug("verfiy() SignatureException");
            throw new InvalidSignatureException(e);
        }
    }

    /**
     * Get the address.
     *
     * @return address
     */
    public Address getAddress() {
        return new Address(ecKey().getAddress());
    }

    /**
     * Get ECKey(include pubKey) using sig & signData.
     *
     * @return ECKey(include pubKey)
     */
    private ECKey ecKey() {
        try {
            byte[] hashedRawData = HashUtil.sha3(getDataForSigning());
            byte[] signatureBin = getHeader().getSignature().toByteArray();
            ECKey.ECDSASignature sig = new ECKey.ECDSASignature(signatureBin);

            return ECKey.signatureToKey(hashedRawData, sig);
        } catch (SignatureException e) {
            throw new InvalidSignatureException(e);
        }
    }

    /**
     * Convert from TransactionHusk.class to JSON string.
     * @return transaction as JsonObject
     */
    public JsonObject toJsonObject() {
        //todo: change to serialize method

        Proto.Transaction.Header.Raw raw = this.transaction.getHeader().getRawData();
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("type", Hex.toHexString(raw.getType().toByteArray()));
        jsonObject.addProperty("version", Hex.toHexString(raw.getVersion().toByteArray()));
        jsonObject.addProperty("dataHash",
                Hex.toHexString(raw.getDataHash().toByteArray()));
        jsonObject.addProperty("timestamp",
                Hex.toHexString(ByteUtil.longToBytes(raw.getTimestamp())));
        jsonObject.addProperty("dataSize",
                Hex.toHexString(ByteUtil.longToBytes(raw.getDataSize())));
        jsonObject.addProperty("signature",
                Hex.toHexString(this.transaction.getHeader().getSignature().toByteArray()));

        jsonObject.add("data", new Gson().fromJson(this.getBody(), JsonObject.class));

        return jsonObject;
    }

    @Override
    public int compareTo(TransactionHusk o) {
        return Long.compare(transaction.getHeader().getRawData().getTimestamp(),
                o.getInstance().getHeader().getRawData().getTimestamp());
    }

    /**
     * Get the data for signing.
     *
     * @return data of sign data
     */
    public byte[] getDataForSigning() {
        ByteArrayOutputStream transaction = new ByteArrayOutputStream();

        try {
            transaction.write(getHeader().getRawData().getType().toByteArray());
            transaction.write(getHeader().getRawData().getVersion().toByteArray());
            transaction.write(getHeader().getRawData().getDataHash().toByteArray());
            transaction.write(ByteUtil.longToBytes(getHeader().getRawData().getTimestamp()));
            transaction.write(ByteUtil.longToBytes(getHeader().getRawData().getDataSize()));
        } catch (IOException e) {
            throw new NotValidateException(e);
        }

        return transaction.toByteArray();
    }

    /**
     * Get the dataHash(sha3) for signing.
     *
     * @return dataHash(sha3, 32Bytes) of sign data
     */
    public byte[] getDataHashForSigning() {
        return HashUtil.sha3(getDataForSigning());
    }
}
