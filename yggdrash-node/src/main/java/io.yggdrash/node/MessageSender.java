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

package io.yggdrash.node;

import com.google.protobuf.ByteString;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionEventListener;
import io.yggdrash.core.net.NodeSyncClient;
import io.yggdrash.proto.BlockChainProto;
import io.yggdrash.util.SerializeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import javax.annotation.PostConstruct;

@Service
public class MessageSender implements DisposableBean, TransactionEventListener {
    private static final Logger log = LoggerFactory.getLogger(MessageSender.class);

    @Value("${grpc.port}")
    private int grpcPort;

    private NodeSyncClient nodeSyncClient;

    @PostConstruct
    public void init() throws InterruptedException {
        int port = grpcPort == 9090 ? 9091 : 9090;
        log.info("Connecting gRPC Server at [{}]", port);
        nodeSyncClient = new NodeSyncClient("localhost", port);
//        nodeSyncClient.blockUtilShutdown();
    }

    public void ping() {
        nodeSyncClient.ping("Ping");
    }

    public void broadcastBlock() {
        nodeSyncClient.broadcastBlock(createBlocks());
    }

    @Override
    public void destroy() {
        nodeSyncClient.stop();
    }

    private static BlockChainProto.Block[] createBlocks() {
        return new BlockChainProto.Block[] {
                BlockChainProto.Block.newBuilder()
                        .setHeader(BlockChainProto.BlockHeader.newBuilder().setAuthor(
                                ByteString.copyFromUtf8("author1"))).build(),
                BlockChainProto.Block.newBuilder()
                        .setHeader(BlockChainProto.BlockHeader.newBuilder().setAuthor(
                                ByteString.copyFromUtf8("author2"))).build(),
                BlockChainProto.Block.newBuilder()
                        .setHeader(BlockChainProto.BlockHeader.newBuilder().setAuthor(
                                ByteString.copyFromUtf8("author3"))).build()
        };
    }

    @Override
    public void newTransaction(Transaction tx) {
        log.debug("New transaction={}", tx.getData());
        nodeSyncClient.broadcastTransaction(new BlockChainProto.Transaction[] {Transaction.of(tx)});
    }
}
