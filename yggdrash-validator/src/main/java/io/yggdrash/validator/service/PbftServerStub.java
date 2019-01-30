package io.yggdrash.validator.service;

import io.grpc.stub.StreamObserver;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.PbftServiceGrpc;
import io.yggdrash.validator.data.PbftBlock;
import io.yggdrash.validator.data.PbftBlockChain;
import io.yggdrash.validator.data.PbftStatus;
import io.yggdrash.validator.data.pbft.PbftMessage;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public void pingPongTime(EbftProto.PingTime request,
                             StreamObserver<EbftProto.PongTime> responseObserver) {
        long timestamp = System.currentTimeMillis();
        EbftProto.PongTime pongTime
                = EbftProto.PongTime.newBuilder().setTimestamp(timestamp).build();
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

        log.trace("multicastPbftMessage");
        PbftMessage pbftMessage = new PbftMessage(request);

        if (!PbftMessage.verify(pbftMessage)) {
            log.warn("Verify Fail");
            responseObserver.onNext(EMPTY);
            responseObserver.onCompleted();
            return;
        }

        long lastIndex = this.blockChain.getLastConfirmedBlock().getIndex();
        if (pbftMessage.getSeqNumber() <= lastIndex) {
            responseObserver.onNext(EMPTY);
            responseObserver.onCompleted();
            return;
        }

        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();

        pbftService.getLock().lock();
        pbftService.updateUnconfirmedMsg(pbftMessage);
        pbftService.getLock().unlock();
    }

    @Override
    public void multicastPbftBlock(io.yggdrash.proto.PbftProto.PbftBlock request,
                                   io.grpc.stub.StreamObserver<io.yggdrash.proto.NetProto.Empty> responseObserver) {

        log.trace("multicastPbftBlock");
        PbftBlock newPbftBlock = new PbftBlock(request);

        if (!PbftBlock.verify(newPbftBlock)) {
            log.warn("Verify Fail");
            responseObserver.onNext(io.yggdrash.proto.NetProto.Empty.newBuilder().build());
            responseObserver.onCompleted();
            return;
        }

        PbftBlock lastPbftBlock = this.blockChain.getLastConfirmedBlock();

        responseObserver.onNext(io.yggdrash.proto.NetProto.Empty.newBuilder().build());
        responseObserver.onCompleted();

        if (lastPbftBlock.getIndex() == newPbftBlock.getIndex() - 1
                && Arrays.equals(lastPbftBlock.getHash(), newPbftBlock.getPrevBlockHash())) {

            pbftService.getLock().lock();
//            pbftService.updateUnconfirmedBlock(newPbftBlock);
            pbftService.getLock().unlock();
        }

    }


    @Override
    public void getPbftBlockList(io.yggdrash.proto.EbftProto.Offset request,
                                 io.grpc.stub.StreamObserver<PbftProto.PbftBlockList> responseObserver) {
        long start = request.getIndex();
        long count = request.getCount();
        List<PbftBlock> blockList = new ArrayList<>();

        long end = Math.min(start - 1 + count,
                this.blockChain.getLastConfirmedBlock().getIndex());

        log.trace("start: " + start);
        log.trace("end: " + end);

        if (start < end) {
            for (long l = start; l <= end; l++) {
                blockList.add(this.blockChain.getBlockStore().get(
                        this.blockChain.getBlockKeyStore().get(l)));
            }
        }

        responseObserver.onNext(PbftBlock.toProtoList(blockList));
        responseObserver.onCompleted();
    }

    private void updateStatus(PbftStatus status) {
        if (!PbftStatus.verify(status)) {
            log.trace("PbftStatus verify fail.");
            return;
        }

        if (status.getIndex()
                <= this.blockChain.getLastConfirmedBlock().getIndex()) {
            return;
        }

        pbftService.getLock().lock();
        pbftService.updateUnconfirmedMsgMap(status.getPbftMessageMap());
        pbftService.getLock().unlock();
    }
}
