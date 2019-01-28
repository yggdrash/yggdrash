package io.yggdrash.validator.service;

import io.grpc.stub.StreamObserver;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.PbftServiceGrpc;
import io.yggdrash.validator.data.PbftBlockChain;
import io.yggdrash.validator.data.PbftStatus;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@GRpcService
@ConditionalOnProperty(name = "yggdrash.validator.consensus.algorithm", havingValue = "pbft")
public class PbftServerStub extends PbftServiceGrpc.PbftServiceImplBase {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftServerStub.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();

    private final PbftBlockChain blockChain;
    private final PbftService pbftService; //todo: check security!

    @Autowired
    public PbftServerStub(PbftBlockChain blockChain, PbftService pbftService) {
        this.blockChain = blockChain;
        this.pbftService = pbftService;
    }

    @Override
    public void pingPongTime(PbftProto.PbftPingTime request,
                             StreamObserver<PbftProto.PbftPongTime> responseObserver) {
        long timestamp = System.currentTimeMillis();
        PbftProto.PbftPongTime pongTime
                = PbftProto.PbftPongTime.newBuilder().setTimestamp(timestamp).build();
        responseObserver.onNext(pongTime);
        responseObserver.onCompleted();
    }

    @Override
    public void exchangePbftStatus(io.yggdrash.proto.PbftProto.PbftStatus request,
                                   io.grpc.stub.StreamObserver<io.yggdrash.proto.PbftProto.PbftStatus> responseObserver) {
        PbftStatus status = new PbftStatus(request);
        updateStatus(status);

        PbftStatus newStatus = pbftService.getMyNodeStatus();
        responseObserver.onNext(PbftStatus.toProto(newStatus));
        responseObserver.onCompleted();
    }


    @Override
    public void multicastPbftMessage(io.yggdrash.proto.PbftProto.PbftMessage request,
                                     io.grpc.stub.StreamObserver<io.yggdrash.proto.NetProto.Empty> responseObserver) {

        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }

    private void updateStatus(PbftStatus status) {
        if (PbftStatus.verify(status)) {
//            for (PbftBlock block : status.getUnConfirmedBlockList()) {
//                if (block.getIndex()
//                        <= this.blockChain.getLastConfirmedBlock().getIndex()) {
//                    continue;
//                }
//                pbftService.getLock().lock();
//                pbftService.updateUnconfirmedBlock(blockCon);
//                pbftService.getLock().unlock();
//            }
        }
    }
}
