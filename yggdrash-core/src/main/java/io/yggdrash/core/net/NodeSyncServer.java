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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.BlockChainOuterClass;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import io.yggdrash.proto.Pong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NodeSyncServer {
    private static final Logger log = LoggerFactory.getLogger(NodeSyncServer.class);

    private Server server;
    private int port;

    public void setPort(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new PingPongImpl())
                .addService(new BlockChainImpl())
                .build()
                .start();
        log.info("GRPC Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may has been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            NodeSyncServer.this.stop();
            System.err.println("*** server shut down");
        }));
    }

    /**
     * Stop serving requests and shutdown resources.
     */
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    static class PingPongImpl extends PingPongGrpc.PingPongImplBase {
        @Override
        public void play(Ping request, StreamObserver<Pong> responseObserver) {
            log.debug(request.getPing());
            Pong pong = Pong.newBuilder().setPong("Pong").build();
            responseObserver.onNext(pong);
            responseObserver.onCompleted();
        }
    }

    static class BlockChainImpl extends BlockChainGrpc.BlockChainImplBase {
        private static Set<StreamObserver<BlockChainOuterClass.Transaction>> observers =
                ConcurrentHashMap.newKeySet();

        @Override
        public StreamObserver<BlockChainOuterClass.Transaction> broadcast(
                StreamObserver<BlockChainOuterClass.Transaction> responseObserver) {

            observers.add(responseObserver);

            return new StreamObserver<BlockChainOuterClass.Transaction>() {
                @Override
                public void onNext(BlockChainOuterClass.Transaction tx) {
                    log.debug("Received Tx: {}", tx);

                    for (StreamObserver<BlockChainOuterClass.Transaction> observer : observers) {
                        observer.onNext(tx);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    log.warn("Broadcasting Failed: {}", t);
                    observers.remove(responseObserver);
                    responseObserver.onError(t);
                }

                @Override
                public void onCompleted() {
                    observers.remove(responseObserver);
                    responseObserver.onCompleted();
                }
            };
        }
    }
}
