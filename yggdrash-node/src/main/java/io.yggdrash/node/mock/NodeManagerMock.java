/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.mock;

import io.yggdrash.core.Block;
import io.yggdrash.core.BlockChain;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionPool;
import io.yggdrash.core.exception.NotValidteException;
import io.yggdrash.node.BlockBuilder;
import io.yggdrash.node.MessageSender;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class NodeManagerMock implements NodeManager {

    private final BlockBuilder blockBuilder = new BlockBuilderMock();

    private final BlockChain blockChain = new BlockChain();

    private final TransactionPool transactionPool = new TransactionPoolMock();

    public NodeManagerMock(MessageSender messageSender) {
        transactionPool.setListener(messageSender);
    }

    @Override
    public Transaction getTxByHash(String id) {
        return transactionPool.getTxByHash(id);
    }

    @Override
    public Transaction addTransaction(Transaction tx) throws IOException {
        return transactionPool.addTx(tx);
    }

    @Override
    public Set<Block> getBlocks() {
        Set<Block> blockSet = new HashSet<>();
        for (Block block : blockChain.getBlocks().values()) {
            blockSet.add(block);
        }
        return blockSet;
    }

    @Override
    public Block addBlock() throws IOException, NotValidteException {
        Block block =
                blockBuilder.build(transactionPool.getTransactionList(), blockChain.getPrevBlock());
        blockChain.addBlock(block);
        return block;
    }

    @Override
    public Block getBlockByIndexOrHash(String indexOrHash) {

        if (isNumeric(indexOrHash)) {
            int index = Integer.parseInt(indexOrHash);
            return blockChain.getBlockByIndex(index);
        } else {
            return blockChain.getBlockByHash(indexOrHash);
        }
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}
