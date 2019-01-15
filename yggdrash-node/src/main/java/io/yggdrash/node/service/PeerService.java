package io.yggdrash.node.service;

import io.grpc.stub.StreamObserver;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.node.GRpcClientChannel;
import io.yggdrash.proto.NodeInfo;
import io.yggdrash.proto.PeerGrpc;
import io.yggdrash.proto.PeerList;
import io.yggdrash.proto.RequestPeer;
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
    public void findPeers(RequestPeer request, StreamObserver<PeerList> responseObserver) {
        Peer peer = Peer.valueOf(request.getPubKey(), request.getIp(), request.getPort());
        List<String> list = peerGroup.getPeers(peer);
        PeerList.Builder peerListBuilder = PeerList.newBuilder();
        for (String url : list) {
            peerListBuilder.addNodes(NodeInfo.newBuilder().setUrl(url).build());
        }
        PeerList peerList = peerListBuilder.build();
        responseObserver.onNext(peerList);
        responseObserver.onCompleted();

        try {
            if (!peerGroup.isMaxChannel()) {
                peerGroup.addChannel(new GRpcClientChannel(peer));
            } else {
                // maxPeer 를 넘은경우부터 거리 계산된 peerTable 을 기반으로 peerChannel 업데이트
                if (peerGroup.isClosePeer(peer)) {
                    log.warn("channel is max");
                    // TODO apply after test
                    //peerGroup.reloadPeerChannel(new GRpcClientChannel(peer));
                }
            }
        } catch (Exception e) {
            log.debug("Failed to connect {} -> {}", peerGroup.getOwner().toAddress(),
                    peer.toAddress());
        }
    }
}