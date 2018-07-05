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

package io.yggdrash.node.mock;

import io.yggdrash.core.Account;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockBody;
import io.yggdrash.core.BlockHeader;
import io.yggdrash.core.Transaction;
import io.yggdrash.node.BlockBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class BlockBuilderMock implements BlockBuilder {

    @Override
    public Block build(List<Transaction> txList, Block prevBlock) throws IOException {
        Account account = new Account();
        BlockBody blockBody = new BlockBody(txList);
        BlockHeader blockHeader = new BlockHeader.Builder()
                .prevBlock(prevBlock)
                .blockBody(blockBody).build(account);
        return new Block(blockHeader, blockBody);
    }
}
