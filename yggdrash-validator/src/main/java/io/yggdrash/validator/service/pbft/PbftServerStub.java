
package io.yggdrash.validator.service.pbft;

import io.grpc.stub.StreamObserver;
import io.yggdrash.common.config.Constants;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.consensus.ConsensusBlockChain;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.PbftServiceGrpc;
import io.yggdrash.validator.data.pbft.PbftBlock;
import io.yggdrash.validator.data.pbft.PbftMessage;
import io.yggdrash.validator.data.pbft.PbftStatus;
import io.yggdrash.validator.data.pbft.PbftVerifier;
import org.slf4j.LoggerFactory;

public class PbftServerStub extends PbftServiceGrpc.PbftServiceImplBase {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftServerStub.class);
    private static final CommonProto.Empty EMPTY = CommonProto.Empty.getDefaultInstance();

    private final ConsensusBlockChain<PbftProto.PbftBlock, PbftMessage> blockChain;
    private final PbftService pbftService; //todo: check security!

    public PbftServerStub(PbftService consensusService) {
        this.blockChain = consensusService.getBlockChain();
        this.pbftService = consensusService;
    }

    @Override
    public void pingPongTime(CommonProto.PingTime request, StreamObserver<CommonProto.PongTime> responseObserver) {
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
                                     StreamObserver<CommonProto.Empty> responseObserver) {
        log.trace("multicastPbftMessage");
        PbftMessage pbftMessage = new PbftMessage(request);
        try {

            if (!PbftVerifier.INSTANCE.verify(pbftMessage)) {
                log.warn("Verify Fail");
                pbftMessage.clear();
                responseObserver.onNext(EMPTY);
                responseObserver.onCompleted();
                return;
            }

            long lastIndex = this.blockChain.getBlockChainManager().getLastIndex();
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
    public void broadcastPbftBlock(PbftProto.PbftBlock request,
                                   StreamObserver<CommonProto.Empty> responseObserver) {
        PbftBlock newPbftBlock = new PbftBlock(request);
        try {
            log.debug("Received BroadcastPbftBlock [{}] {} ", newPbftBlock.getIndex(), newPbftBlock.getHash());
            if (!PbftVerifier.INSTANCE.verify(newPbftBlock)) {
                log.warn("Verify Fail");
                responseObserver.onNext(EMPTY);
                responseObserver.onCompleted();
                return;
            }

            responseObserver.onNext(CommonProto.Empty.newBuilder().build());
            responseObserver.onCompleted();

            pbftService.getLock().lock();
            ConsensusBlock<PbftProto.PbftBlock> lastPbftBlock
                    = this.blockChain.getBlockChainManager().getLastConfirmedBlock();
            if (lastPbftBlock.getIndex() == newPbftBlock.getIndex() - 1
                    && lastPbftBlock.getHash().equals(newPbftBlock.getPrevBlockHash())) {
                this.pbftService.confirmedBlock(newPbftBlock);
            }
            pbftService.getLock().unlock();
        } finally {
            newPbftBlock.clear();
        }
    }

    @Override
    public void getPbftBlockList(CommonProto.Offset request, StreamObserver<PbftProto.PbftBlockList> responseObserver) {
        long start = request.getIndex();
        if (start < 0) {
            start = 0;
        }
        long count = request.getCount();
        long end = Math.min(start - 1 + count, blockChain.getBlockChainManager().getLastIndex());

        log.trace("start: {}", start);
        log.trace("end: {}", end);

        responseObserver.onNext(getBlockList(start, end));
        responseObserver.onCompleted();
    }

    private PbftProto.PbftBlockList getBlockList(long start, long end) {
        PbftProto.PbftBlockList.Builder builder = PbftProto.PbftBlockList.newBuilder();
        if (start >= end) {
            return builder.build();
        }
        long bodyLengthSum = 0;
        for (long l = start; l <= end; l++) {
            try {
                // todo: check efficiency
                ConsensusBlock<PbftProto.PbftBlock> block = blockChain.getBlockChainManager().getBlockByIndex(l);
                bodyLengthSum += block.getSerializedSize();
                if (bodyLengthSum > Constants.Limit.BLOCK_SYNC_SIZE) {
                    return builder.build();
                }
                builder.addPbftBlock(block.getInstance());
            } catch (Exception e) {
                break;
            }
        }
        return builder.build();
    }

    private void updateStatus(PbftStatus status) {
        if (!PbftStatus.verify(status)) {
            log.trace("PbftStatus verify fail.");
            return;
        }

        if (status.getIndex()
                <= this.blockChain.getBlockChainManager().getLastIndex()) {
            return;
        }

        pbftService.getLock().lock();
        pbftService.updateUnconfirmedMsgMap(status.getUnConfirmedPbftMessageMap());
        pbftService.getLock().unlock();
    }
}
