package io.yggdrash.node.mock;

import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerClientChannel;
import io.yggdrash.proto.BlockChainProto;
import io.yggdrash.proto.Pong;

import java.util.Collections;
import java.util.List;

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
    public List<BlockChainProto.Block> syncBlock(long offset) {
        return Collections.singletonList(BlockChainProto.Block.getDefaultInstance());
    }

    @Override
    public List<BlockChainProto.Transaction> syncTransaction() {
        return Collections.singletonList(BlockChainProto.Transaction.getDefaultInstance());
    }

    @Override
    public void broadcastTransaction(BlockChainProto.Transaction[] txs) {
    }

    @Override
    public void broadcastBlock(BlockChainProto.Block[] blocks) {
    }

    @Override
    public List<String> requestPeerList(String ynodeUri, int limit) {
        return Collections.singletonList(peer.getYnodeUri());
    }

    @Override
    public void disconnectPeer(String ynodeUri) {

    }
}
