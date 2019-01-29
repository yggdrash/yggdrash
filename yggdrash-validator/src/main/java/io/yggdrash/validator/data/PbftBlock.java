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

import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.Proto;
import io.yggdrash.validator.data.pbft.PbftMessageSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PbftBlock {
    private static final Logger log = LoggerFactory.getLogger(PbftBlock.class);

    private final Block block;
    private final PbftMessageSet pbftMessageSet;

    public PbftBlock(Block block, PbftMessageSet pbftMessageSet) {
        this.block = block;
        this.pbftMessageSet = pbftMessageSet;
    }

    public PbftBlock(byte[] bytes) {
        this.block = new Block(ByteUtil.parseBytes(bytes, 0, (int) Block.getBlockLengthInBytes(bytes)));
        this.pbftMessageSet = null;
    }

    public PbftBlock(PbftProto.PbftBlock protoBlock) {
        this.block = Block.toBlock(protoBlock.getBlock());
        this.pbftMessageSet = new PbftMessageSet(protoBlock.getPbftMessageSet());
    }

    public byte[] toBinary() {
        if (this.pbftMessageSet != null) {
            return ByteUtil.merge(this.block.toBinary(), this.pbftMessageSet.toBinary());
        } else {
            return this.block.toBinary();
        }
    }

    public long getIndex() {
        return this.block.getIndex();
    }

    public byte[] getHash() {
        return this.block.getHash();
    }

    public byte[] getPrevBlockHash() {
        return this.block.getHeader().getPrevBlockHash();
    }

    public Block getBlock() {
        return block;
    }

    public PbftMessageSet getPbftMessageSet() {
        return pbftMessageSet;
    }

    public static boolean verify(PbftBlock block) {
        if (block == null) {
            return false;
        }

        //todo : check 2f + 1 message count when ?
        if (block.getBlock().verify()
                && block.getPbftMessageSet().verify(block.getPbftMessageSet(), block.getBlock())) {
            return true;
        }

        return false;
    }

    public static PbftProto.PbftBlock toProto(PbftBlock pbftBlock) {
        Proto.Block protoBlock = pbftBlock.getBlock().toProtoBlock();
        PbftProto.PbftMessageSet protoPbftMessageSet =
                PbftMessageSet.toProto(pbftBlock.getPbftMessageSet());

        PbftProto.PbftBlock.Builder protoPbftBlockBuilder = PbftProto.PbftBlock.newBuilder();
        if (protoBlock != null) {
            protoPbftBlockBuilder.setBlock(protoBlock);
        }
        if (protoPbftMessageSet != null) {
            protoPbftBlockBuilder.setPbftMessageSet(protoPbftMessageSet);
        }

        return protoPbftBlockBuilder.build();
    }


}