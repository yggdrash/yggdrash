package io.yggdrash.validator.service.pbft;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.yggdrash.common.config.Constants;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.PbftServiceGrpc;
import io.yggdrash.proto.Proto;
import io.yggdrash.validator.data.pbft.PbftBlock;
import io.yggdrash.validator.data.pbft.PbftStatus;
import io.yggdrash.validator.service.ConsensusClientStub;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.yggdrash.common.config.Constants.TIMEOUT_BLOCK;
import static io.yggdrash.common.config.Constants.TIMEOUT_BLOCKLIST;
import static io.yggdrash.common.config.Constants.TIMEOUT_PING;
import static io.yggdrash.common.config.Constants.TIMEOUT_STATUS;
import static io.yggdrash.common.config.Constants.TIMEOUT_TRANSACTION;

public class PbftClientStub implements ConsensusClientStub<PbftBlock> {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftClientStub.class);

    private boolean myclient;
    private final String addr;
    private final String host;
    private final int port;
    private final String id;
    private boolean isRunning;

    private ManagedChannel channel;
    private PbftServiceGrpc.PbftServiceBlockingStub blockingStub;

    PbftClientStub(String addr, String host, int port) {
        this.addr = addr;
        this.host = host;
        this.port = port;
        this.id = this.addr + "@" + this.host + ":" + this.port;
        this.isRunning = false;

        if (host == null || host.equals("") || port == 0L) {
            this.channel = null;
            this.blockingStub = null;
        } else {
            try {
                this.channel = ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext()
                        .maxInboundMessageSize(Constants.MAX_GRPC_MESSAGE_LIMIT)
                        .build();
                blockingStub = PbftServiceGrpc.newBlockingStub(channel);
            } catch (Exception e) {
                if (channel != null) {
                    channel.shutdown();
                }
                this.channel = null;
                this.blockingStub = null;
            }
        }
    }

    @Override
    public void multicastTransaction(Proto.Transaction protoTx) {
        blockingStub.withDeadlineAfter(TIMEOUT_TRANSACTION, TimeUnit.SECONDS)
                .multicastTransaction(protoTx);
    }

    void multicastPbftMessage(PbftProto.PbftMessage pbftMessage) {
        blockingStub.withDeadlineAfter(TIMEOUT_BLOCK, TimeUnit.SECONDS)
                .multicastPbftMessage(pbftMessage);
    }

    void broadcastPbftBlock(PbftProto.PbftBlock pbftBlock) {
        blockingStub.withDeadlineAfter(TIMEOUT_BLOCK, TimeUnit.SECONDS)
                .broadcastPbftBlock(pbftBlock);
    }

    @Override
    public List<PbftBlock> getBlockList(long index) {
        log.trace("getBlockList with {}", this.id);

        try {
            PbftProto.PbftBlockList protoBlockList = blockingStub
                    .withDeadlineAfter(TIMEOUT_BLOCKLIST, TimeUnit.SECONDS)
                    .getPbftBlockList(
                            CommonProto.Offset.newBuilder().setIndex(index).setCount(10L).build());

            if (Context.current().isCancelled()) {
                return new ArrayList<>();
            }

            List<PbftBlock> newPbftBlockList = new ArrayList<>();
            for (PbftProto.PbftBlock protoPbftBlock : protoBlockList.getPbftBlockList()) {
                newPbftBlockList.add(new PbftBlock(protoPbftBlock));
            }

            return newPbftBlockList;

        } catch (Exception e) {
            log.debug(e.getMessage());
            return new ArrayList<>();
        }
    }

    public long pingPongTime(long timestamp) {
        log.trace("pingPongTime with {}", this.id);

        if (blockingStub == null) {
            return 0L;
        }

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

        PbftStatus pbftStatus1;
        try {
            pbftStatus1 = new PbftStatus(blockingStub
                    .withDeadlineAfter(TIMEOUT_STATUS, TimeUnit.SECONDS)
                    .exchangePbftStatus(pbftStatus));
            if (Context.current().isCancelled()) {
                return null;
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
            return null;
        }

        return pbftStatus1;
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Override
    public boolean isMyclient() {
        return myclient;
    }

    public void setMyclient(boolean myclient) {
        this.myclient = myclient;
    }

    @Override
    public String getAddr() {
        return addr;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    public void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    @Override
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
