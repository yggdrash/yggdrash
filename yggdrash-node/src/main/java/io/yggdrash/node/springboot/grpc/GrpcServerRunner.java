/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.springboot.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;

import java.io.IOException;

public class GrpcServerRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(GrpcServerRunner.class);

    private final GrpcServerBuilderConfigurer configurer;
    private final ServerBuilder<?> serverBuilder;

    private Server server;

    public GrpcServerRunner(GrpcServerBuilderConfigurer configurer, ServerBuilder<?> serverBuilder) {
        this.configurer = configurer;
        this.serverBuilder = serverBuilder;
    }

    @Override
    public void run(String... args) throws IOException {
        log.info("Starting gRPC Server ...");

        configurer.configure(serverBuilder);

        // find and register all GrpcService enabled beans
        server = serverBuilder.build().start();

        log.info("gRPC Server started, listening on port {}.", server.getPort());
        startDaemonAwaitThread();
    }

    private void startDaemonAwaitThread() {
        Thread awaitThread = new Thread(() -> {
            try {
                GrpcServerRunner.this.server.awaitTermination();
            } catch (InterruptedException e) {
                log.error("gRPC server stopped.", e);
            }
        });
        awaitThread.setDaemon(false);
        awaitThread.start();
    }
}
