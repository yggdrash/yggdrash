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

package io.yggdrash.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.genesis.BlockInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class BlockChainLoader {
    private final ObjectMapper mapper = new ObjectMapper();
    private InputStream branchInfoStream;

    public BlockChainLoader(InputStream branchInfoStream) {
        this.branchInfoStream = branchInfoStream;
    }

    public BlockChainLoader(File infoFile) {
        try {
            this.branchInfoStream = new FileInputStream(infoFile);
        } catch (FileNotFoundException e) {
            throw new FailedOperationException(e);
        }
    }

    public BlockHusk getGenesis() {
        return convertJsonToBlock();
    }

    public BlockInfo loadBranchInfo() throws IOException {
        return mapper.readValue(branchInfoStream, BlockInfo.class);
    }

    private BlockHusk convertJsonToBlock() {
        //TODO 브랜치 정보 파일 컨버팅
        try {
            BlockInfo blockinfo = loadBranchInfo();

            return convertBlock(blockinfo);
        } catch (Exception e) {
            throw new NotValidateException();
        }
    }

    private BlockHusk convertBlock(BlockInfo blockinfo) {
        return new BlockHusk(Block.fromBlockInfo(blockinfo).toProtoBlock());
    }
}
