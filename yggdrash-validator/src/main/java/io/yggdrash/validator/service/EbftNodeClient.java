package io.yggdrash.validator.service;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.ConsensusEbftGrpc;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.validator.data.BlockCon;
import io.yggdrash.validator.data.NodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EbftNodeClient {

    private static final Logger log = LoggerFactory.getLogger(EbftNodeClient.class);

    private boolean myclient;
    private String pubKey;
    private String host;
    private int port;
    private String id;
    private boolean isRunning;
    private NodeStatus nodeStatus;

    private ManagedChannel channel;
    private ConsensusEbftGrpc.ConsensusEbftBlockingStub blockingStub;

    public EbftNodeClient(String pubKey, String host, int port) {
        this.pubKey = pubKey;
        this.host = host;
        this.port = port;
        this.id = this.pubKey + "@" + this.host + ":" + this.port;
        this.isRunning = false;

        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = ConsensusEbftGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public long pingPongTime(long timestamp) {
        EbftProto.PingTime pingTime =
                EbftProto.PingTime.newBuilder().setTimestamp(timestamp).build();
        EbftProto.PongTime pongTime;
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

    public NodeStatus exchangeNodeStatus(EbftProto.NodeStatus nodeStatus) {
        this.nodeStatus =
                new NodeStatus(blockingStub
                        .withDeadlineAfter(3, TimeUnit.SECONDS)
                        .exchangeNodeStatus(nodeStatus));
        if (Context.current().isCancelled()) {
            return null;
        }

        return this.nodeStatus;
    }

    public void broadcastBlockCon(EbftProto.BlockCon blockCon) {
        blockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .broadcastBlockCon(blockCon);
    }

    public List<BlockCon> getBlockConList(long index) {
        EbftProto.BlockConList protoBlockConList = blockingStub
                .withDeadlineAfter(10, TimeUnit.SECONDS)
                .getBlockConList(
                        EbftProto.Offset.newBuilder().setIndex(index).setCount(10L).build());

        if (Context.current().isCancelled()) {
            return null;
        }

        List<BlockCon> newBlockConList = new ArrayList<>();

        for (EbftProto.BlockCon protoBlockCon : protoBlockConList.getBlockConListList()) {
            newBlockConList.add(new BlockCon(protoBlockCon));
        }

        return newBlockConList;
    }

    public boolean isMyclient() {
        return myclient;
    }

    public void setMyclient(boolean myclient) {
        this.myclient = myclient;
    }

    public String getPubKey() {
        return pubKey;
    }

    public String getAddress() {
        return Hex.toHexString(Wallet.calculateAddress(Hex.decode(this.pubKey)));
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

    public ConsensusEbftGrpc.ConsensusEbftBlockingStub getBlockingStub() {
        return blockingStub;
    }

    @Override
    public String toString() {
        return this.pubKey + "@" + this.host + ":" + this.port;
    }

}
