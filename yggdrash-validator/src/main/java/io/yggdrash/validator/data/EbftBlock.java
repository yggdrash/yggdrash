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

package io.yggdrash.validator.data;

import com.google.protobuf.ByteString;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.util.ByteUtil;
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
import java.util.Collections;
import java.util.List;

public class EbftBlock {
    private static final Logger log = LoggerFactory.getLogger(EbftBlock.class);

    private static final int BLOCK_HEADER_LENGTH = 124;
    private static final int SIGNATURE_LENGTH = 65;
    private static final int MAX_VALIDATOR_COUNT = 100;

    private byte[] hash;
    private byte[] chain;
    private long index;
    private byte[] prevBlockHash;
    private Block block;
    private final List<String> consensusList = new ArrayList<>();

    public EbftBlock(byte[] blockBytes) {
        int position = 0;

        byte[] headerBytes = new byte[BLOCK_HEADER_LENGTH];
        System.arraycopy(blockBytes, 0, headerBytes, 0, headerBytes.length);
        position += headerBytes.length;
        BlockHeader blockHeader = new BlockHeader(headerBytes);

        byte[] signature = new byte[SIGNATURE_LENGTH];
        System.arraycopy(blockBytes, position, signature, 0, signature.length);
        position += signature.length;

        int blockBodyLength = (int) blockHeader.getBodyLength();
        if (blockBodyLength < 0 || blockBodyLength > Constants.MAX_MEMORY) {
            log.debug("EbftBlock body length is not valid");
            throw new NotValidateException();
        }

        byte[] bodyBytes = new byte[blockBodyLength];
        System.arraycopy(blockBytes, position, bodyBytes, 0, bodyBytes.length);
        position += bodyBytes.length;

        if ((blockBytes.length - position) % SIGNATURE_LENGTH != 0) {
            throw new NotValidateException();
        }

        byte[] consensus = new byte[SIGNATURE_LENGTH];
        while (position < blockBytes.length) {
            System.arraycopy(blockBytes, position, consensus, 0, consensus.length);
            position += consensus.length;
            this.consensusList.add(Hex.toHexString(consensus));
        }

        this.block = new Block(blockHeader, signature, new BlockBody(bodyBytes));
        this.hash = this.block.getHash();
        this.chain = this.block.getHeader().getChain();
        this.index = this.block.getHeader().getIndex();
        this.prevBlockHash = this.block.getHeader().getPrevBlockHash();
    }

    public EbftBlock(long index, byte[] prevBlockHash, Block block) {
        this(index, prevBlockHash, block, null);
    }

    public EbftBlock(long index, byte[] prevBlockHash, Block block, List<String> consensusList) {
        this.chain = block.getHeader().getChain();
        this.index = index;
        this.prevBlockHash = prevBlockHash;
        this.block = block;
        this.hash = block.getHash();
        if (consensusList != null) {
            this.consensusList.addAll(consensusList);
        }
    }

    public EbftBlock(EbftProto.EbftBlock block) {
        this.block = Block.toBlock(block.getBlock());
        this.chain = block.getChain().toByteArray();
        this.index = block.getIndex();
        this.prevBlockHash = block.getPrevBlockHash().toByteArray();
        this.hash = block.getHash().toByteArray();
        if (block.getConsensusList().getConsensusListList() != null) {
            for (String consensus : block.getConsensusList().getConsensusListList()) {
                if (consensus != null) {
                    this.consensusList.add(consensus);
                }
            }
        }
    }

    public byte[] getHash() {
        return hash;
    }

    public byte[] getChain() {
        return chain;
    }

    public String getHashHex() {
        return Hex.toHexString(hash);
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public byte[] getPrevBlockHash() {
        return prevBlockHash;
    }

    private void setPrevBlockHash(byte[] prevBlockHash) {
        this.prevBlockHash = prevBlockHash;
    }

    public List<String> getConsensusList() {
        return consensusList;
    }

    public static boolean verify(EbftBlock ebftBlock) {
        if (ebftBlock == null) {
            return false;
        }

        return Arrays.equals(ebftBlock.getHash(), ebftBlock.getBlock().getHash())
                && ebftBlock.getIndex() == ebftBlock.getBlock().getHeader().getIndex()
                && Arrays.equals(ebftBlock.getPrevBlockHash(),
                ebftBlock.getBlock().getHeader().getPrevBlockHash())
                && ebftBlock.getBlock().verify();
    }

    public EbftBlock clone() {
        return new EbftBlock(this.index, this.prevBlockHash, this.block, this.consensusList);
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
        ebftBlock.getConsensusList().removeAll(Collections.singleton(null));
        EbftProto.EbftBlock.Builder protoBlock = EbftProto.EbftBlock.newBuilder()
                .setChain(ByteString.copyFrom(ebftBlock.getChain()))
                .setHash(ByteString.copyFrom(ebftBlock.getHash()))
                .setIndex(ebftBlock.getIndex())
                .setPrevBlockHash(ByteString.copyFrom(ebftBlock.getPrevBlockHash()))
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
        return this.index == ebftBlock.getIndex()
                && Arrays.equals(this.chain, ebftBlock.getChain())
                && Arrays.equals(this.hash, ebftBlock.getHash())
                && Arrays.equals(this.prevBlockHash, ebftBlock.getPrevBlockHash())
                && this.block.equals(ebftBlock.getBlock())
                && Arrays.equals(this.consensusList.toArray(), ebftBlock.consensusList.toArray());
    }
}