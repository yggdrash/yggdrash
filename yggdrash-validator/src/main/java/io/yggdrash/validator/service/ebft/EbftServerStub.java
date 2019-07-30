package io.yggdrash.validator.service.ebft;

import io.grpc.stub.StreamObserver;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.util.VerifierUtils;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionImpl;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.consensus.ConsensusBlockChain;
import io.yggdrash.core.exception.errorcode.BusinessError;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.proto.EbftServiceGrpc;
import io.yggdrash.proto.Proto;
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
    public void multicastTransaction(Proto.Transaction protoTx,
                                     StreamObserver<CommonProto.Empty> responseObserver) {
        Transaction tx = new TransactionImpl(protoTx);
        log.debug("Received transaction: hash={}, {}", tx.getHash(), this);
        if (tx.getBranchId().equals(blockChain.getBranchId())
                && blockChain.getBlockChainManager().verify(tx) == BusinessError.VALID.toValue()) {
            blockChain.getBlockChainManager().addTransaction(tx);
        }
        responseObserver.onNext(EMPTY);
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
        if (!VerifierUtils.verify(newEbftBlock) || !ebftService.consensusVerify(newEbftBlock)) {
            log.warn("multicastEbftBlock Verify Fail");
            responseObserver.onNext(EMPTY);
            responseObserver.onCompleted();
            return;
        }

        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();

        ConsensusBlock<EbftProto.EbftBlock> lastEbftBlock = blockChain.getBlockChainManager().getLastConfirmedBlock();

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

            ConsensusBlock<EbftProto.EbftBlock> lastEbftBlock
                    = this.blockChain.getBlockChainManager().getLastConfirmedBlock();

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

    private EbftProto.EbftBlockList getBlockList(long start, long end) {
        EbftProto.EbftBlockList.Builder builder = EbftProto.EbftBlockList.newBuilder();
        if (start >= end) {
            return builder.build();
        }
        long bodyLengthSum = 0;
        for (long l = start; l <= end; l++) {
            try {
                // todo: check efficiency
                ConsensusBlock<EbftProto.EbftBlock> block = blockChain.getBlockChainManager().getBlockByIndex(l);
                bodyLengthSum += block.getSerializedSize();
                if (bodyLengthSum > Constants.Limit.BLOCK_SYNC_SIZE) {
                    return builder.build();
                }
                builder.addEbftBlock(block.getInstance());
            } catch (Exception e) {
                break;
            }
        }
        return builder.build();
    }

    private void updateStatus(EbftStatus ebftStatus) {
        if (EbftStatus.verify(ebftStatus)) {
            ebftService.getLock().lock();
            for (EbftBlock ebftBlock : ebftStatus.getUnConfirmedEbftBlockList()) {
                if (ebftBlock.getIndex() > blockChain.getBlockChainManager().getLastIndex()) {
                    ebftService.updateUnconfirmedBlock(ebftBlock);
                }
            }
            ebftService.getLock().unlock();
        }
    }

}
