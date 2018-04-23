package io.yggdrash.core;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.yggdrash.proto.Block;
import io.yggdrash.proto.BlockServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class GrpcMessageSender {

    private static final Logger log = LoggerFactory.getLogger(GrpcMessageSender.class);
    private BlockServiceGrpc.BlockServiceBlockingStub blockServiceBlockingStub;

    private Long height = 0L;

    @Scheduled(fixedRate = 1000)
    public void sendBlock() {
        Block block = Block.newBuilder().setHeight(height++).build();
        Block response = blockServiceBlockingStub.sync(block);
        log.debug("response: {}", response);
    }

    @PostConstruct
    private void initialize() {
        ManagedChannel managedChannel = ManagedChannelBuilder
                .forAddress("localhost", 6565).usePlaintext(true).build();
        blockServiceBlockingStub = BlockServiceGrpc.newBlockingStub(managedChannel);
    }
}
