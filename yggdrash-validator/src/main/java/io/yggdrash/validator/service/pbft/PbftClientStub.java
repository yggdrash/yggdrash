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
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.yggdrash.common.config.Constants.TIMEOUT_BLOCK;
import static io.yggdrash.common.config.Constants.TIMEOUT_BLOCKLIST;
import static io.yggdrash.common.config.Constants.TIMEOUT_PING;
import static io.yggdrash.common.config.Constants.TIMEOUT_STATUS;

public class PbftClientStub {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftClientStub.class);

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
        blockingStub.withDeadlineAfter(TIMEOUT_BLOCK, TimeUnit.SECONDS)
                .multicastPbftMessage(pbftMessage);
    }

    public void broadcastPbftBlock(PbftProto.PbftBlock pbftBlock) {
        blockingStub.withDeadlineAfter(TIMEOUT_BLOCK, TimeUnit.SECONDS)
                .broadcastPbftBlock(pbftBlock);
    }

    public List<PbftBlock> getBlockList(long index) {
        log.trace("getBlockList with {}", this.id);

        try {
            PbftProto.PbftBlockList protoBlockList = blockingStub
                    .withDeadlineAfter(TIMEOUT_BLOCKLIST, TimeUnit.SECONDS)
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

        } catch (Exception e) {
            log.debug(e.getMessage());
            return null;
        }
    }

    public long pingPongTime(long timestamp) {
        log.trace("pingPongTime with {}", this.id);

        CommonProto.PingTime pingTime =
                CommonProto.PingTime.newBuilder().setTimestamp(timestamp).build();
        CommonProto.PongTime pongTime;
        try {
            pongTime = blockingStub
                    .withDeadlineAfter(TIMEOUT_PING, TimeUnit.SECONDS)
                    .pingPongTime(pingTime);
            if (Context.current().isCancelled()) {
                return 0L;
            }

        } catch (StatusRuntimeException e) {
            return 0L;
        }

        return pongTime.getTimestamp();
    }

    public PbftStatus exchangePbftStatus(PbftProto.PbftStatus pbftStatus) {
        log.trace("exchangePbftStatus with {}", this.id);

        try {
            this.pbftStatus = new PbftStatus(blockingStub
                    .withDeadlineAfter(TIMEOUT_STATUS, TimeUnit.SECONDS)
                    .exchangePbftStatus(pbftStatus));
            if (Context.current().isCancelled()) {
                return null;
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
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
