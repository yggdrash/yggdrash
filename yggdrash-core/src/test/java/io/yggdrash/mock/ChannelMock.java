package io.yggdrash.mock;

import io.yggdrash.TestUtils;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.Transaction;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerClientChannel;
import io.yggdrash.proto.Pong;
import io.yggdrash.proto.Proto;

import java.util.Collections;
import java.util.List;

public class ChannelMock implements PeerClientChannel {
    private final Peer peer;
    private final Pong pong = Pong.newBuilder().setPong("Pong").build();
    private boolean pongResponse = true;

    public ChannelMock(String ynodeUri) {
        this.peer = Peer.valueOf(ynodeUri);
    }

    @Override
    public Peer getPeer() {
        return peer;
    }

    @Override
    public void stop() {
    }

    @Override
    public Pong ping(String message) {
        if (pongResponse) {
            pongResponse = false;
            return pong;
        }
        pongResponse = true;
        return null;
    }

    @Override
    public List<Proto.Block> syncBlock(BranchId branchId, long offset) {
        return Collections.singletonList(TestUtils.sampleBlock().toProtoBlock());
    }

    @Override
    public List<Proto.Transaction> syncTransaction(BranchId branchId) {
        Proto.Transaction protoTx = Transaction.toProtoTransaction(TestUtils.sampleTransferTx());
        return Collections.singletonList(protoTx);
    }

    @Override
    public void broadcastTransaction(Proto.Transaction[] txs) {
    }

    @Override
    public void broadcastBlock(Proto.Block[] blocks) {
    }

    public static PeerClientChannel dummy() {
        return new ChannelMock("ynode://75bff16c@localhost:32918");
    }
}
