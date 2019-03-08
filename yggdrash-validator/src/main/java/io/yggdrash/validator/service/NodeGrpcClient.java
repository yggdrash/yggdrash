package io.yggdrash.validator.service;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.PeerGrpc;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class NodeGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(NodeGrpcClient.class);
    private static final int DEFAULT_LIMIT = 10;

    private String host;
    private int port;

    private ManagedChannel channel;
    private final PeerGrpc.PeerBlockingStub blockingPeerStub;
    private final BlockChainGrpc.BlockChainBlockingStub blockingBlockChainStub;
    private final BlockChainGrpc.BlockChainBlockingStub asyncBlockChainStub;

    public NodeGrpcClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingPeerStub = PeerGrpc.newBlockingStub(channel);
        this.blockingBlockChainStub = BlockChainGrpc.newBlockingStub(channel);
        this.asyncBlockChainStub = BlockChainGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public PeerGrpc.PeerBlockingStub getBlockingPeerStub() {
        return blockingPeerStub;
    }

    public BlockChainGrpc.BlockChainBlockingStub getBlockingBlockChainStub() {
        return blockingBlockChainStub;
    }

    public BlockChainGrpc.BlockChainBlockingStub getAsyncBlockChainStub() {
        return asyncBlockChainStub;
    }

    public List<Proto.Block> syncBlock(byte[] branchId, long offset) {
        NetProto.SyncLimit syncLimit = NetProto.SyncLimit.newBuilder()
                .setOffset(offset)
                .setLimit(DEFAULT_LIMIT)
                .setBranch(ByteString.copyFrom(branchId)).build();
        return blockingBlockChainStub.simpleSyncBlock(syncLimit).getBlocksList();
    }

    public List<Proto.Transaction> syncTransaction(byte[] branchId) {
        NetProto.SyncLimit syncLimit = NetProto.SyncLimit.newBuilder()
                .setBranch(ByteString.copyFrom(branchId)).build();
        return blockingBlockChainStub.simpleSyncTransaction(syncLimit).getTransactionsList();
    }

    public void broadcastTransaction(List<Proto.Transaction> txs) {
        for (Proto.Transaction tx : txs) {
            log.trace("Sending transaction: {}", tx);
            asyncBlockChainStub.simpleBroadcastTransaction(tx);
        }
    }
}
