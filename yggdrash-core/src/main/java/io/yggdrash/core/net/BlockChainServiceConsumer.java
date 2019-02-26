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
    private final BranchGroup branchGroup;
    private CatchUpSyncEventListener listener;

    public BlockChainServiceConsumer(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    /*
    3 cases of doing syncBlock
    - The first time a new node connects to the network
    - If the rebooting node is connected to the network again
    - If the node received a block with a different block height

    The current block synchronization proceeds recursively.
    That is, it recursively inquires whether the current block height is higher than
    the current block height.
    This should proceed in normal_mode of block synchronization.
    The synchronization will be very slow if the block height is high.

    TODO
    The "fast_sync" mode should be implemented so that when a new node is connected
    to the network(=branch) and quickly synchronize blocks that have already been verified,
    such as block-boxes or file types.
    If so, which node should receive the request for fast sync?
    A malicious node may transmit the blocks that have not been verified and DDoS attacks
    are possible with requests that require relatively large return values.
    Also, Isn't it necessary to request the current block height(=status)?
    The synchronization will take a long time when there are many gaps in block height during
    the node isn't connected to the network.
     */
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

        for (int i = 0; i < limit; i++) {
            BlockHusk block = branchGroup.getBlockByIndex(branchId, offset++);
            if (block == null) {
                break;
            }
            blockHuskList.add(block);
        }
        return blockHuskList;
    }

    @Override
    public List<TransactionHusk> syncTx(BranchId branchId) {
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
    public void broadcastTx(TransactionHusk tx) {
        try {
            branchGroup.addTransaction(tx);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }
}
