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
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;
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

    public static BlockDto createBy(ConsensusBlock block) {
        return createBy(block, block.getBody().getCount() < MAX_TX_BODY);
    }

    private static BlockDto createBy(ConsensusBlock block, boolean withBody) {
        BlockDto blockDto = new BlockDto();
        BlockHeader header = block.getBlock().getHeader();
        blockDto.branchId = block.getBranchId().toString();
        blockDto.version = Hex.toHexString(header.getVersion());
        blockDto.type = Hex.toHexString(header.getType());
        blockDto.prevBlockId = block.getPrevBlockHash().toString();
        blockDto.index = block.getIndex();
        blockDto.timestamp = Timestamps.toString(Timestamps.fromMillis(header.getTimestamp()));
        blockDto.merkleRoot = Hex.toHexString(header.getMerkleRoot());
        blockDto.bodyLength = header.getBodyLength();
        blockDto.signature = Hex.toHexString(block.getSignature());
        blockDto.txSize = block.getBody().getCount();
        if (withBody) {
            List<Transaction> txList = block.getBody().getTransactionList();
            blockDto.body = txList.stream().map(TransactionDto::createBy).collect(Collectors.toList());
        }
        blockDto.author = block.getBlock().getAddress().toString();
        blockDto.blockId = block.getHash().toString();
        return blockDto;
    }

}
