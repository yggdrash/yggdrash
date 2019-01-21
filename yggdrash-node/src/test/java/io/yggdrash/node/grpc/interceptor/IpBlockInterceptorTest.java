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

package io.yggdrash.node.grpc.interceptor;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.StatusRuntimeException;
import io.yggdrash.node.springboot.grpc.GrpcGlobalInterceptor;
import io.yggdrash.node.springboot.grpc.GrpcServerRunner;
import io.yggdrash.node.springboot.grpc.context.LocalRunningGrpcPort;
import io.yggdrash.node.springboot.grpc.demo.GrpcDemoApp;
import io.yggdrash.proto.PeerGrpc;
import io.yggdrash.proto.Proto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {GrpcDemoApp.class, IpBlockInterceptorTest.TestConfig.class})
public class IpBlockInterceptorTest {

    @LocalRunningGrpcPort
    private int runningPort;

    @Autowired
    private GrpcServerRunner grpcServerRunner;

    @Autowired
    @Qualifier("ipBlockInterceptor")
    private ServerInterceptor ipBlockInterceptor;

    @Test(expected = StatusRuntimeException.class)
    public void testIpBlockInterceptor() {
        assertThat(grpcServerRunner).isNotNull();

        // Create channel
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", runningPort)
                .usePlaintext().build();

        Proto.Pong reply = PeerGrpc.newBlockingStub(channel)
                .play(Proto.Ping.newBuilder().setPing("ping").build());

        reply.getPong();
        assertThat(reply.getPong()).isEqualTo("Pong");

        // Set ip for blocking
        ((IpBlockInterceptor) ipBlockInterceptor).setBlackIps(new String[] {"127.0.0.1"});

        PeerGrpc.newBlockingStub(channel)
                .play(Proto.Ping.newBuilder().setPing("ping").build());
    }

    @TestConfiguration
    public static class TestConfig {
        @Bean(name = "ipBlockInterceptor")
        @GrpcGlobalInterceptor
        public ServerInterceptor ipBlockInterceptor() {
            return new IpBlockInterceptor();
        }
    }
}
