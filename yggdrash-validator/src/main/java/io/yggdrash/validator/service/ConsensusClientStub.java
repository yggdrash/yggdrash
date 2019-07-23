package io.yggdrash.validator.service;

import io.grpc.ManagedChannel;
import io.yggdrash.proto.Proto;

import java.util.List;

public interface ConsensusClientStub<T> {

    List<T> getBlockList(long index);

    boolean isMyclient();

    String getAddr();

    String getHost();

    int getPort();

    String getId();

    ManagedChannel getChannel();

    boolean isRunning();

    void multicastTransaction(Proto.Transaction protoTx);

}
