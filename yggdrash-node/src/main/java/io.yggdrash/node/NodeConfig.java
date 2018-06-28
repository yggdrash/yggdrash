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

import io.yggdrash.core.TransactionPool;
import io.yggdrash.core.net.NodeSyncServer;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.node.mock.BlockBuilderMock;
import io.yggdrash.node.mock.BlockChainMock;
import io.yggdrash.node.mock.TransactionPoolMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;

@Configuration
class NodeConfig {

    @Bean
    BlockBuilder blockBuilder() {
        return new BlockBuilderMock();
    }

    @Bean
    BlockChain blockChain() {
        return new BlockChainMock();
    }

    @Bean
    TransactionPool transactionPool() {
        return new TransactionPoolMock();
    }

    @Bean
    NodeSyncServer nodeSyncServer() {
        return new NodeSyncServer();
    }

    @Bean
    PeerGroup peerGroup() {
        return new PeerGroup();
    }

    @Bean
    MessageSender messageSender() {
        return new MessageSender();
    }

    @Bean
    BeanNameUrlHandlerMapping beanNameUrlHandlerMapping() {
        return new BeanNameUrlHandlerMapping();
    }
}
