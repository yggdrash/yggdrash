package io.yggdrash.node.service.pbft;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.yggdrash.core.blockchain.pbft.PbftBlock;
import io.yggdrash.core.blockchain.pbft.PbftStatus;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.PbftServiceGrpc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PbftClientStub {

    private boolean myClient;
    private Peer owner;
    private boolean isRunning;

    private ManagedChannel channel;
    private PbftServiceGrpc.PbftServiceBlockingStub blockingStub;

    PbftClientStub(Peer owner) {
        this.owner = owner;
        this.isRunning = false;

        this.channel = ManagedChannelBuilder.forAddress(owner.getHost(), owner.getPort()).usePlaintext().build();
        this.blockingStub = PbftServiceGrpc.newBlockingStub(channel);
    }

    void multicastPbftMessage(PbftProto.PbftMessage pbftMessage) {
        blockingStub.withDeadlineAfter(3, TimeUnit.SECONDS).multicastPbftMessage(pbftMessage);
    }

    List<PbftBlock> getBlockList(long index) {
        PbftProto.PbftBlockList protoBlockList = blockingStub
                .withDeadlineAfter(3, TimeUnit.SECONDS)
                .getPbftBlockList(CommonProto.Offset.newBuilder().setIndex(index).setCount(10L).build());

        if (Context.current().isCancelled()) {
            return Collections.emptyList();
        }

        List<PbftBlock> newPbftBlockList = new ArrayList<>();
        for (PbftProto.PbftBlock protoPbftBlock : protoBlockList.getPbftBlockList()) {
            newPbftBlockList.add(new PbftBlock(protoPbftBlock));
        }

        return newPbftBlockList;
    }

    long pingPongTime(long timestamp) {
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

    PbftStatus exchangePbftStatus(PbftProto.PbftStatus protoPbftStatus) {
        PbftStatus pbftStatus = new PbftStatus(blockingStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .exchangePbftStatus(protoPbftStatus));
        if (Context.current().isCancelled()) {
            return null;
        }
        return pbftStatus;
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    boolean isMyClient() {
        return myClient;
    }

    void setMyClient(boolean myClient) {
        this.myClient = myClient;
    }

    String getPubKey() {
        return owner.getPubKey().toString();
    }

    String getId() {
        return owner.toString();
    }

    boolean isRunning() {
        return isRunning;
    }

    void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    @Override
    public String toString() {
        return owner.getYnodeUri();
    }

}
