package io.yggdrash.node.service;

import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.net.DiscoveryConsumer;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.node.springboot.grpc.GrpcService;
import io.yggdrash.proto.PeerServiceGrpc;
import io.yggdrash.proto.Proto;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@GrpcService
public class DiscoveryService extends PeerServiceGrpc.PeerServiceImplBase {

    private final DiscoveryConsumer discoveryConsumer;

    @Autowired
    public DiscoveryService(DiscoveryConsumer discoveryConsumer) {
        this.discoveryConsumer = discoveryConsumer;
    }

    @Override
    public void findPeers(Proto.TargetPeer target, StreamObserver<Proto.PeerList> responseObserver) {
        Peer peer = Peer.valueOf(target.getPubKey(), target.getIp(), target.getPort());
        BranchId branchId = BranchId.of(target.getBranch().toByteArray());
        List<Peer> list = discoveryConsumer.findPeers(branchId, peer); // peer -> target
        Proto.PeerList.Builder peerListBuilder = Proto.PeerList.newBuilder();

        for (Peer p : list) {
            peerListBuilder.addPeers(Proto.PeerInfo.newBuilder().setUrl(p.getYnodeUri()).build());
        }

        Proto.PeerList peerList = peerListBuilder.build();

        responseObserver.onNext(peerList);
        responseObserver.onCompleted();
    }

    @Override
    public void ping(Proto.Ping request, StreamObserver<Proto.Pong> responseObserver) {
        //TODO peer validation
        Peer from = Peer.valueOf(request.getFrom());
        from.setBestBlock(request.getBestBlock());
        Peer to = Peer.valueOf(request.getTo());
        BranchId branchId = BranchId.of(request.getBranch().toByteArray());
        String reply = discoveryConsumer.ping(branchId, from, to, request.getPing());
        Proto.Pong pong = Proto.Pong.newBuilder().setPong(reply).build();
        responseObserver.onNext(pong);
        responseObserver.onCompleted();
    }
}
