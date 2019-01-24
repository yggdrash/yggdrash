package io.yggdrash.validator.service;

import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.PbftServiceGrpc;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@GRpcService
@ConditionalOnProperty(name = "yggdrash.validator.consensus.algorithm", havingValue = "pbft")
public class PbftServerStub extends PbftServiceGrpc.PbftServiceImplBase {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftServerStub.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();

    @Autowired
    public PbftServerStub() {
    }

    @Override
    public void multicastPbftMessage(io.yggdrash.proto.PbftProto.PbftMessage request,
                                     io.grpc.stub.StreamObserver<io.yggdrash.proto.NetProto.Empty> responseObserver) {

        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }

}
