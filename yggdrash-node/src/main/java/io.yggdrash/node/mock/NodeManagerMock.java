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

import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.Block;
import io.yggdrash.core.BlockChain;
import io.yggdrash.core.NodeEventListener;
import io.yggdrash.core.NodeManager;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.exception.NotValidteException;
import io.yggdrash.core.store.TransactionPool;
import io.yggdrash.node.BlockBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NodeManagerMock implements NodeManager {
    private static final Logger log = LoggerFactory.getLogger(NodeManager.class);

    private final BlockBuilder blockBuilder = new BlockBuilderMock(this);

    private final BlockChain blockChain = new BlockChain();

    private final TransactionPool transactionPool = new TransactionPoolMock();

    private final DefaultConfig defaultConfig = new DefaultConfig();

    private final Wallet wallet = readWallet();

    private NodeEventListener listener;

    private Wallet readWallet() {
        Wallet wallet = null;

        try {
            wallet = new Wallet(this.defaultConfig);
            log.debug("NodeManagerMock wallet = " + wallet.toString());
        } catch (IOException e) {
            log.error("Error IOException");
        } catch (InvalidCipherTextException ice) {
            log.error("Error InvalidCipherTextException");
        }

        return wallet;
    }

    @PostConstruct
    private void init() {
        if (listener == null) {
            return;
        }
        try {
            List<Block> blockList = listener.syncBlock(blockChain.getLastIndex());
            for (Block block : blockList) {
                blockChain.addBlock(block);
            }
            List<Transaction> txList = listener.syncTransaction();
            for (Transaction tx : txList) {
                transactionPool.addTx(tx);
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    @Override
    public void setListener(NodeEventListener listener) {
        this.listener = listener;
    }

    @Override
    public Transaction getTxByHash(String id) {
        return transactionPool.getTxByHash(id);
    }

    @Override
    public Transaction addTransaction(Transaction tx) throws IOException {
        Transaction newTx = transactionPool.addTx(tx);
        if (listener != null) {
            listener.newTransaction(tx);
        }
        return newTx;
    }

    @Override
    public List<Transaction> getTransactionList() {
        return transactionPool.getTxList();
    }

    @Override
    public Set<Block> getBlocks() {
        return new HashSet<>(blockChain.getBlocks().values());
    }

    @Override
    public Block generateBlock() throws IOException, NotValidteException {
        Block block =
                blockBuilder.build(
                        this.wallet,
                        transactionPool.getTxList(),
                        blockChain.getPrevBlock());

        blockChain.addBlock(block);

        if (listener != null) {
            listener.newBlock(block);
        }
        removeTxByBlock(block);
        return block;
    }

    @Override
    public Block addBlock(Block block) throws IOException, NotValidteException {
        Block newBlock = null;
        if (blockChain.isGenesisBlockChain() && block.getIndex() == 0) {
            blockChain.addBlock(block);
            newBlock = block;
        } else if (blockChain.getPrevBlock().nextIndex() == block.getIndex()) {
            blockChain.addBlock(block);
            newBlock = block;
        }
        if (listener != null) {
            listener.newBlock(block);
        }
        removeTxByBlock(block);
        return newBlock;
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

    @Override
    public String getNodeId() {
        return wallet.getNodeId();
    }

    private void removeTxByBlock(Block block) throws IOException {
        if (block == null || block.getData().getTransactionList() == null) {
            return;
        }
        List<String> idList = new ArrayList<>();

        for (Transaction tx : block.getData().getTransactionList()) {
            idList.add(tx.getHashString());
        }
        this.transactionPool.removeTx(idList);
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    public Wallet getWallet() {
        return wallet;
    }
}
