package io.yggdrash.node.service;

import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.net.BestBlock;
import io.yggdrash.core.net.DiscoveryConsumer;
import io.yggdrash.core.net.Peer;
import io.yggdrash.node.springboot.grpc.GrpcService;
import io.yggdrash.proto.PeerGrpc;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@GrpcService
public class DiscoveryService extends PeerGrpc.PeerImplBase {
    private static final Logger log = LoggerFactory.getLogger(DiscoveryService.class);

    private DiscoveryConsumer discoveryConsumer;

    @Autowired
    public DiscoveryService(DiscoveryConsumer discoveryConsumer) {
        this.discoveryConsumer = discoveryConsumer;
    }

    @Override
    public void findPeers(Proto.TargetPeer target, StreamObserver<Proto.PeerList> responseObserver) {
        Peer peer = Peer.valueOf(target.getPubKey(), target.getIp(), target.getPort());

        List<Peer> list = discoveryConsumer.findPeers(peer); // peer -> target
        Proto.PeerList.Builder peerListBuilder = Proto.PeerList.newBuilder();

        for (Peer p : list) {
            peerListBuilder.addPeers(Proto.PeerInfo.newBuilder().setUrl(p.getYnodeUri()).build());
        }

        Proto.PeerList peerList = peerListBuilder.build();

        responseObserver.onNext(peerList);
        responseObserver.onCompleted();

        discoveryConsumer.afterFindPeersResponse();
    }

    @Override
    public void ping(Proto.Ping request, StreamObserver<Proto.Pong> responseObserver) {
        //TODO peer validation
        Peer from = Peer.valueOf(request.getFrom());
        for (Proto.BestBlock bestBlock : request.getBestBlocksList()) {
            from.updateBestBlock(toBestBlock(bestBlock));
        }
        Peer to = Peer.valueOf(request.getTo());
        log.debug("Received " + request.getPing());
        String reply = discoveryConsumer.ping(from, to, request.getPing());
        Proto.Pong pong = Proto.Pong.newBuilder().setPong(reply).build();
        responseObserver.onNext(pong);
        responseObserver.onCompleted();
    }

    private BestBlock toBestBlock(Proto.BestBlock bestBlock) {
        BranchId branchId = BranchId.of(bestBlock.getBranch().toByteArray());
        long index = bestBlock.getIndex();
        return BestBlock.of(branchId, index);
    }
}