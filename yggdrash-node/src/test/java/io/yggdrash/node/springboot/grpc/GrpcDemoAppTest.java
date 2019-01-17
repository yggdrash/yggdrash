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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.yggdrash.node.springboot.grpc.context.LocalRunningGrpcPort;
import io.yggdrash.node.springboot.grpc.demo.GrpcDemoApp;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {GrpcDemoApp.class, GrpcDemoAppTest.TestConfig.class})
public class GrpcDemoAppTest {
    @LocalRunningGrpcPort
    private int runningPort;

    @Autowired
    private GrpcServerRunner grpcServerRunner;

    @Autowired
    @Qualifier("globalInterceptor")
    private ServerInterceptor globalInterceptor;

    @Test
    public void testGlobalInterceptor() throws ExecutionException, InterruptedException {
        assertThat(grpcServerRunner).isNotNull();

        // Create channel
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", runningPort)
                .usePlaintext().build();

        PingPongGrpc.newFutureStub(channel)
                .play(Ping.newBuilder().setPing("ping").build()).get().getPong();

        Mockito.verify(globalInterceptor, Mockito.times(1))
                .interceptCall(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @TestConfiguration
    public static class TestConfig {
        @Bean(name = "globalInterceptor")
        @GrpcGlobalInterceptor
        public ServerInterceptor globalInterceptor() {
            ServerInterceptor mock = mock(ServerInterceptor.class);
            Mockito.when(mock.interceptCall(any(), any(), any())).thenAnswer(
                    (Answer<ServerCall.Listener>) invocation ->
                            ((ServerCallHandler) invocation.getArguments()[2]).startCall(
                                    (ServerCall) invocation.getArguments()[0],
                                    (Metadata) invocation.getArguments()[1]
                            ));
            return mock;
        }
    }
}
