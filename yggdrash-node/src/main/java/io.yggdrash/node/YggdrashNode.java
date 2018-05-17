package io.yggdrash.node;

import io.yggdrash.core.TransactionPool;
import io.yggdrash.node.mock.BlockBuilderMock;
import io.yggdrash.node.mock.BlockChainMock;
import io.yggdrash.node.mock.TransactionPoolMock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class YggdrashNode {

    public static void main(String[] args) {
        SpringApplication.run(YggdrashNode.class, args);
    }

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
    }
}