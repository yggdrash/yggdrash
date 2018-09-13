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
import java.util.Objects;

public class ChannelMock implements PeerClientChannel {
    private final Peer peer;

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
    public void stop(String ynodeUri) {
    }

    @Override
    public Pong ping(String message) {
        return Pong.newBuilder().setPong("Pong").build();
    }

    @Override
    public List<Proto.Block> syncBlock(BranchId branchId, long offset) {
        return Collections.singletonList(TestUtils.sampleBlock().toProtoBlock());
    }

    @Override
    public List<Proto.Transaction> syncTransaction(BranchId branchId) {
        return Collections.singletonList(
                Objects.requireNonNull(Transaction.toProtoTransaction(TestUtils.sampleTx())));
    }

    @Override
    public void broadcastTransaction(Proto.Transaction[] txs) {
    }

    @Override
    public void broadcastBlock(Proto.Block[] blocks) {
    }

    @Override
    public List<String> requestPeerList(String ynodeUri, int limit) {
        return Collections.singletonList(peer.getYnodeUri());
    }

    @Override
    public void disconnectPeer(String ynodeUri) {

    }
}
