package io.yggdrash.validator.service.node;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.consensus.ConsensusBlockChain;
import io.yggdrash.core.consensus.ConsensusService;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.DiscoveryServiceGrpc;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscoveryServiceStub extends DiscoveryServiceGrpc.DiscoveryServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(DiscoveryServiceStub.class);
    private static final CommonProto.Empty EMPTY = CommonProto.Empty.getDefaultInstance();

    private final ConsensusBlockChain blockChain;
    private final ConsensusService consensusService;

    public DiscoveryServiceStub(ConsensusBlockChain blockChain, ConsensusService consensusService) {
        this.blockChain = blockChain;
        this.consensusService = consensusService;
    }

    @Override
    public void ping(Proto.Ping request, StreamObserver<Proto.Pong> responseObserver) {
        BranchId branchId = BranchId.of(request.getBranch().toByteArray());
        if (branchId == null || branchId.toString().equals("")) {
            return;
        }

        Peer from = Peer.valueOf(request.getFrom());
        from.setBestBlock(request.getBestBlock());
        Peer to = Peer.valueOf(request.getTo());
        Proto.Pong pong = Proto.Pong.newBuilder()
                .setPong("Pong")
                .setFrom(to.toString())
                .setTo(from.toString())
                .setBranch(ByteString.copyFrom(branchId.getBytes()))
                .setBestBlock(blockChain.getBlockChainManager().getLastIndex())
                .build();
        responseObserver.onNext(pong);
        responseObserver.onCompleted();
    }
}
