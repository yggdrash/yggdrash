/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerClientChannel;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.NetProto.SyncLimit;
import io.yggdrash.proto.NodeInfo;
import io.yggdrash.proto.PeerGrpc;
import io.yggdrash.proto.PeerInfo;
import io.yggdrash.proto.Ping;
import io.yggdrash.proto.PingPongGrpc;
import io.yggdrash.proto.Pong;
import io.yggdrash.proto.Proto;
import io.yggdrash.proto.RequestPeer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GRpcClientChannel implements PeerClientChannel {

    private static final Logger log = LoggerFactory.getLogger(GRpcClientChannel.class);
    private static final int DEFAULT_LIMIT = 10000;

    private final ManagedChannel channel;
    private final PeerGrpc.PeerBlockingStub blockingPeerStub;
    private final PingPongGrpc.PingPongBlockingStub blockingPingPongStub;
    private final BlockChainGrpc.BlockChainBlockingStub blockingBlockChainStub;
    private final BlockChainGrpc.BlockChainBlockingStub asyncBlockChainStub;
    private final Peer peer;

    public GRpcClientChannel(Peer peer) {
        this(ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort()).usePlaintext()
                .build(), peer);
    }

    GRpcClientChannel(ManagedChannel channel, Peer peer) {
        this.channel = channel;
        this.peer = peer;
        this.blockingPeerStub = PeerGrpc.newBlockingStub(channel);
        this.blockingPingPongStub = PingPongGrpc.newBlockingStub(channel);
        this.blockingBlockChainStub = BlockChainGrpc.newBlockingStub(channel);
        this.asyncBlockChainStub = BlockChainGrpc.newBlockingStub(channel);
    }

    @Override
    public List<NodeInfo> findPeers(Peer peer) {
        RequestPeer requestPeer = RequestPeer.newBuilder()
                .setPubKey(peer.getPubKey().toString())
                .setIp(peer.getHost())
                .setPort(peer.getPort())
                .build();
        return blockingPeerStub.findPeers(requestPeer).getNodesList();
    }

    @Override
    public Peer getPeer() {
        return peer;
    }

    @Override
    public void stop() {
        log.debug("Stop for peer=" + peer.getYnodeUri());
        if (channel != null) {
            channel.shutdown();
        }
    }

    @Override
    public Pong ping(String message, Peer peer) {
        PeerInfo peerInfo = PeerInfo.newBuilder().setPubKey(peer.getPubKey().toString())
                .setIp(peer.getHost())
                .setPort(peer.getPort())
                .build();
        Ping request = Ping.newBuilder().setPing(message).setPeer(peerInfo).build();
        return blockingPingPongStub.play(request);
    }

    /**
     * Sync block request
     *
     * @param offset the start block index to sync
     * @return the block list
     */
    @Override
    public List<Proto.Block> syncBlock(BranchId branchId, long offset) {
        SyncLimit syncLimit = SyncLimit.newBuilder()
                .setOffset(offset)
                .setLimit(DEFAULT_LIMIT)
                .setBranch(ByteString.copyFrom(branchId.getBytes())).build();
        return blockingBlockChainStub.syncBlock(syncLimit).getBlocksList();
    }

    /**
     * Sync transaction request
     *
     * @return the transaction list
     */
    @Override
    public List<Proto.Transaction> syncTransaction(BranchId branchId) {
        SyncLimit syncLimit = SyncLimit.newBuilder()
                .setBranch(ByteString.copyFrom(branchId.getBytes())).build();
        return blockingBlockChainStub.syncTransaction(syncLimit).getTransactionsList();
    }

    @Override
    public void broadcastTransaction(Proto.Transaction[] txs) {
        log.info("*** Broadcasting txs...");
        for (Proto.Transaction tx : txs) {
            log.trace("Sending transaction: {}", tx);
            asyncBlockChainStub.broadcastTransaction(tx);
        }
    }

    @Override
    public void broadcastBlock(Proto.Block[] blocks) {
        log.info("*** Broadcasting blocks -> {}", peer.getHost() + ":" + peer.getPort());
        for (Proto.Block block : blocks) {
            log.trace("Sending block: {}", block);
            asyncBlockChainStub.broadcastBlock(block);
        }
    }
}
