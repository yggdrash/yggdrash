package io.yggdrash.node.service;

import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.net.BestBlock;
import io.yggdrash.core.net.DiscoveryConsumer;
import io.yggdrash.core.net.Peer;
import io.yggdrash.proto.PeerGrpc;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GRpcDiscoveryService extends PeerGrpc.PeerImplBase {
    private static final Logger log = LoggerFactory.getLogger(GRpcDiscoveryService.class);

    private DiscoveryConsumer discoveryConsumer;

    GRpcDiscoveryService(DiscoveryConsumer discoveryConsumer) {
        this.discoveryConsumer = discoveryConsumer;
    }

    @Override
    public void findPeers(Proto.RequestPeer request, StreamObserver<Proto.PeerList> responseObserver) {
        Peer peer = Peer.valueOf(request.getPubKey(), request.getIp(), request.getPort());

        for (Proto.BestBlock bestBlock : request.getBestBlocksList()) {
            BestBlock bb = toBestBlock(bestBlock);
            peer.updateBestBlock(bb);
        }

        List<String> list = discoveryConsumer.findPeers(peer);
        Proto.PeerList.Builder peerListBuilder = Proto.PeerList.newBuilder();
        for (String url : list) {
            peerListBuilder.addPeers(Proto.PeerInfo.newBuilder().setUrl(url).build());
        }
        Proto.PeerList peerList = peerListBuilder.build();
        responseObserver.onNext(peerList);
        responseObserver.onCompleted();

        discoveryConsumer.afterFindPeersResponse();
    }

    @Override
    public void play(Proto.Ping request, StreamObserver<Proto.Pong> responseObserver) {
        String url = request.getPeer().getUrl();
        Peer from = Peer.valueOf(url);
        log.debug("Received " + request.getPing());
        String reply = discoveryConsumer.play(from, request.getPing());
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