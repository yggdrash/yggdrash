package io.yggdrash.validator.service;

import io.grpc.stub.StreamObserver;
import io.yggdrash.proto.ConsensusEbftGrpc;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.validator.data.EbftBlock;
import io.yggdrash.validator.data.EbftBlockChain;
import io.yggdrash.validator.data.NodeStatus;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@GRpcService
@ConditionalOnProperty(name = "yggdrash.validator.consensus.algorithm", havingValue = "ebft")
public class EbftServerStub extends ConsensusEbftGrpc.ConsensusEbftImplBase {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(EbftServerStub.class);

    private final EbftBlockChain ebftBlockChain;
    private final EbftService ebftService; //todo: check security!

    @Autowired
    public EbftServerStub(EbftBlockChain ebftBlockChain, EbftService ebftService) {
        this.ebftBlockChain = ebftBlockChain;
        this.ebftService = ebftService;
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
    public void getNodeStatus(
            EbftProto.Chain request,
            StreamObserver<io.yggdrash.proto.EbftProto.NodeStatus> responseObserver) {
        NodeStatus newNodeStatus = ebftService.getMyNodeStatus();
        responseObserver.onNext(NodeStatus.toProto(newNodeStatus));
        responseObserver.onCompleted();
    }

    @Override
    public void exchangeNodeStatus(io.yggdrash.proto.EbftProto.NodeStatus request,
            io.grpc.stub.StreamObserver<io.yggdrash.proto.EbftProto.NodeStatus> responseObserver) {
        NodeStatus blockStatus = new NodeStatus(request);
        updateStatus(blockStatus);

        NodeStatus newNodeStatus = ebftService.getMyNodeStatus();
        responseObserver.onNext(NodeStatus.toProto(newNodeStatus));
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

        EbftBlock lastEbftBlock = this.ebftBlockChain.getLastConfirmedEbftBlock();

        responseObserver.onNext(io.yggdrash.proto.NetProto.Empty.newBuilder().build());
        responseObserver.onCompleted();

        if (lastEbftBlock.getIndex() == newEbftBlock.getIndex() - 1
                && Arrays.equals(lastEbftBlock.getHash(), newEbftBlock.getPrevBlockHash())) {

            ebftService.getLock().lock();
            ebftService.updateUnconfirmedBlock(newEbftBlock);
            ebftService.getLock().unlock();
        }
    }

    @Override
    public void getEbftBlockList(io.yggdrash.proto.EbftProto.Offset request,
                                 io.grpc.stub.StreamObserver<EbftProto.EbftBlockList> responseObserver) {
        long start = request.getIndex();
        long count = request.getCount();
        List<EbftBlock> ebftBlockList = new ArrayList<>();

        long end = Math.min(start - 1 + count,
                this.ebftBlockChain.getLastConfirmedEbftBlock().getIndex());

        log.trace("start: " + start);
        log.trace("end: " + end);

        if (start < end) {
            for (long l = start; l <= end; l++) {
                ebftBlockList.add(this.ebftBlockChain.getEbftBlockStore().get(
                        this.ebftBlockChain.getEbftBlockKeyStore().get(l)));
            }
        }

        responseObserver.onNext(EbftBlock.toProtoList(ebftBlockList));
        responseObserver.onCompleted();
    }

    private void updateStatus(NodeStatus nodeStatus) {
        if (NodeStatus.verify(nodeStatus)) {
            for (EbftBlock ebftBlock : nodeStatus.getUnConfirmedEbftBlockList()) {
                if (ebftBlock.getIndex()
                        <= this.ebftBlockChain.getLastConfirmedEbftBlock().getIndex()) {
                    continue;
                }
                ebftService.getLock().lock();
                ebftService.updateUnconfirmedBlock(ebftBlock);
                ebftService.getLock().unlock();
            }
        }
    }

}
