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
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.proto.EbftProto;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class BlockCon {
    private static final boolean TEST_NO_VERIFY = false;

    private byte[] id;
    private byte[] chain;
    private long index;
    private byte[] parentId;
    private BlockHusk block;
    private final List<String> consensusList = new ArrayList<>();

    public BlockCon(long index, byte[] parentId, BlockHusk block) {
        this(index, parentId, block, null);
    }

    public BlockCon(long index, byte[] parentId, BlockHusk block, List<String> consensusList) {
        this.chain = block.getBranchId().getBytes();
        this.index = index;
        this.parentId = parentId;
        this.block = block;
        this.id = block.getHash().getBytes();
        if (consensusList != null) {
            this.consensusList.addAll(consensusList);
        }
    }

    public BlockCon(EbftProto.BlockCon blockCon) {
        this.chain = blockCon.getChain().toByteArray();
        this.index = blockCon.getIndex();
        this.parentId = blockCon.getParentId().toByteArray();
        this.block = new BlockHusk(blockCon.getBlock());
        this.id = block.getHash().getBytes();
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

    public BlockHusk getBlock() {
        return block;
    }

    public void setBlock(BlockHusk block) {
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

        if (Arrays.equals(blockCon.getId(), blockCon.getBlock().getHash().getBytes())
                && blockCon.getIndex() == blockCon.getBlock().getIndex()
                && Arrays.equals(blockCon.getParentId(),
                        blockCon.getBlock().getPrevHash().getBytes())
                && blockCon.getBlock().verify()) {
            return true;
        }

        return false;
    }

    public BlockCon clone() {
        return new BlockCon(this.index, this.parentId, this.block, this.consensusList);
    }

    public static EbftProto.BlockCon toProto(BlockCon blockCon) {
        blockCon.getConsensusList().removeAll(Collections.singleton(null));
        EbftProto.BlockCon.Builder protoBlockCon = EbftProto.BlockCon.newBuilder()
                .setChain(ByteString.copyFrom(blockCon.getChain()))
                .setId(ByteString.copyFrom(blockCon.getId()))
                .setIndex(blockCon.getIndex())
                .setParentId(ByteString.copyFrom(blockCon.getParentId()))
                .setBlock(blockCon.getBlock().getInstance())
                .setConsensusList(EbftProto.ConsensusList.newBuilder()
                        .addAllConsensusList(blockCon.getConsensusList()).build());
        return protoBlockCon.build();
    }

    public static EbftProto.BlockConList toProtoList(Collection<BlockCon> collection) {
        EbftProto.BlockConListOrBuilder builder = EbftProto.BlockConList.newBuilder();

        for (BlockCon blockCon : collection) {
            ((EbftProto.BlockConList.Builder) builder).addBlockConList(BlockCon.toProto(blockCon));
        }

        return ((EbftProto.BlockConList.Builder) builder).build();
    }

    public boolean equals(BlockCon blockCon) {
        if (this.index == blockCon.getIndex()
                && Arrays.equals(this.chain, blockCon.getChain())
                && Arrays.equals(this.id, blockCon.getId())
                && Arrays.equals(this.parentId, blockCon.getParentId())
                && Arrays.equals(this.block.getHash().getBytes(),
                blockCon.getBlock().getHash().getBytes())) {
            return true;
        } else {
            return false;
        }
    }
}