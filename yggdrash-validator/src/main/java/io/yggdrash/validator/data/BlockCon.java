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
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.proto.EbftProto;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class BlockCon {
    private static final boolean TEST_NO_VERIFY = false; // todo: delete when testing is finished
    private static final int BLOCK_HEADER_LENGTH = 124;
    private static final int SIGNATURE_LENGTH = 65;

    private byte[] hash;
    private byte[] chain;
    private long index;
    private byte[] prevBlockHash;
    private Block block;
    private final List<String> consensusList = new ArrayList<>();

    public BlockCon(byte[] blockConBytes) {
        int position = 0;

        byte[] headerBytes = new byte[BLOCK_HEADER_LENGTH];
        System.arraycopy(blockConBytes, 0, headerBytes, 0, headerBytes.length);
        position += headerBytes.length;
        BlockHeader blockHeader = new BlockHeader(headerBytes);

        byte[] signature = new byte[SIGNATURE_LENGTH];
        System.arraycopy(blockConBytes, position, signature, 0, signature.length);
        position += signature.length;

        byte[] bodyBytes = new byte[(int)blockHeader.getBodyLength()];
        System.arraycopy(blockConBytes, position, bodyBytes, 0, bodyBytes.length);
        position += bodyBytes.length;

        if ((blockConBytes.length - position) % SIGNATURE_LENGTH != 0) {
            throw new NotValidateException();
        }

        byte[] consensus = new byte[SIGNATURE_LENGTH];
        while (position < blockConBytes.length) {
            System.arraycopy(blockConBytes, position, consensus, 0, consensus.length);
            position += consensus.length;
            this.consensusList.add(Hex.toHexString(consensus));
        }

        this.block = new Block(blockHeader, signature, new BlockBody(bodyBytes));
        this.hash = this.block.getHash();
        this.chain = this.block.getHeader().getChain();
        this.index = this.block.getHeader().getIndex();
        this.prevBlockHash = this.block.getHeader().getPrevBlockHash();
    }

    public BlockCon(long index, byte[] prevBlockHash, Block block) {
        this(index, prevBlockHash, block, null);
    }

    public BlockCon(long index, byte[] prevBlockHash, Block block, List<String> consensusList) {
        this.chain = block.getHeader().getChain();
        this.index = index;
        this.prevBlockHash = prevBlockHash;
        this.block = block;
        this.hash = block.getHash();
        if (consensusList != null) {
            this.consensusList.addAll(consensusList);
        }
    }

    public BlockCon(EbftProto.BlockCon blockCon) {
        this.block = Block.toBlock(blockCon.getBlock());
        this.chain = blockCon.getChain().toByteArray();
        this.index = blockCon.getIndex();
        this.prevBlockHash = blockCon.getPrevBlockHash().toByteArray();
        this.hash = blockCon.getHash().toByteArray();
        if (blockCon.getConsensusList().getConsensusListList() != null) {
            for (String consensus : blockCon.getConsensusList().getConsensusListList()) {
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

    public static boolean verify(BlockCon blockCon) {
        if (TEST_NO_VERIFY) {
            return true;
        }

        if (blockCon == null) {
            return false;
        }

        return Arrays.equals(blockCon.getHash(), blockCon.getBlock().getHash())
                && blockCon.getIndex() == blockCon.getBlock().getHeader().getIndex()
                && Arrays.equals(blockCon.getPrevBlockHash(),
                blockCon.getBlock().getHeader().getPrevBlockHash())
                && blockCon.getBlock().verify();
    }

    public BlockCon clone() {
        return new BlockCon(this.index, this.prevBlockHash, this.block, this.consensusList);
    }

    public byte[] toBinary() {
        int pos = 0;
        byte[] consensusList = new byte[SIGNATURE_LENGTH * this.consensusList.size()];
        for (String consensus : this.consensusList) {
            System.arraycopy(Hex.decode(consensus), 0, consensusList, pos, SIGNATURE_LENGTH);
            pos += SIGNATURE_LENGTH;
        }

        return ByteUtil.merge(this.block.toBinary(), consensusList);
    }

    public static EbftProto.BlockCon toProto(BlockCon blockCon) {
        blockCon.getConsensusList().removeAll(Collections.singleton(null));
        EbftProto.BlockCon.Builder protoBlockCon = EbftProto.BlockCon.newBuilder()
                .setChain(ByteString.copyFrom(blockCon.getChain()))
                .setHash(ByteString.copyFrom(blockCon.getHash()))
                .setIndex(blockCon.getIndex())
                .setPrevBlockHash(ByteString.copyFrom(blockCon.getPrevBlockHash()))
                .setBlock(blockCon.getBlock().toProtoBlock())
                .setConsensusList(EbftProto.ConsensusList.newBuilder()
                        .addAllConsensusList(blockCon.getConsensusList()).build());
        return protoBlockCon.build();
    }

    public static EbftProto.BlockConList toProtoList(Collection<BlockCon> collection) {
        EbftProto.BlockConList.Builder builder = EbftProto.BlockConList.newBuilder();

        for (BlockCon blockCon : collection) {
            builder.addBlockConList(BlockCon.toProto(blockCon));
        }

        return builder.build();
    }

    public boolean equals(BlockCon blockCon) {
        return this.index == blockCon.getIndex()
                && Arrays.equals(this.chain, blockCon.getChain())
                && Arrays.equals(this.hash, blockCon.getHash())
                && Arrays.equals(this.prevBlockHash, blockCon.getPrevBlockHash())
                && this.block.equals(blockCon.getBlock())
                && Arrays.equals(this.consensusList.toArray(), blockCon.consensusList.toArray());
    }
}