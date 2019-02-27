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

package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BlockChainServiceConsumer implements BlockChainConsumer {
    private static final Logger log = LoggerFactory.getLogger(BlockChainServiceConsumer.class);
    private static final long BLOCK_SIZE_LIMIT = 3 * 1024 * 1024; // 3MB
    private final BranchGroup branchGroup;
    private CatchUpSyncEventListener listener;

    public BlockChainServiceConsumer(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @Override
    public void setListener(CatchUpSyncEventListener listener) {
        this.listener = listener;
    }

    @Override
    public List<BlockHusk> syncBlock(BranchId branchId, long offset, long limit) {
        BlockChain blockChain = branchGroup.getBranch(branchId);
        List<BlockHusk> blockHuskList = new ArrayList<>();
        if (blockChain == null) {
            log.warn("Invalid syncBlock request for branchId={}", branchId);
            return blockHuskList;
        }
        if (offset < 0) {
            offset = 0;
        }

        long bodyLengthSum = 0;
        for (int i = 0; i < limit; i++) {
            BlockHusk block = branchGroup.getBlockByIndex(branchId, offset++);
            if (block == null) {
                break;
            }
            bodyLengthSum += block.getBodyLength();
            if (bodyLengthSum > BLOCK_SIZE_LIMIT) {
                break;
            }
            blockHuskList.add(block);
        }
        return blockHuskList;
    }

    @Override
    public List<TransactionHusk> syncTransaction(BranchId branchId) {
        return branchGroup.getUnconfirmedTxs(branchId);
    }

    @Override
    public void broadcastBlock(BlockHusk block) {
        try {
            branchGroup.addBlock(block, true);
        } catch (Exception e) {
            log.warn(e.getMessage());
            if (listener != null) {
                listener.catchUpRequest(block);
            }
        }
    }

    @Override
    public void broadcastTransaction(TransactionHusk tx) {
        try {
            branchGroup.addTransaction(tx);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }
}
