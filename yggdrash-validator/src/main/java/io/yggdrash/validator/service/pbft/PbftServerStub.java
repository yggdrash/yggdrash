package io.yggdrash.validator.service.pbft;

import io.grpc.stub.StreamObserver;
import io.yggdrash.core.consensus.Block;
import io.yggdrash.core.consensus.ConsensusBlockChain;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.PbftServiceGrpc;
import io.yggdrash.validator.data.pbft.PbftMessage;
import io.yggdrash.validator.data.pbft.PbftStatus;
import org.slf4j.LoggerFactory;

public class PbftServerStub extends PbftServiceGrpc.PbftServiceImplBase {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftServerStub.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();

    private final ConsensusBlockChain<PbftProto.PbftBlock, PbftMessage> blockChain;
    private final PbftService pbftService; //todo: check security!

    public PbftServerStub(PbftService consensusService) {
        this.blockChain = consensusService.getBlockChain();
        this.pbftService = consensusService;
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
        long end = Math.min(start - 1 + count, this.blockChain.getLastConfirmedBlock().getIndex());

        log.trace("start: {}", start);
        log.trace("end: {}", end);

        PbftProto.PbftBlockList.Builder builder = PbftProto.PbftBlockList.newBuilder();
        if (start < end) {
            for (long l = start; l <= end; l++) {
                try {
                    Block<PbftProto.PbftBlock> block = blockChain.getBlockStore().getBlockByIndex(l);
                    builder.addPbftBlock(block.getInstance());
                } catch (Exception e) {
                    break;
                }
            }
        }

        responseObserver.onNext(builder.build());
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
        pbftService.updateUnconfirmedMsgMap(status.getUnConfirmedPbftMessageMap());
        pbftService.getLock().unlock();
    }
}
