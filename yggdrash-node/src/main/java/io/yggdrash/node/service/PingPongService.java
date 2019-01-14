package io.yggdrash.node.service;

import io.grpc.stub.StreamObserver;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerGroup;
import io.yggdrash.proto.PeerInfo;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import io.yggdrash.proto.Pong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingPongService extends PingPongGrpc.PingPongImplBase {
    private static final Logger log = LoggerFactory.getLogger(PingPongService.class);

    private final PeerGroup peerGroup;

    public PingPongService(PeerGroup peerGroup) {
        this.peerGroup = peerGroup;
    }

    @Override
    public void play(Ping request, StreamObserver<Pong> responseObserver) {
        PeerInfo peerInfo = request.getPeer();
        Peer peer = Peer.valueOf(peerInfo.getPubKey(), peerInfo.getIp(), peerInfo.getPort());
        peerGroup.touchPeer(peer);

        log.debug("Received " + request.getPing());
        Pong pong = Pong.newBuilder().setPong("Pong").build();
        responseObserver.onNext(pong);
        responseObserver.onCompleted();
    }

}
