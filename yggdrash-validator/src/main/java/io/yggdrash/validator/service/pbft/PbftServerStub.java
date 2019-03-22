package io.yggdrash.validator.service.pbft;

import io.grpc.stub.StreamObserver;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.PbftServiceGrpc;
import io.yggdrash.validator.data.ConsensusBlockChain;
import io.yggdrash.validator.data.pbft.PbftBlock;
import io.yggdrash.validator.data.pbft.PbftBlockChain;
import io.yggdrash.validator.data.pbft.PbftMessage;
import io.yggdrash.validator.data.pbft.PbftStatus;
import io.yggdrash.validator.service.ConsensusService;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PbftServerStub extends PbftServiceGrpc.PbftServiceImplBase {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftServerStub.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();

    private final PbftBlockChain blockChain;
    private final PbftService pbftService; //todo: check security!

    public PbftServerStub(ConsensusBlockChain blockChain, ConsensusService consensusService) {
        this.blockChain = (PbftBlockChain) blockChain;
        this.pbftService = (PbftService) consensusService;
    }

    @Override
    public void pingPongTime(CommonProto.PingTime request,
                             StreamObserver<CommonProto.PongTime> responseObserver) {
        long timestamp = System.currentTimeMillis();
        CommonProto.PongTime pongTime
                = CommonProto.PongTime.newBuilder().setTimestamp(timestamp).build();
        responseObserver.onNext(pongTime);
        responseObserver.onCompleted();
    }

    @Override
    public void exchangePbftStatus(PbftProto.PbftStatus request,
                                   StreamObserver<PbftProto.PbftStatus> responseObserver) {
        PbftStatus status = new PbftStatus(request);
        PbftStatus newStatus = pbftService.getMyNodeStatus();
        try {
            updateStatus(status);

            responseObserver.onNext(PbftStatus.toProto(newStatus));
            responseObserver.onCompleted();
        } finally {
            status.clear();
            newStatus.clear();
        }
    }

    @Override
    public void multicastPbftMessage(PbftProto.PbftMessage request,
                                     StreamObserver<NetProto.Empty> responseObserver) {
        log.trace("multicastPbftMessage");
        PbftMessage pbftMessage = new PbftMessage(request);
        try {

            if (!PbftMessage.verify(pbftMessage)) {
                log.warn("Verify Fail");
                pbftMessage.clear();
                responseObserver.onNext(EMPTY);
                responseObserver.onCompleted();
                return;
            }

            long lastIndex = this.blockChain.getLastConfirmedBlock().getIndex();
            if (pbftMessage.getSeqNumber() <= lastIndex) {
                pbftMessage.clear();
                responseObserver.onNext(EMPTY);
                responseObserver.onCompleted();
                return;
            }

            responseObserver.onNext(EMPTY);
            responseObserver.onCompleted();

            pbftService.getLock().lock();
            pbftService.updateUnconfirmedMsg(pbftMessage);
            pbftService.getLock().unlock();
        } finally {
            pbftMessage.clear();
        }
    }

    @Override
    public void getPbftBlockList(io.yggdrash.proto.CommonProto.Offset request,
                                 io.grpc.stub.StreamObserver<PbftProto.PbftBlockList> responseObserver) {
        long start = request.getIndex();
        long count = request.getCount();
        List<PbftBlock> blockList = new ArrayList<>();

        try {
            long end = Math.min(start - 1 + count,
                    this.blockChain.getLastConfirmedBlock().getIndex());

            log.trace("start: " + start);
            log.trace("end: " + end);

            if (start < end) {
                for (long l = start; l <= end; l++) {
                    byte[] key = this.blockChain.getBlockKeyStore().get(l);
                    blockList.add(this.blockChain.getBlockStore().get(key));
                }
            }

            responseObserver.onNext(PbftBlock.toProtoList(blockList));
            responseObserver.onCompleted();
        } finally {
            for (PbftBlock pbftBlock : blockList) {
                pbftBlock.clear();
            }
            blockList.clear();
        }
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
        pbftService.updateUnconfirmedMsgMap(status.getUnConfirmedPbftMessageMap());
        pbftService.getLock().unlock();
    }
}
