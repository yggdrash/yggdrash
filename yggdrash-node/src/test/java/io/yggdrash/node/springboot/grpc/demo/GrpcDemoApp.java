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

package io.yggdrash.node.springboot.grpc.demo;

import io.grpc.stub.StreamObserver;
import io.yggdrash.node.springboot.grpc.GrpcService;
import io.yggdrash.proto.PeerGrpc;
import io.yggdrash.proto.Proto.Ping;
import io.yggdrash.proto.Proto.Pong;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GrpcDemoApp {

    public static void main(String[] args) {
        SpringApplication.run(GrpcDemoApp.class, args);
    }

    @GrpcService
    public static class PingPongService extends PeerGrpc.PeerImplBase {
        @Override
        public void play(Ping request, StreamObserver<Pong> responseObserver) {
            System.out.println("In PingPongService");
            Pong pong = Pong.newBuilder().setPong("Pong").build();
            responseObserver.onNext(pong);
            responseObserver.onCompleted();
        }
    }
}
