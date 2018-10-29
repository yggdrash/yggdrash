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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.protobuf.ByteString;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.proto.Proto;
import org.spongycastle.util.encoders.Hex;

public class TransactionDto {

    private byte[] chain;
    private byte[] version;
    private byte[] type;
    private long timestamp;
    private byte[] bodyHash;
    private long bodyLength;
    private byte[] signature;
    private String body;
    private String author;
    private String hash;

    @JsonProperty("chain")
    public String getChainHex() {
        return Hex.toHexString(chain);
    }

    public void setChain(byte[] chain) {
        this.chain = chain;
    }

    public void setChainHex(String chain) {
        this.chain = Hex.decode(chain);
    }

    @JsonProperty("version")
    public String getVersionHex() {
        return Hex.toHexString(version);
    }

    public void setVersion(byte[] version) {
        this.version = version;
    }

    public void setVersionHex(String version) {
        this.version = Hex.decode(version);
    }

    @JsonProperty("type")
    public String getTypeHex() {
        return Hex.toHexString(type);
    }

    public void setType(byte[] type) {
        this.type = type;
    }

    public void setTypeHex(String type) {
        this.type = Hex.decode(type);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @JsonProperty("bodyHash")
    public String getBodyHashHex() {
        return Hex.toHexString(bodyHash);
    }

    public void setBodyHash(byte[] bodyHash) {
        this.bodyHash = bodyHash;
    }

    public void setBodyHashHex(String bodyHash) {
        this.bodyHash = Hex.decode(bodyHash);
    }

    public long getBodyLength() {
        return bodyLength;
    }

    public void setBodyLength(long bodyLength) {
        this.bodyLength = bodyLength;
    }

    @JsonProperty("signature")
    public String getSignatureHex() {
        return Hex.toHexString(signature);
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public void setSignatureHex(String signature) {
        this.signature = Hex.decode(signature);
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public static TransactionHusk of(TransactionDto dto) {
        Proto.Transaction.Header header = Proto.Transaction.Header.newBuilder()
                .setChain(ByteString.copyFrom(Hex.decode(dto.getChainHex())))
                .setVersion(ByteString.copyFrom(Hex.decode(dto.getVersionHex())))
                .setType(ByteString.copyFrom(Hex.decode(dto.getTypeHex())))
                .setTimestamp(ByteString.copyFrom(ByteUtil.longToBytes(dto.getTimestamp())))
                .setBodyHash(ByteString.copyFrom(Hex.decode(dto.getBodyHashHex())))
                .setBodyLength(ByteString.copyFrom(ByteUtil.longToBytes(dto.getBodyLength())))
                .build();

        Proto.Transaction tx = Proto.Transaction.newBuilder()
                .setHeader(header)
                .setSignature(ByteString.copyFrom(Hex.decode(dto.getSignatureHex())))
                .setBody(ByteString.copyFromUtf8(dto.getBody()))
                .build();
        return new TransactionHusk(tx);
    }

    public static TransactionDto createBy(TransactionHusk tx) {
        TransactionDto transactionDto = new TransactionDto();
        Proto.Transaction.Header header = tx.getInstance().getHeader();

        transactionDto.setChain(header.getChain().toByteArray());
        transactionDto.setVersion(header.getVersion().toByteArray());
        transactionDto.setType(header.getType().toByteArray());
        transactionDto.setTimestamp(
                ByteUtil.byteArrayToLong(header.getTimestamp().toByteArray()));
        transactionDto.setBodyHash(header.getBodyHash().toByteArray());
        transactionDto.setBodyLength(
                ByteUtil.byteArrayToLong(header.getBodyLength().toByteArray()));
        transactionDto.setSignature(tx.getInstance().getSignature().toByteArray());
        transactionDto.setBody(tx.getBody());
        transactionDto.setAuthor(tx.getAddress().toString());
        transactionDto.setHash(tx.getHash().toString());
        return transactionDto;
    }
}
