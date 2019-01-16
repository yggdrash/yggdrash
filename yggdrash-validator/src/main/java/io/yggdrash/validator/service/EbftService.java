package io.yggdrash.validator.service;

import io.grpc.stub.StreamObserver;
import io.yggdrash.proto.ConsensusEbftGrpc;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.validator.GrpcNodeServer;
import io.yggdrash.validator.data.BlockCon;
import io.yggdrash.validator.data.BlockConChain;
import io.yggdrash.validator.data.NodeStatus;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@GRpcService
public class EbftService extends ConsensusEbftGrpc.ConsensusEbftImplBase {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(EbftService.class);

    private final BlockConChain blockConChain;
    private final GrpcNodeServer grpcNodeServer; //todo: check security!

    @Autowired
    public EbftService(BlockConChain blockConChain, GrpcNodeServer grpcNodeServer) {
        this.blockConChain = blockConChain;
        this.grpcNodeServer = grpcNodeServer;
    }

    @Override
    public void pingPongTime(EbftProto.PingTime request,
                             StreamObserver<EbftProto.PongTime> responseObserver) {
        long timestamp = System.currentTimeMillis();
        EbftProto.PongTime pongTime
                = EbftProto.PongTime.newBuilder().setTimestamp(timestamp).build();
        responseObserver.onNext(pongTime);
        responseObserver.onCompleted();
    }

    @Override
    public void getNodeStatus(
            EbftProto.Chain request,
            StreamObserver<io.yggdrash.proto.EbftProto.NodeStatus> responseObserver) {
        NodeStatus newNodeStatus = grpcNodeServer.getMyNodeStatus();
        responseObserver.onNext(NodeStatus.toProto(newNodeStatus));
        responseObserver.onCompleted();
    }

    @Override
    public void exchangeNodeStatus(io.yggdrash.proto.EbftProto.NodeStatus request,
            io.grpc.stub.StreamObserver<io.yggdrash.proto.EbftProto.NodeStatus> responseObserver) {
        NodeStatus blockStatus = new NodeStatus(request);
        updateStatus(blockStatus);

        NodeStatus newNodeStatus = grpcNodeServer.getMyNodeStatus();
        responseObserver.onNext(NodeStatus.toProto(newNodeStatus));
        responseObserver.onCompleted();
    }

    @Override
    public void broadcastBlockCon(io.yggdrash.proto.EbftProto.BlockCon request,
              io.grpc.stub.StreamObserver<io.yggdrash.proto.NetProto.Empty> responseObserver) {
        BlockCon newBlockCon = new BlockCon(request);
        if (!BlockCon.verify(newBlockCon) || !grpcNodeServer.consensusVerify(newBlockCon)) {
            log.error("Verify Fail");
            return;
        }

        BlockCon lastBlockCon = this.blockConChain.getLastConfirmedBlockCon();

        responseObserver.onNext(io.yggdrash.proto.NetProto.Empty.newBuilder().build());
        responseObserver.onCompleted();

        if (lastBlockCon.getIndex() == newBlockCon.getIndex() - 1
                && Arrays.equals(lastBlockCon.getHash(), newBlockCon.getPrevBlockHash())) {

            grpcNodeServer.getLock().lock();
            grpcNodeServer.updateUnconfirmedBlock(newBlockCon);
            grpcNodeServer.getLock().unlock();
        }
    }

    @Override
    public void getBlockConList(io.yggdrash.proto.EbftProto.Offset request,
                    io.grpc.stub.StreamObserver<EbftProto.BlockConList> responseObserver) {
        long start = request.getIndex();
        long count = request.getCount();
        List<BlockCon> blockConList = new ArrayList<>();

        long end = Math.min(start - 1 + count,
                this.blockConChain.getLastConfirmedBlockCon().getIndex());

        log.debug("start: " + start);
        log.debug("end: " + end);

        if (start < end) {
            for (long l = start; l <= end; l++) {
                blockConList.add(this.blockConChain.getBlockConStore().get(
                        this.blockConChain.getBlockConKeyStore().get(l)));
            }
        }

        responseObserver.onNext(BlockCon.toProtoList(blockConList));
        responseObserver.onCompleted();
    }

    private void updateStatus(NodeStatus nodeStatus) {
        if (NodeStatus.verify(nodeStatus)) {
            for (BlockCon blockCon : nodeStatus.getUnConfirmedBlockConList()) {
                if (blockCon.getIndex()
                        <= this.blockConChain.getLastConfirmedBlockCon().getIndex()) {
                    continue;
                }
                grpcNodeServer.getLock().lock();
                grpcNodeServer.updateUnconfirmedBlock(blockCon);
                grpcNodeServer.getLock().unlock();
            }
        }
    }

}
