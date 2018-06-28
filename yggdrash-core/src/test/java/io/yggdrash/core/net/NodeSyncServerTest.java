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

package io.yggdrash.core.net;

import com.google.protobuf.ByteString;
import io.grpc.internal.testing.StreamRecorder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.BlockChainProto;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import io.yggdrash.proto.Pong;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NodeSyncServerTest {

    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

    @Test
    public void play() {
        grpcServerRule.getServiceRegistry().addService(new NodeSyncServer.PingPongImpl());

        PingPongGrpc.PingPongBlockingStub blockingStub = PingPongGrpc.newBlockingStub
                (grpcServerRule.getChannel());

        Pong pong = blockingStub.play(Ping.newBuilder().setPing("Ping").build());
        assertEquals("Pong", pong.getPong());
    }

    @Test
    public void broadcastTransaction() throws Exception {
        grpcServerRule.getServiceRegistry().addService(new NodeSyncServer.BlockChainImpl());
        BlockChainGrpc.BlockChainStub stub = BlockChainGrpc.newStub(grpcServerRule.getChannel());
        StreamRecorder<BlockChainProto.Transaction> responseObserver = StreamRecorder.create();
        StreamObserver<BlockChainProto.Transaction> requestObserver
                = stub.broadcastTransaction(responseObserver);

        for (int i = 1; i <= 3; i++) {
            BlockChainProto.Transaction request
                    = BlockChainProto.Transaction.newBuilder().setData("tx" + i).build();
            requestObserver.onNext(request);
        }
        requestObserver.onCompleted();

        BlockChainProto.Transaction firstTxResponse = responseObserver.firstValue().get();
        assertEquals("tx1", firstTxResponse.getData());
    }

    @Test
    public void broadcastBlock() throws Exception {
        grpcServerRule.getServiceRegistry().addService(new NodeSyncServer.BlockChainImpl());
        BlockChainGrpc.BlockChainStub stub = BlockChainGrpc.newStub(grpcServerRule.getChannel());
        StreamRecorder<BlockChainProto.Block> responseObserver = StreamRecorder.create();
        StreamObserver<BlockChainProto.Block> requestObserver
                = stub.broadcastBlock(responseObserver);

        for (int i = 1; i <= 3; i++) {
            BlockChainProto.Transaction tx
                    = BlockChainProto.Transaction.newBuilder().setData("tx").build();
            BlockChainProto.Block block = BlockChainProto.Block.newBuilder()
                    .setHeader(BlockChainProto.BlockHeader.newBuilder().setAuthor(
                            ByteString.copyFromUtf8("author" + i)))
                    .setData(BlockChainProto.BlockBody.newBuilder().addTrasactions(tx)).build();
            requestObserver.onNext(block);
        }
        requestObserver.onCompleted();

        BlockChainProto.Block firstBlockResponse = responseObserver.firstValue().get();
        assertEquals("author1", firstBlockResponse.getHeader().getAuthor().toStringUtf8());
        assertEquals("tx", firstBlockResponse.getData().getTrasactions(0).getData());
    }
}
