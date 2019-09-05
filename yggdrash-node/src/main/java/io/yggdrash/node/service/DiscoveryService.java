package io.yggdrash.node.service;

import io.grpc.Grpc;
import io.grpc.stub.StreamObserver;
import io.yggdrash.common.config.Constants;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.SyncManager;
import io.yggdrash.core.net.DiscoveryConsumer;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.node.springboot.grpc.GrpcServerRunner;
import io.yggdrash.node.springboot.grpc.GrpcService;
import io.yggdrash.proto.DiscoveryServiceGrpc;
import io.yggdrash.proto.Proto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Profile({Constants.ActiveProfiles.NODE, Constants.ActiveProfiles.BOOTSTRAP})
@GrpcService
public class DiscoveryService extends DiscoveryServiceGrpc.DiscoveryServiceImplBase {

    private final DiscoveryConsumer discoveryConsumer;

    private SyncManager syncManager;

    private GrpcServerRunner grpcServerRunner;

    @Autowired
    private BranchGroup branchGroup;

    @Autowired
    public DiscoveryService(DiscoveryConsumer discoveryConsumer, GrpcServerRunner grpcServerRunner) {
        this.discoveryConsumer = discoveryConsumer;
        this.grpcServerRunner = grpcServerRunner;
    }

    @Autowired
    private void setSyncManager(SyncManager syncManager) {
        this.syncManager = syncManager;
        discoveryConsumer.setListener(syncManager);
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
        BranchId branchId = BranchId.of(request.getBranch().toByteArray());
        if (branchId == null || branchId.toString().equals("")) {
            return;
        }

        Peer from = Peer.valueOf(request.getFrom());
        Peer to = Peer.valueOf(request.getTo());

        String grpcHost = "127.0.0.1"; // TODO: change when considering test cases.
        if (grpcServerRunner != null) {
            grpcHost = grpcServerRunner.getServerCallCapture().get()
                    .getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR)
                    .toString().split(":")[0].replaceAll("/", "");
        }

        if (!from.getHost().equals(grpcHost)) {
            return;
        }

        from.setBestBlock(request.getBestBlock());
        to.setBestBlock(branchGroup != null ? branchGroup.getLastIndex(branchId) : 0L);
        Proto.Pong pong = discoveryConsumer.ping(
                branchId, from, to, request.getPing(), to.getBestBlock());

        responseObserver.onNext(pong);
        responseObserver.onCompleted();
    }
}
