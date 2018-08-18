/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.controller;

import com.google.protobuf.ByteString;
import io.yggdrash.core.husk.TransactionHusk;
import io.yggdrash.proto.Proto;

public class TransactionDto {

    private byte[] type;
    private byte[] version;
    private byte[] dataHash;
    private long dataSize;
    private long timestamp;
    private byte[] signature;
    private String data;
    private String txHash;

    public static TransactionHusk of(TransactionDto dto) {
        Proto.Transaction.Header header = Proto.Transaction.Header.newBuilder()
                .setRawData(Proto.Transaction.Header.Raw.newBuilder()
                        .setType(ByteString.copyFrom(dto.getType()))
                        .setVersion(ByteString.copyFrom(dto.getType()))
                        .setDataHash(ByteString.copyFrom(dto.getDataHash()))
                        .setDataSize(dto.getDataSize())
                        .setTimestamp(dto.getTimestamp())
                        .build())
                .setSignature(ByteString.copyFrom(dto.getSignature()))
                .build();
        Proto.Transaction tx = Proto.Transaction.newBuilder()
                .setHeader(header)
                .setBody(dto.getData())
                .build();
        return new TransactionHusk(tx);
    }

    public static TransactionDto createBy(TransactionHusk tx) {
        TransactionDto transactionDto = new TransactionDto();
        Proto.Transaction.Header.Raw raw = tx.getInstance().getHeader().getRawData();
        transactionDto.setType(raw.getType().toByteArray());
        transactionDto.setVersion(raw.getVersion().toByteArray());
        transactionDto.setDataHash(raw.getDataHash().toByteArray());
        transactionDto.setDataSize(raw.getDataSize());
        transactionDto.setTimestamp(raw.getTimestamp());
        transactionDto.setSignature(tx.getInstance().getHeader().getSignature().toByteArray());
        transactionDto.setData(tx.getBody());
        transactionDto.setTxHash(tx.getHash().toString());
        return transactionDto;
    }

    public byte[] getType() {
        return type;
    }

    public void setType(byte[] type) {
        this.type = type;
    }

    public byte[] getVersion() {
        return version;
    }

    public void setVersion(byte[] version) {
        this.version = version;
    }

    public byte[] getDataHash() {
        return dataHash;
    }

    public void setDataHash(byte[] dataHash) {
        this.dataHash = dataHash;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }
}
