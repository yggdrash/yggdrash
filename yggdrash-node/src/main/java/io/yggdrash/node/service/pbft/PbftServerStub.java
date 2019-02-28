package io.yggdrash.node.service.pbft;

import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.pbft.PbftBlock;
import io.yggdrash.core.blockchain.pbft.PbftMessage;
import io.yggdrash.core.blockchain.pbft.PbftStatus;
import io.yggdrash.node.springboot.grpc.GrpcService;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.PbftServiceGrpc;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@GrpcService
public class PbftServerStub extends PbftServiceGrpc.PbftServiceImplBase {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftServerStub.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();

    private final PbftBlockChain blockChain;
    private final PbftService pbftService; //todo: check security!

    @Autowired
    public PbftServerStub(PbftBlockChain pbftBlockChain, PbftService pbftService) {
        this.blockChain = pbftBlockChain;
        this.pbftService = pbftService;
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
        updateStatus(status);

        PbftStatus newStatus = pbftService.getMyNodeStatus();
        responseObserver.onNext(PbftStatus.toProto(newStatus));
        responseObserver.onCompleted();
    }


    @Override
    public void multicastPbftMessage(PbftProto.PbftMessage request,
                                     StreamObserver<NetProto.Empty> responseObserver) {

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
    public void multicastPbftBlock(PbftProto.PbftBlock request,
                                   StreamObserver<NetProto.Empty> responseObserver) {

        log.trace("multicastPbftBlock");
        PbftBlock newPbftBlock = new PbftBlock(request);

        if (!PbftBlock.verify(newPbftBlock)) {
            log.warn("Verify Fail");
            responseObserver.onNext(EMPTY);
            responseObserver.onCompleted();
            return;
        }

        PbftBlock lastPbftBlock = this.blockChain.getLastConfirmedBlock();

        responseObserver.onNext(NetProto.Empty.newBuilder().build());
        responseObserver.onCompleted();

        if (lastPbftBlock.getIndex() == newPbftBlock.getIndex() - 1
                && Arrays.equals(lastPbftBlock.getHash(), newPbftBlock.getPrevBlockHash())) {

            pbftService.getLock().lock();
            // todo: check block confirm
            //pbftService.updateUnconfirmedBlock(newPbftBlock);
            pbftService.getLock().unlock();
        }

    }


    @Override
    public void getPbftBlockList(CommonProto.Offset request,
                                 StreamObserver<PbftProto.PbftBlockList> responseObserver) {
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
        pbftService.updateUnconfirmedMsgMap(status.getUnConfirmedPbftMessageMap());
        pbftService.getLock().unlock();
    }
}
