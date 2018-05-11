package io.yggdrash.node;

import io.yggdrash.core.BlockChain;
import io.yggdrash.core.BlockGenerator;
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
        BlockGenerator blockGenerator() {
            return new BlockGenerator();
        }

        @Bean
        BlockChain blockChain() {
            return new BlockChain();
        }
    }
}