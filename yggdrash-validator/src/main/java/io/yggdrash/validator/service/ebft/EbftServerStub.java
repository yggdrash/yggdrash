package io.yggdrash.validator.service.ebft;

import io.grpc.stub.StreamObserver;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.proto.EbftServiceGrpc;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.data.ebft.EbftBlockChain;
import io.yggdrash.validator.data.ebft.EbftStatus;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@GRpcService
@ConditionalOnProperty(name = "yggdrash.validator.consensus.algorithm", havingValue = "ebft")
public class EbftServerStub extends EbftServiceGrpc.EbftServiceImplBase {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(EbftServerStub.class);

    private final EbftBlockChain ebftBlockChain;
    private final EbftService ebftService; //todo: check security!

    @Autowired
    public EbftServerStub(EbftBlockChain ebftBlockChain, EbftService ebftService) {
        this.ebftBlockChain = ebftBlockChain;
        this.ebftService = ebftService;
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
    public void getNodeStatus(
            CommonProto.Chain request,
            StreamObserver<io.yggdrash.proto.EbftProto.EbftStatus> responseObserver) {
        EbftStatus newEbftStatus = ebftService.getMyNodeStatus();
        responseObserver.onNext(EbftStatus.toProto(newEbftStatus));
        responseObserver.onCompleted();
    }

    @Override
    public void exchangeNodeStatus(io.yggdrash.proto.EbftProto.EbftStatus request,
                                   io.grpc.stub.StreamObserver<io.yggdrash.proto.EbftProto.EbftStatus> responseObserver) {
        EbftStatus blockStatus = new EbftStatus(request);
        updateStatus(blockStatus);

        EbftStatus newEbftStatus = ebftService.getMyNodeStatus();
        responseObserver.onNext(EbftStatus.toProto(newEbftStatus));
        responseObserver.onCompleted();
    }

    @Override
    public void broadcastEbftBlock(io.yggdrash.proto.EbftProto.EbftBlock request,
                                   io.grpc.stub.StreamObserver<io.yggdrash.proto.NetProto.Empty> responseObserver) {
        EbftBlock newEbftBlock = new EbftBlock(request);
        if (!EbftBlock.verify(newEbftBlock) || !ebftService.consensusVerify(newEbftBlock)) {
            log.warn("broadcast EbftBlock Verify Fail");
            responseObserver.onNext(io.yggdrash.proto.NetProto.Empty.newBuilder().build());
            responseObserver.onCompleted();
            return;
        }

        EbftBlock lastEbftBlock = this.ebftBlockChain.getLastConfirmedBlock();

        responseObserver.onNext(io.yggdrash.proto.NetProto.Empty.newBuilder().build());
        responseObserver.onCompleted();

        if (lastEbftBlock.getIndex() == newEbftBlock.getIndex() - 1
                && Arrays.equals(lastEbftBlock.getHash(), newEbftBlock.getBlock().getPrevBlockHash())) {

            ebftService.getLock().lock();
            ebftService.updateUnconfirmedBlock(newEbftBlock);
            ebftService.getLock().unlock();
        }
    }

    @Override
    public void getEbftBlockList(io.yggdrash.proto.CommonProto.Offset request,
                                 io.grpc.stub.StreamObserver<EbftProto.EbftBlockList> responseObserver) {
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
            for (EbftBlock ebftBlock : ebftStatus.getUnConfirmedEbftBlockList()) {
                if (ebftBlock.getIndex()
                        <= this.ebftBlockChain.getLastConfirmedBlock().getIndex()) {
                    continue;
                }
                ebftService.getLock().lock();
                ebftService.updateUnconfirmedBlock(ebftBlock);
                ebftService.getLock().unlock();
            }
        }
    }

}
