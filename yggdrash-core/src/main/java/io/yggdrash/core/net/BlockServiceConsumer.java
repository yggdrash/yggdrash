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

import io.yggdrash.common.config.Constants.Limit;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.consensus.ConsensusBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BlockServiceConsumer<T> implements BlockConsumer<T> {
    private static final Logger log = LoggerFactory.getLogger(BlockServiceConsumer.class);
    private final BranchGroup branchGroup;
    private CatchUpSyncEventListener listener;

    public BlockServiceConsumer(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @Override
    public void setListener(CatchUpSyncEventListener listener) {
        this.listener = listener;
    }

    @Override
    public List<ConsensusBlock<T>> syncBlock(BranchId branchId, long offset, long limit) {
        long curBestBlock = branchGroup.getLastIndex(branchId);
        List<ConsensusBlock<T>> blockList = new ArrayList<>();
        if (curBestBlock == 0) {
            return blockList;
        }
        if (isNeedBlockSync(curBestBlock, offset)) {
            // Catchup Event!
            if (listener != null) {
                listener.catchUpRequest(branchId, offset);
            }
        } else {
            updateBlockList(branchId, offset, limit, blockList);
        }
        return blockList;
    }

    //TODO check syncronization about addBlock()
    @Override
    public void broadcastBlock(ConsensusBlock<T> block) {
        try {
            BranchId branchId = block.getBranchId();
            long nextIndex = branchGroup.getLastIndex(branchId) + 1;
            long receivedIndex = block.getIndex();

            if (receivedIndex == nextIndex) {
                branchGroup.addBlock(block, true);
            } else {
                log.trace("Received blockIndex({}) is not nextBlockIndex({}).",
                        block.getIndex(), nextIndex);
            }
        } catch (Exception e) {
            log.debug("BroadcastBlock() is failed. {}", e.getMessage());
        }
    }

    private boolean isNeedBlockSync(long curIndex, long reqIndex) {
        // TODO limit maxDiff
        long maxDiffBetweenCurrentAndReceivedBlockHeight = 10000;
        if (curIndex + 1 < reqIndex) {
            long diff = reqIndex - curIndex;
            // TODO 'from' is a bad peer if 'diff' is greater then 10000. Add 'from' peer to the blackList.
            return diff < maxDiffBetweenCurrentAndReceivedBlockHeight;
        }
        return false;
    }

    private void updateBlockList(BranchId branchId, long offset, long limit, List<ConsensusBlock<T>> blockList) {
        try {
            if (offset < 0) {
                offset = 0;
            }

            long bodyLengthSum = 0;

            for (int i = 0; i < limit; i++) {
                ConsensusBlock block = branchGroup.getBlockByIndex(branchId, offset++);
                if (block == null) {
                    return;
                }
                bodyLengthSum += block.getSerializedSize();
                if (bodyLengthSum > Limit.BLOCK_SYNC_SIZE) {
                    return;
                }
                blockList.add(block);
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }

}
