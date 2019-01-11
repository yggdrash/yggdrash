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
    private static final boolean TEST_NO_VERIFY = false;
    private static final int BLOCK_HEADER_LENGTH = 124;
    private static final int SIGNATURE_LENGTH = 65;

    private byte[] id; // todo: change name to blockConHash or something
    private byte[] chain;
    private long index;
    private byte[] parentId;
    private Block block;
    private final List<String> consensusList = new ArrayList<>();

    public BlockCon(byte[] blockConBytes) {
        int position = 0;

        byte[] headerBytes = new byte[BLOCK_HEADER_LENGTH];
        System.arraycopy(blockConBytes, 0, headerBytes, 0, headerBytes.length);
        position += headerBytes.length;

        byte[] signature = new byte[SIGNATURE_LENGTH];
        System.arraycopy(blockConBytes, position, signature, 0, signature.length);
        position += signature.length;

        byte[] bodyBytes = new byte[blockConBytes.length - headerBytes.length - signature.length];
        System.arraycopy(blockConBytes, position, bodyBytes, 0, bodyBytes.length);
        position += bodyBytes.length;

        if ((blockConBytes.length - position) % SIGNATURE_LENGTH != 0) {
            throw new NotValidateException();
        }

        byte[] consensus = new byte[SIGNATURE_LENGTH];
        while (position > blockConBytes.length) {
            System.arraycopy(blockConBytes, position, consensus, 0, consensus.length);
            position += consensus.length;
            this.consensusList.add(Hex.toHexString(consensus));
        }

        this.block = new Block(new BlockHeader(headerBytes), signature, new BlockBody(bodyBytes));
        this.id = this.block.getHash();
        this.chain = this.block.getHeader().getChain();
        this.index = this.block.getHeader().getIndex();
        this.parentId = this.block.getHeader().getPrevBlockHash();
    }

    public BlockCon(long index, byte[] parentId, Block block) {
        this(index, parentId, block, null);
    }

    public BlockCon(long index, byte[] parentId, Block block, List<String> consensusList) {
        this.chain = block.getHeader().getChain();
        this.index = index;
        this.parentId = parentId;
        this.block = block;
        this.id = block.getHash();
        if (consensusList != null) {
            this.consensusList.addAll(consensusList);
        }
    }

    public BlockCon(EbftProto.BlockCon blockCon) {
        this.block = Block.toBlock(blockCon.getBlock());
        this.chain = blockCon.getChain().toByteArray();
        this.index = blockCon.getIndex();
        this.parentId = blockCon.getParentId().toByteArray();
        this.id = blockCon.getId().toByteArray();
        if (blockCon.getConsensusList().getConsensusListList() != null) {
            for (String consensus : blockCon.getConsensusList().getConsensusListList()) {
                if (consensus != null) {
                    this.consensusList.add(consensus);
                }
            }
        }
    }

    public byte[] getId() {
        return id;
    }

    public byte[] getChain() {
        return chain;
    }

    public String getIdHex() {
        return Hex.toHexString(id);
    }

    public void setId(byte[] id) {
        this.id = id;
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

    public byte[] getParentId() {
        return parentId;
    }

    private void setParentId(byte[] parentId) {
        this.parentId = parentId;
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

        if (Arrays.equals(blockCon.getId(), blockCon.getBlock().getHash())
                && blockCon.getIndex() == blockCon.getBlock().getHeader().getIndex()
                && Arrays.equals(blockCon.getParentId(),
                        blockCon.getBlock().getHeader().getPrevBlockHash())
                && blockCon.getBlock().verify()) {
            return true;
        }

        return false;
    }

    public BlockCon clone() {
        return new BlockCon(this.index, this.parentId, this.block, this.consensusList);
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
                .setId(ByteString.copyFrom(blockCon.getId()))
                .setIndex(blockCon.getIndex())
                .setParentId(ByteString.copyFrom(blockCon.getParentId()))
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
                && Arrays.equals(this.id, blockCon.getId())
                && Arrays.equals(this.parentId, blockCon.getParentId())
                && this.block.equals(blockCon.getBlock());
    }
}