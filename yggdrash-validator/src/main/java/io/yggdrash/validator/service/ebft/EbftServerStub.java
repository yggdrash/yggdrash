package io.yggdrash.validator.service.ebft;

import io.grpc.stub.StreamObserver;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.proto.EbftServiceGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.validator.data.ConsensusBlockChain;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.data.ebft.EbftBlockChain;
import io.yggdrash.validator.data.ebft.EbftStatus;
import io.yggdrash.validator.service.ConsensusService;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EbftServerStub extends EbftServiceGrpc.EbftServiceImplBase {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(EbftServerStub.class);

    private final EbftBlockChain ebftBlockChain;
    private final EbftService ebftService; //todo: check security!

    public EbftServerStub(ConsensusBlockChain blockChain, ConsensusService service) {
        this.ebftBlockChain = (EbftBlockChain) blockChain;
        this.ebftService = (EbftService) service;
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
    public void exchangeEbftStatus(EbftProto.EbftStatus request,
                                   StreamObserver<EbftProto.EbftStatus> responseObserver) {
        EbftStatus blockStatus = new EbftStatus(request);
        updateStatus(blockStatus);

        EbftStatus newEbftStatus = ebftService.getMyNodeStatus();
        responseObserver.onNext(EbftStatus.toProto(newEbftStatus));
        responseObserver.onCompleted();
    }

    @Override
    public void multicastEbftBlock(EbftProto.EbftBlock request,
                                   StreamObserver<NetProto.Empty> responseObserver) {
        EbftBlock newEbftBlock = new EbftBlock(request);
        if (!EbftBlock.verify(newEbftBlock) || !ebftService.consensusVerify(newEbftBlock)) {
            log.warn("broadcast EbftBlock Verify Fail");
            responseObserver.onNext(NetProto.Empty.newBuilder().build());
            responseObserver.onCompleted();
            return;
        }

        responseObserver.onNext(NetProto.Empty.newBuilder().build());
        responseObserver.onCompleted();

        EbftBlock lastEbftBlock = this.ebftBlockChain.getLastConfirmedBlock();

        ebftService.getLock().lock();
        if (newEbftBlock.getIndex() == lastEbftBlock.getIndex() + 1
                && Arrays.equals(lastEbftBlock.getHash(),
                newEbftBlock.getBlock().getPrevBlockHash())) {
            ebftService.updateUnconfirmedBlock(newEbftBlock);
        }
        ebftService.getLock().unlock();
    }

    @Override
    public void getEbftBlockList(CommonProto.Offset request,
                                 StreamObserver<EbftProto.EbftBlockList> responseObserver) {
        long start = request.getIndex();
        long count = request.getCount();
        List<EbftBlock> ebftBlockList = new ArrayList<>();

        long end = Math.min(start - 1 + count,
                this.ebftBlockChain.getLastConfirmedBlock().getIndex());

        log.trace("start: " + start);
        log.trace("end: " + end);

        if (start < end) {
            for (long l = start; l <= end; l++) {
                ebftBlockList.add(this.ebftBlockChain.getBlockStore().get(
                        this.ebftBlockChain.getBlockKeyStore().get(l)));
            }
        }

        responseObserver.onNext(EbftBlock.toProtoList(ebftBlockList));
        responseObserver.onCompleted();
    }

    private void updateStatus(EbftStatus ebftStatus) {
        if (EbftStatus.verify(ebftStatus)) {
            ebftService.getLock().lock();
            for (EbftBlock ebftBlock : ebftStatus.getUnConfirmedEbftBlockList()) {
                if (ebftBlock.getIndex()
                        > this.ebftBlockChain.getLastConfirmedBlock().getIndex()) {
                    ebftService.updateUnconfirmedBlock(ebftBlock);
                }
            }
            ebftService.getLock().unlock();
        }
    }

}
