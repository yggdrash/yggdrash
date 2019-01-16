package io.yggdrash.node.service;

import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.net.BestBlock;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.node.GRpcPeerHandler;
import io.yggdrash.proto.PeerGrpc;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PeerService extends PeerGrpc.PeerImplBase {
    private static final Logger log = LoggerFactory.getLogger(PeerService.class);

    private final PeerGroup peerGroup;

    public PeerService(PeerGroup peerGroup) {
        this.peerGroup = peerGroup;
    }

    @Override
    public void findPeers(Proto.RequestPeer request, StreamObserver<Proto.PeerList> responseObserver) {
        Peer peer = Peer.valueOf(request.getPubKey(), request.getIp(), request.getPort());

        for (Proto.BestBlock bestBlock : request.getBestBlocksList()) {
            BestBlock bb = toBestBlock(bestBlock);
            peer.updateBestBlock(bb);
        }
        List<String> list = peerGroup.getPeers(peer);
        Proto.PeerList.Builder peerListBuilder = Proto.PeerList.newBuilder();
        for (String url : list) {
            peerListBuilder.addPeers(Proto.PeerInfo.newBuilder().setUrl(url).build());
        }
        Proto.PeerList peerList = peerListBuilder.build();
        responseObserver.onNext(peerList);
        responseObserver.onCompleted();

        // TODO remove cross connection
        try {
            if (!peerGroup.isMaxHandler()) {
                peerGroup.addHandler(peer);
            } else {
                // maxPeer 를 넘은경우부터 거리 계산된 peerTable 을 기반으로 peerChannel 업데이트
                if (peerGroup.isClosePeer(peer)) {
                    log.warn("channel is max");
                    // TODO apply after test
                    //peerGroup.reloadPeerChannel(new GRpcPeerHandler(peer));
                }
            }
        } catch (Exception e) {
            log.debug("Failed to connect {} -> {}", peerGroup.getOwner().toAddress(),
                    peer.toAddress());
        }
    }

    @Override
    public void play(Proto.Ping request, StreamObserver<Proto.Pong> responseObserver) {
        String url = request.getPeer().getUrl();
        Peer peer = Peer.valueOf(url);
        peerGroup.touchPeer(peer);
        log.debug("Received " + request.getPing());
        Proto.Pong pong = Proto.Pong.newBuilder().setPong("Pong").build();
        responseObserver.onNext(pong);
        responseObserver.onCompleted();
    }

    private BestBlock toBestBlock(Proto.BestBlock bestBlock) {
        BranchId branchId = BranchId.of(bestBlock.getBranch().toByteArray());
        long index = bestBlock.getIndex();
        return BestBlock.of(branchId, index);
    }
}