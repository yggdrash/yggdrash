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

import io.grpc.stub.StreamObserver;
import io.yggdrash.node.springboot.grpc.autoconfigure.GrpcAutoConfiguration;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import io.yggdrash.proto.Pong;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {GrpcAutoConfiguration.class, GrpcAutoConfigTest.TestConfig.class})
public class GrpcAutoConfigTest {
    @Test
    public void testServerRun() {

    }

    @TestConfiguration
    @ComponentScan    // When ComponentScan off, gRpcServer don't running
    public static class TestConfig {

        @GrpcService
        public static class PingPongService extends PingPongGrpc.PingPongImplBase {

            @Override
            public void play(Ping request, StreamObserver<Pong> responseObserver) {
                super.play(request, responseObserver);
                responseObserver.onCompleted();
            }
        }
    }
}
