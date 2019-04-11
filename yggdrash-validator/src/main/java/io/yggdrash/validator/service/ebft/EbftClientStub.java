package io.yggdrash.validator.service.ebft;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.proto.EbftServiceGrpc;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.data.ebft.EbftStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EbftClientStub {

    private static final Logger log = LoggerFactory.getLogger(EbftClientStub.class);

    private boolean myclient;
    private final String addr;
    private final String host;
    private final int port;
    private final String id;
    private boolean isRunning;
    private EbftStatus ebftStatus;

    private ManagedChannel channel;
    private final EbftServiceGrpc.EbftServiceBlockingStub blockingStub;

    public EbftClientStub(String addr, String host, int port) {
        this.addr = addr;
        this.host = host;
        this.port = port;
        this.id = this.addr + "@" + this.host + ":" + this.port;
        this.isRunning = false;

        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = EbftServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public long pingPongTime(long timestamp) {
        CommonProto.PingTime pingTime =
                CommonProto.PingTime.newBuilder().setTimestamp(timestamp).build();
        CommonProto.PongTime pongTime;
        try {
            pongTime = blockingStub
                    .withDeadlineAfter(1, TimeUnit.SECONDS)
                    .pingPongTime(pingTime);
        } catch (StatusRuntimeException e) {
            return 0L;
        }

        if (Context.current().isCancelled()) {
            return 0L;
        }

        return pongTime.getTimestamp();
    }

    public EbftStatus exchangeNodeStatus(EbftProto.EbftStatus nodeStatus) {
        this.ebftStatus =
                new EbftStatus(blockingStub
                        .withDeadlineAfter(3, TimeUnit.SECONDS)
                        .exchangeEbftStatus(nodeStatus));
        if (Context.current().isCancelled()) {
            return null;
        }

        return this.ebftStatus;
    }

    public void multicastEbftBlock(EbftProto.EbftBlock block) {
        blockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .multicastEbftBlock(block);
    }

    public void broadcastEbftBlock(EbftProto.EbftBlock block) {
        blockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .broadcastEbftBlock(block);
    }

    public List<EbftBlock> getEbftBlockList(long index) {
        EbftProto.EbftBlockList protoEbftBlockList = blockingStub
                .withDeadlineAfter(3, TimeUnit.SECONDS)
                .getEbftBlockList(
                        CommonProto.Offset.newBuilder().setIndex(index).setCount(10L).build());

        if (Context.current().isCancelled()) {
            return null;
        }

        List<EbftBlock> newEbftBlockList = new ArrayList<>();

        for (EbftProto.EbftBlock block : protoEbftBlockList.getEbftBlockListList()) {
            newEbftBlockList.add(new EbftBlock(block));
        }

        return newEbftBlockList;
    }

    public boolean isMyclient() {
        return myclient;
    }

    public void setMyclient(boolean myclient) {
        this.myclient = myclient;
    }

    public String getAddr() {
        return addr;
    }

    public String getAddress() {
        return this.addr;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getId() {
        return id;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public EbftServiceGrpc.EbftServiceBlockingStub getBlockingStub() {
        return blockingStub;
    }

    @Override
    public String toString() {
        return this.addr + "@" + this.host + ":" + this.port;
    }

}
