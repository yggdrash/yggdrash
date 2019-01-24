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

import io.yggdrash.core.blockchain.Block;
import io.yggdrash.proto.EbftProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PbftBlock {
    private static final Logger log = LoggerFactory.getLogger(PbftBlock.class);

    private static final boolean TEST_NO_VERIFY = false; // todo: delete when testing is finished
    private static final int BLOCK_HEADER_LENGTH = 124;
    private static final int SIGNATURE_LENGTH = 65;
    private static final int MAX_VALIDATOR_COUNT = 100;

    private Block block;


    public PbftBlock(byte[] blockBytes) {
    }

    public PbftBlock(long index, byte[] prevBlockHash, Block block) {
        this(index, prevBlockHash, block, null);
    }

    public PbftBlock(long index, byte[] prevBlockHash, Block block, List<String> consensusList) {

    }

    public PbftBlock(EbftProto.BlockCon blockCon) {

    }

    public byte[] toBinary() {
        return null;
    }

    public byte[] getHash() {
        return this.block.getHash();
    }

    public byte[] getPrevBlockHash() {
        return this.block.getHeader().getPrevBlockHash();
    }


}