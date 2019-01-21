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

package io.yggdrash.node.service;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.net.BlockChainConsumer;
import io.yggdrash.core.net.DiscoveryConsumer;
import io.yggdrash.core.net.PeerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GRpcPeerListener implements PeerListener {
    private static final Logger log = LoggerFactory.getLogger(GRpcPeerListener.class);

    private List<BindableService> bindableServiceList = new ArrayList<>();

    @Override
    public void initConsumer(DiscoveryConsumer discoveryConsumer,
                             BlockChainConsumer blockChainConsumer) {
        if (blockChainConsumer != null) {
            bindableServiceList.add(new GRpcBlockChainService(blockChainConsumer));
        }
        bindableServiceList.add(new GRpcDiscoveryService(discoveryConsumer));
    }

    private Server server;

    @Override
    public void start(String host, int port) {
        ServerBuilder builder = ServerBuilder.forPort(port);
        for (BindableService service : bindableServiceList) {
            builder.addService(service);
        }
        try {
            this.server = builder.build().start();
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }

        log.info("GRPC Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may has been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            this.stop();
            System.err.println("*** server shut down");
        }));

        if (server == null) {
            throw new FailedOperationException("Node start fail.");
        }

        Thread awaitThread = new Thread(() -> {
            try {
                server.awaitTermination();
            } catch (InterruptedException e) {
                log.error("gRPC server stopped.", e);
            }
        });
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    /**
     * Stop serving requests and shutdown resources.
     */
    @Override
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

}
