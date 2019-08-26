package io.yggdrash.validator.service.ebft;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.yggdrash.common.config.Constants;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.proto.EbftServiceGrpc;
import io.yggdrash.proto.Proto;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.data.ebft.EbftStatus;
import io.yggdrash.validator.service.ConsensusClientStub;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.yggdrash.common.config.Constants.TIMEOUT_BLOCK;
import static io.yggdrash.common.config.Constants.TIMEOUT_BLOCKLIST;
import static io.yggdrash.common.config.Constants.TIMEOUT_PING;
import static io.yggdrash.common.config.Constants.TIMEOUT_STATUS;
import static io.yggdrash.common.config.Constants.TIMEOUT_TRANSACTION;

public class EbftClientStub implements ConsensusClientStub<EbftBlock> {

    private boolean myclient;
    private final String addr;
    private final String host;
    private final int port;
    private final String id;
    private boolean isRunning;

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
                .maxInboundMessageSize(Constants.MAX_GRPC_MESSAGE_LIMIT)
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
                    .withDeadlineAfter(TIMEOUT_PING, TimeUnit.SECONDS)
                    .pingPongTime(pingTime);
        } catch (StatusRuntimeException e) {
            return 0L;
        }

        if (Context.current().isCancelled()) {
            return 0L;
        }

        return pongTime.getTimestamp();
    }

    @Override
    public void multicastTransaction(Proto.Transaction protoTx) {
        blockingStub.withDeadlineAfter(TIMEOUT_TRANSACTION, TimeUnit.SECONDS)
                .multicastTransaction(protoTx);
    }

    public EbftStatus exchangeNodeStatus(EbftProto.EbftStatus nodeStatus) {
        EbftStatus ebftStatus = new EbftStatus(blockingStub
                .withDeadlineAfter(TIMEOUT_STATUS, TimeUnit.SECONDS)
                .exchangeEbftStatus(nodeStatus));
        if (Context.current().isCancelled()) {
            return null;
        }

        return ebftStatus;
    }

    public void multicastEbftBlock(EbftProto.EbftBlock block) {
        blockingStub.withDeadlineAfter(TIMEOUT_BLOCK, TimeUnit.SECONDS)
                .multicastEbftBlock(block);
    }

    public void broadcastEbftBlock(EbftProto.EbftBlock block) {
        blockingStub.withDeadlineAfter(TIMEOUT_BLOCK, TimeUnit.SECONDS)
                .broadcastEbftBlock(block);
    }

    @Override
    public List<EbftBlock> getBlockList(long index) {
        EbftProto.EbftBlockList protoEbftBlockList = blockingStub
                .withDeadlineAfter(TIMEOUT_BLOCKLIST, TimeUnit.SECONDS)
                .getEbftBlockList(
                        CommonProto.Offset.newBuilder().setIndex(index).setCount(10L).build());

        List<EbftBlock> newEbftBlockList = new ArrayList<>();
        if (Context.current().isCancelled()) {
            return newEbftBlockList;
        }

        for (EbftProto.EbftBlock block : protoEbftBlockList.getEbftBlockList()) {
            newEbftBlockList.add(new EbftBlock(block));
        }

        return newEbftBlockList;
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

    public EbftServiceGrpc.EbftServiceBlockingStub getBlockingStub() {
        return blockingStub;
    }

    @Override
    public String toString() {
        return this.addr + "@" + this.host + ":" + this.port;
    }

}
