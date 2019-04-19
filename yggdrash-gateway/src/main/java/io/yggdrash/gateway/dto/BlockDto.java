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

package io.yggdrash.gateway.dto;

import com.google.protobuf.util.Timestamps;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.consensus.Block;
import io.yggdrash.proto.Proto;
import org.spongycastle.util.encoders.Hex;

import java.util.List;
import java.util.stream.Collectors;

public class BlockDto {
    private static final int MAX_TX_BODY = 100;

    public String branchId;
    public String version;
    public String type;
    public String prevBlockId;
    public long index;
    public String timestamp;
    public String merkleRoot;
    public long bodyLength;
    public long txSize;
    public String signature;
    public List<TransactionDto> body;
    public String author;
    public String blockId;

    public static BlockDto createBy(Block block) {
        return createBy(block, block.getBodyCount() < MAX_TX_BODY);
    }

    private static BlockDto createBy(Block block, boolean withBody) {
        BlockDto blockDto = new BlockDto();
        Proto.Block.Header header = block.getProtoBlock().getHeader();
        blockDto.branchId = Hex.toHexString(header.getChain().toByteArray());
        blockDto.version = Hex.toHexString(header.getVersion().toByteArray());
        blockDto.type = Hex.toHexString(header.getType().toByteArray());
        blockDto.prevBlockId = Hex.toHexString(block.getPrevBlockHash().getBytes());
        blockDto.index = block.getIndex();
        blockDto.timestamp = Timestamps.toString(header.getTimestamp());
        blockDto.merkleRoot = Hex.toHexString(header.getMerkleRoot().toByteArray());
        blockDto.bodyLength = header.getBodyLength();
        blockDto.signature = Hex.toHexString(block.getSignature());
        blockDto.txSize = block.getBodyCount();
        if (withBody) {
            List<TransactionHusk> txList = block.getBody();
            blockDto.body = txList.stream().map(TransactionDto::createBy)
                    .collect(Collectors.toList());
        }
        if (block.getIndex() != 0) {
            // Genesis Block has no author
            blockDto.author = block.getAddress() == null ? null : block.getAddress().toString();
        }
        blockDto.blockId = block.getHash().toString();
        return blockDto;
    }

}
