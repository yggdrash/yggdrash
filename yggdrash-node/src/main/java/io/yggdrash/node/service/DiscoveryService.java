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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Objects;

@Profile({Constants.ActiveProfiles.NODE, Constants.ActiveProfiles.BOOTSTRAP})
@GrpcService
public class DiscoveryService extends DiscoveryServiceGrpc.DiscoveryServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(DiscoveryService.class);

    private final DiscoveryConsumer discoveryConsumer;

    private SyncManager syncManager;

    private final GrpcServerRunner grpcServerRunner;

    private final BranchGroup branchGroup;

    @Autowired
    public DiscoveryService(
            DiscoveryConsumer discoveryConsumer, GrpcServerRunner grpcServerRunner, BranchGroup branchGroup) {
        this.discoveryConsumer = discoveryConsumer;
        this.grpcServerRunner = grpcServerRunner;
        this.branchGroup = branchGroup;
    }

    @Autowired
    private void setSyncManager(SyncManager syncManager) {
        this.syncManager = syncManager;
        discoveryConsumer.setListener(syncManager);
    }

    @Override
    public void findPeers(Proto.TargetPeer target, StreamObserver<Proto.PeerList> responseObserver) {
        try {
            Peer peer = Peer.valueOf(target.getPubKey(), target.getIp(), target.getPort());
            BranchId branchId = BranchId.of(target.getBranch().toByteArray());
            List<Peer> list = discoveryConsumer.findPeers(branchId, peer); // peer -> target
            Proto.PeerList.Builder peerListBuilder = Proto.PeerList.newBuilder();

            for (Peer p : list) {
                peerListBuilder.addPeers(Proto.PeerInfo.newBuilder().setUrl(p.getYnodeUri()).build());
            }

            Proto.PeerList peerList = peerListBuilder.build();
            log.trace("findPeers() response: {}", peerList.getPeersList().toString());

            responseObserver.onNext(peerList);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.debug("findPeers() is failed. {}", e.getMessage());
        }
    }

    @Override
    public void ping(Proto.Ping request, StreamObserver<Proto.Pong> responseObserver) {
        try {
            BranchId branchId = BranchId.of(request.getBranch().toByteArray());
            if (branchId.toString().equals("")) {
                return;
            }

            Peer from = Peer.valueOf(request.getFrom());
            Peer to = Peer.valueOf(request.getTo());
            String grpcHost = Objects.requireNonNull(grpcServerRunner.getServerCallCapture().get()
                    .getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR))
                    .toString().split(":")[0].replaceAll("/", "");

            from.setBestBlock(request.getBestBlock());
            to.setBestBlock(branchGroup != null ? branchGroup.getLastIndex(branchId) : 0L);
            Proto.Pong pong = discoveryConsumer.ping(
                    branchId, from, to, request.getPing(), to.getBestBlock(), from.getHost().equals(grpcHost));

            responseObserver.onNext(pong);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.debug("ping() is failed. {}", e.getMessage());
        }
    }
}
