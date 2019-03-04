/*
 * Copyright 2019 Akashic Foundation
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

package io.yggdrash.validator.data.ebft;

import io.yggdrash.common.config.Constants;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.proto.EbftProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class EbftBlock {
    private static final Logger log = LoggerFactory.getLogger(EbftBlock.class);

    private static final int BLOCK_HEADER_LENGTH = 124;
    private static final int SIGNATURE_LENGTH = 65;
    private static final int MAX_VALIDATOR_COUNT = 100;

    private Block block;
    private final List<String> consensusList = new ArrayList<>();

    public EbftBlock(byte[] bytes) {
        int position = 0;

        byte[] headerBytes = new byte[BLOCK_HEADER_LENGTH];
        System.arraycopy(bytes, 0, headerBytes, 0, headerBytes.length);
        position += headerBytes.length;
        BlockHeader blockHeader = new BlockHeader(headerBytes);

        byte[] signature = new byte[SIGNATURE_LENGTH];
        System.arraycopy(bytes, position, signature, 0, signature.length);
        position += signature.length;

        int blockBodyLength = (int) blockHeader.getBodyLength();
        if (blockBodyLength < 0 || blockBodyLength > Constants.MAX_MEMORY) {
            log.debug("EbftBlock body length is not valid");
            throw new NotValidateException();
        }

        byte[] bodyBytes = new byte[blockBodyLength];
        System.arraycopy(bytes, position, bodyBytes, 0, bodyBytes.length);
        position += bodyBytes.length;

        if ((bytes.length - position) % SIGNATURE_LENGTH != 0) {
            throw new NotValidateException();
        }

        byte[] consensus = new byte[SIGNATURE_LENGTH];
        while (position < bytes.length) {
            System.arraycopy(bytes, position, consensus, 0, consensus.length);
            position += consensus.length;
            this.consensusList.add(Hex.toHexString(consensus));
        }

        this.block = new Block(blockHeader, signature, new BlockBody(bodyBytes));
    }

    public EbftBlock(Block block) {
        this(block, null);
    }

    public EbftBlock(Block block, List<String> consensusList) {
        this.block = block;
        if (consensusList != null) {
            this.consensusList.addAll(consensusList);
        }
    }

    public EbftBlock(EbftProto.EbftBlock block) {
        this.block = Block.toBlock(block.getBlock());
        if (block.getConsensusList().getConsensusListList() != null) {
            for (String consensus : block.getConsensusList().getConsensusListList()) {
                if (consensus != null) {
                    this.consensusList.add(consensus);
                }
            }
        }
    }

    public byte[] getChain() {
        return this.block.getChain();
    }

    public long getIndex() {
        return this.block.getIndex();
    }

    public byte[] getHash() {
        return this.block.getHash();
    }

    public String getHashHex() {
        return this.block.getHashHex();
    }

    public byte[] getPrevBlockHash() {
        return this.block.getPrevBlockHash();
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public List<String> getConsensusList() {
        return consensusList;
    }

    public static boolean verify(EbftBlock ebftBlock) {
        if (ebftBlock == null) {
            return false;
        }

        // todo: check consensuses whether validator's signatures or not

        return ebftBlock.getBlock().verify();
    }

    public byte[] toBinary() {
        int pos = 0;

        int consensusListSize = this.consensusList.size();
        if (consensusListSize > MAX_VALIDATOR_COUNT) {
            throw new NotValidateException();
        }

        byte[] consensusList = new byte[SIGNATURE_LENGTH * consensusListSize];
        for (String consensus : this.consensusList) {
            System.arraycopy(Hex.decode(consensus), 0, consensusList, pos, SIGNATURE_LENGTH);
            pos += SIGNATURE_LENGTH;
        }

        return ByteUtil.merge(this.block.toBinary(), consensusList);
    }

    public static EbftProto.EbftBlock toProto(EbftBlock ebftBlock) {
        EbftProto.EbftBlock.Builder protoBlock = EbftProto.EbftBlock.newBuilder()
                .setBlock(ebftBlock.getBlock().toProtoBlock())
                .setConsensusList(EbftProto.ConsensusList.newBuilder()
                        .addAllConsensusList(ebftBlock.getConsensusList()).build());
        return protoBlock.build();
    }

    public static EbftProto.EbftBlockList toProtoList(Collection<EbftBlock> collection) {
        EbftProto.EbftBlockList.Builder builder = EbftProto.EbftBlockList.newBuilder();

        for (EbftBlock ebftBlock : collection) {
            builder.addEbftBlockList(EbftBlock.toProto(ebftBlock));
        }

        return builder.build();
    }

    public boolean equals(EbftBlock ebftBlock) {
        return this.block.equals(ebftBlock.getBlock())
                && Arrays.equals(this.consensusList.toArray(), ebftBlock.consensusList.toArray());
    }

    public void clear() {
        this.block.clear();
        this.consensusList.clear();
    }

    public EbftBlock clone() {
        return new EbftBlock(this.block.clone(), this.consensusList);
    }
}