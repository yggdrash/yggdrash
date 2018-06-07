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
import io.yggdrash.node.BlockBuilder;

import java.util.Arrays;

public class BlockBuilderMock implements BlockBuilder {
    @Override
    public Block build(String data) {
        Account account = new Account();
        BlockBody blockBody = new BlockBody(Arrays.asList());
        BlockHeader blockHeader = new BlockHeader.Builder()
                .account(account)
                .prevBlock(null)
                .blockBody(blockBody).build();
        return new Block(blockHeader, blockBody);
    }
}
