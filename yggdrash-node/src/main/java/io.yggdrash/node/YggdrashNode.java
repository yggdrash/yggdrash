package io.yggdrash.node;

import io.yggdrash.core.TransactionPool;
import io.yggdrash.core.net.NodeSyncClient;
import io.yggdrash.core.net.NodeSyncServer;
import io.yggdrash.node.mock.BlockBuilderMock;
import io.yggdrash.node.mock.BlockChainMock;
import io.yggdrash.node.mock.TransactionPoolMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
public class YggdrashNode implements CommandLineRunner {
    @Value("${grpc.port}")
    private int grpcPort;

    @Autowired
    NodeSyncServer nodeSyncServer;

    public static void main(String[] args) {
        SpringApplication.run(YggdrashNode.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        nodeSyncServer.setPort(this.grpcPort);
        nodeSyncServer.start();
        nodeSyncServer.blockUntilShutdown();
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

        @Bean
        NodeSyncServer nodeSyncServer() {
            return new NodeSyncServer();
        }
    }

    @Component
    class NodeScheduler {
        @Value("${grpc.port}")
        private int grpcPort;

        private NodeSyncClient nodeSyncClient;

        @PostConstruct
        public void init() {
            int port = grpcPort == 9090 ? 9091 : 9090;
            nodeSyncClient = new NodeSyncClient("localhost", port);
        }

        @Scheduled(fixedRate = 3000)
        public void ping() {
            nodeSyncClient.ping("ping");
        }
    }
}