package io.yggdrash.core.net;

import io.yggdrash.core.BranchId;
import io.yggdrash.proto.Pong;
import io.yggdrash.proto.Proto;

import java.util.List;

public interface PeerClientChannel {

    Peer getPeer();

    void stop();

    Pong ping(String message);

    List<Proto.Block> syncBlock(BranchId branchId, long offset);

    List<Proto.Transaction> syncTransaction(BranchId branchId);

    void broadcastTransaction(Proto.Transaction[] txs);

    void broadcastBlock(Proto.Block[] blocks);
}
