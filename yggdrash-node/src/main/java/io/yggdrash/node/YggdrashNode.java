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

package io.yggdrash.node;

import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.net.Discovery;
import io.yggdrash.core.net.Node;
import io.yggdrash.core.net.NodeServer;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerClientChannel;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.node.config.NodeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class YggdrashNode extends Node
        implements CommandLineRunner, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(YggdrashNodeApp.class);

    private final NodeProperties nodeProperties;

    private final NodeStatus nodeStatus;

    private final BranchGroup branchGroup;

    @Autowired
    public void setNodeServer(NodeServer nodeServer) {
        this.nodeServer = nodeServer;
    }

    @Autowired
    public void setDiscovery(Discovery discovery) {
        this.discovery = discovery;
    }


    YggdrashNode(NodeProperties nodeProperties, NodeStatus nodeStatus, BranchGroup branchGroup) {
        this.nodeProperties = nodeProperties;
        this.nodeStatus = nodeStatus;
        this.branchGroup = branchGroup;
    }

    @Override
    public void run(String... args) {
        String host = nodeProperties.getGrpc().getHost();
        int port = nodeProperties.getGrpc().getPort();
        start(host, port);
        bootstrapping();
    }

    @Override
    public void destroy() {
        log.info("Destroy node=" + getPeerGroup().getOwner());
        getPeerGroup().destroy();
        log.info("Shutting down gRPC server...");
        nodeServer.stop();
    }

    private PeerGroup getPeerGroup() {
        return discovery.getPeerGroup();
    }

    @Override
    protected PeerClientChannel getChannel(Peer peer) {
        return new GRpcClientChannel(peer);
    }

    @Override
    public void bootstrapping() {
        log.info("Bootstrapping... node=" + getPeerGroup().getOwner());
        super.bootstrapping();

        if (nodeProperties.isSeed()) {
            log.info("I'm the Bootstrap Node.");
            nodeStatus.up();
            return;
        }
        nodeStatus.sync();
        syncBlockAndTransaction();
        nodeStatus.up();
    }

    private void syncBlockAndTransaction() {
        try {
            for (BlockChain blockChain : branchGroup.getAllBranch()) {
                BlockChainSync.syncTransaction(blockChain, getPeerGroup());
                BlockChainSync.syncBlock(blockChain, getPeerGroup());
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    private static class BlockChainSync {

        static void syncBlock(BlockChain blockChain, PeerGroup peerGroup) {
            List<BlockHusk> blockList;
            do {
                blockList = peerGroup.syncBlock(blockChain.getBranchId(),
                        blockChain.getLastIndex() + 1);
                for (BlockHusk block : blockList) {
                    blockChain.addBlock(block, false);
                }
            } while (!blockList.isEmpty());
        }

        static void syncTransaction(BlockChain blockChain, PeerGroup peerGroup) {
            List<TransactionHusk> txList = peerGroup.syncTransaction(blockChain.getBranchId());
            for (TransactionHusk tx : txList) {
                try {
                    blockChain.addTransaction(tx);
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
            }
        }
    }
}
