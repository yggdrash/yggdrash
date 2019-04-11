package io.yggdrash.validator.service.pbft;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.PbftServiceGrpc;
import io.yggdrash.validator.data.pbft.PbftBlock;
import io.yggdrash.validator.data.pbft.PbftStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PbftClientStub {

    private static final Logger log = LoggerFactory.getLogger(PbftClientStub.class);

    private boolean myclient;
    private final String addr;
    private final String host;
    private final int port;
    private final String id;
    private boolean isRunning;
    private PbftStatus pbftStatus;

    private final ManagedChannel channel;
    private final PbftServiceGrpc.PbftServiceBlockingStub blockingStub;

    public PbftClientStub(String addr, String host, int port) {
        this.addr = addr;
        this.host = host;
        this.port = port;
        this.id = this.addr + "@" + this.host + ":" + this.port;
        this.isRunning = false;

        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = PbftServiceGrpc.newBlockingStub(channel);
    }

    public void multicastPbftMessage(PbftProto.PbftMessage pbftMessage) {
        blockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .multicastPbftMessage(pbftMessage);
    }

    public void broadcastPbftBlock(PbftProto.PbftBlock pbftBlock) {
        blockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .broadcastPbftBlock(pbftBlock);
    }

    public List<PbftBlock> getBlockList(long index) {
        PbftProto.PbftBlockList protoBlockList = blockingStub
                .withDeadlineAfter(3, TimeUnit.SECONDS)
                .getPbftBlockList(
                        CommonProto.Offset.newBuilder().setIndex(index).setCount(10L).build());

        if (Context.current().isCancelled()) {
            return null;
        }

        List<PbftBlock> newPbftBlockList = new ArrayList<>();
        for (PbftProto.PbftBlock protoPbftBlock : protoBlockList.getPbftBlockList()) {
            newPbftBlockList.add(new PbftBlock(protoPbftBlock));
        }

        return newPbftBlockList;
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

    public PbftStatus exchangePbftStatus(PbftProto.PbftStatus pbftStatus) {
        this.pbftStatus = new PbftStatus(blockingStub
                .withDeadlineAfter(3, TimeUnit.SECONDS)
                .exchangePbftStatus(pbftStatus));
        if (Context.current().isCancelled()) {
            return null;
        }
        return this.pbftStatus;
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
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

    public PbftServiceGrpc.PbftServiceBlockingStub getBlockingStub() {
        return blockingStub;
    }

    @Override
    public String toString() {
        return this.addr + "@" + this.host + ":" + this.port;
    }

}
