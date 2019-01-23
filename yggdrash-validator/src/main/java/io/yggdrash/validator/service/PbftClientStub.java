package io.yggdrash.validator.service;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.PbftServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.concurrent.TimeUnit;

public class PbftClientStub {

    private static final Logger log = LoggerFactory.getLogger(PbftClientStub.class);

    private boolean myclient;
    private String pubKey;
    private String host;
    private int port;
    private String id;
    private boolean isRunning;

    private ManagedChannel channel;
    private PbftServiceGrpc.PbftServiceBlockingStub blockingStub;

    public PbftClientStub(String pubKey, String host, int port) {
        this.pubKey = pubKey;
        this.host = host;
        this.port = port;
        this.id = this.pubKey + "@" + this.host + ":" + this.port;
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

    public long pingPongTime(long timestamp) {
        PbftProto.PbftPingTime pingTime =
                PbftProto.PbftPingTime.newBuilder().setTimestamp(timestamp).build();
        PbftProto.PbftPongTime pongTime;
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

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
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

    public PbftServiceGrpc.PbftServiceBlockingStub getBlockingStub() {
        return blockingStub;
    }

    @Override
    public String toString() {
        return this.pubKey + "@" + this.host + ":" + this.port;
    }

}
