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

package io.yggdrash.node.controller;

import io.yggdrash.core.BlockHusk;
import io.yggdrash.proto.Proto;

import java.util.List;
import java.util.stream.Collectors;

public class BlockDto {
    private byte[] type;
    private byte[] version;
    private long index;
    private long timestamp;
    private byte[] prevBlockHash;
    private String author;
    private byte[] merkleRoot;
    private long dataSize;
    private byte[] signature;
    private List<TransactionDto> body;
    private String hash;

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

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getPrevBlockHash() {
        return prevBlockHash;
    }

    public void setPrevBlockHash(byte[] prevBlockHash) {
        this.prevBlockHash = prevBlockHash;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public byte[] getMerkleRoot() {
        return merkleRoot;
    }

    public void setMerkleRoot(byte[] merkleRoot) {
        this.merkleRoot = merkleRoot;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public List<TransactionDto> getBody() {
        return body;
    }

    public void setBody(List<TransactionDto> body) {
        this.body = body;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public static BlockDto createBy(BlockHusk block) {
        BlockDto blockDto = new BlockDto();
        Proto.Block.Header.Raw raw = block.getInstance().getHeader().getRawData();
        blockDto.setType(raw.getType().toByteArray());
        blockDto.setVersion(raw.getVersion().toByteArray());
        blockDto.setIndex(block.getIndex());
        blockDto.setTimestamp(raw.getTimestamp());
        blockDto.setPrevBlockHash(block.getPrevHash().getBytes());
        blockDto.setAuthor(block.getAddress().toString());
        blockDto.setMerkleRoot(raw.getMerkleRoot().toByteArray());
        blockDto.setDataSize(raw.getDataSize());
        blockDto.setSignature(block.getInstance().getHeader().getSignature().toByteArray());
        blockDto.setBody(block.getBody().stream().map(TransactionDto::createBy)
                .collect(Collectors.toList()));
        blockDto.setHash(block.getHash().toString());
        return blockDto;
    }

}
