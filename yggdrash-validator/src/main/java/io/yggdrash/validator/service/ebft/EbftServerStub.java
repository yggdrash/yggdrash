package io.yggdrash.validator.service.ebft;

import io.grpc.stub.StreamObserver;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.consensus.ConsensusBlockChain;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.proto.EbftServiceGrpc;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.data.ebft.EbftStatus;
import org.slf4j.LoggerFactory;

public class EbftServerStub extends EbftServiceGrpc.EbftServiceImplBase {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(EbftServerStub.class);
    private static final CommonProto.Empty EMPTY = CommonProto.Empty.getDefaultInstance();

    private final ConsensusBlockChain<EbftProto.EbftBlock, EbftBlock> blockChain;
    private final EbftService ebftService; //todo: check security!

    public EbftServerStub(EbftService consensusService) {
        this.blockChain = consensusService.getBlockChain();
        this.ebftService = consensusService;
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
    public void exchangeEbftStatus(EbftProto.EbftStatus request,
                                   StreamObserver<EbftProto.EbftStatus> responseObserver) {
        EbftStatus blockStatus = new EbftStatus(request);
        updateStatus(blockStatus);

        EbftStatus newEbftStatus = ebftService.getMyNodeStatus();
        responseObserver.onNext(EbftStatus.toProto(newEbftStatus));
        responseObserver.onCompleted();
    }

    @Override
    public void multicastEbftBlock(EbftProto.EbftBlock request, StreamObserver<CommonProto.Empty> responseObserver) {
        EbftBlock newEbftBlock = new EbftBlock(request);
        if (!newEbftBlock.verify() || !ebftService.consensusVerify(newEbftBlock)) {
            log.warn("multicastEbftBlock Verify Fail");
            responseObserver.onNext(EMPTY);
            responseObserver.onCompleted();
            return;
        }

        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();

        ConsensusBlock<EbftProto.EbftBlock> lastEbftBlock = blockChain.getLastConfirmedBlock();

        ebftService.getLock().lock();
        if (newEbftBlock.getIndex() == lastEbftBlock.getIndex() + 1
                && lastEbftBlock.getHash().equals(newEbftBlock.getPrevBlockHash())) {
            ebftService.updateUnconfirmedBlock(newEbftBlock);
        }
        ebftService.getLock().unlock();

    }

    @Override
    public void broadcastEbftBlock(EbftProto.EbftBlock request,
                                   StreamObserver<CommonProto.Empty> responseObserver) {
        EbftBlock newEbftBlock = new EbftBlock(request);
        try {
            if (!EbftBlock.verify(newEbftBlock) || !ebftService.consensusVerify(newEbftBlock)) {
                log.warn("broadcastEbftBlock Verify Fail");
                responseObserver.onNext(EMPTY);
                responseObserver.onCompleted();
                return;
            }

            responseObserver.onNext(EMPTY);
            responseObserver.onCompleted();

            ConsensusBlock<EbftProto.EbftBlock> lastEbftBlock = this.blockChain.getLastConfirmedBlock();

            ebftService.getLock().lock();
            if (newEbftBlock.getIndex() == lastEbftBlock.getIndex() + 1
                    && lastEbftBlock.getHash().equals(newEbftBlock.getPrevBlockHash())) {
                this.blockChain.addBlock(newEbftBlock);
            }
            ebftService.getLock().unlock();
        } finally {
            newEbftBlock.clear();
        }
    }

    @Override
    public void getEbftBlockList(CommonProto.Offset request, StreamObserver<EbftProto.EbftBlockList> responseObserver) {
        long start = request.getIndex();
        long count = request.getCount();
        long end = Math.min(start - 1 + count, blockChain.getLastConfirmedBlock().getIndex());

        log.trace("start: {}", start);
        log.trace("end: {}", end);

        EbftProto.EbftBlockList.Builder builder = EbftProto.EbftBlockList.newBuilder();
        if (start < end) {
            for (long l = start; l <= end; l++) {
                try {
                    ConsensusBlock<EbftProto.EbftBlock> block =
                            blockChain.getBlockStore()
                                    .get(Sha3Hash.createByHashed((byte[]) blockChain.getBlockKeyStore().get(l)));
                    builder.addEbftBlock(block.getInstance());
                } catch (Exception e) {
                    break;
                }
            }
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private void updateStatus(EbftStatus ebftStatus) {
        if (EbftStatus.verify(ebftStatus)) {
            ebftService.getLock().lock();
            for (EbftBlock ebftBlock : ebftStatus.getUnConfirmedEbftBlockList()) {
                if (ebftBlock.getIndex() > blockChain.getLastConfirmedBlock().getIndex()) {
                    ebftService.updateUnconfirmedBlock(ebftBlock);
                }
            }
            ebftService.getLock().unlock();
        }
    }

}
