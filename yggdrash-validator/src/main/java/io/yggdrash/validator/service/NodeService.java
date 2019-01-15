package io.yggdrash.validator.service;

import io.grpc.stub.StreamObserver;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.Proto;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GRpcService
public class NodeService extends BlockChainGrpc.BlockChainImplBase {
    private static final Logger log = LoggerFactory.getLogger(NodeService.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();

    @Override
    public void syncBlock(NetProto.SyncLimit syncLimit,
                          StreamObserver<Proto.BlockList> responseObserver) {
        log.debug("NodeService syncBlock");
        responseObserver.onNext(Proto.BlockList.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void syncTransaction(NetProto.SyncLimit syncLimit,
                                StreamObserver<Proto.TransactionList> responseObserver) {
        log.debug("NodeService syncTransaction");

        responseObserver.onNext(Proto.TransactionList.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void broadcastBlock(Proto.Block request,
                               StreamObserver<NetProto.Empty> responseObserver) {
        log.debug("NodeService broadcastBlock");
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }

    @Override
    public void broadcastTransaction(Proto.Transaction request,
                                     StreamObserver<NetProto.Empty> responseObserver) {
        log.debug("NodeService broadcastTransaction");
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }
}
