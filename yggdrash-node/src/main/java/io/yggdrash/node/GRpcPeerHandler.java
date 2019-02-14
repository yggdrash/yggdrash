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
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.net.BestBlock;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerHandler;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto.SyncLimit;
import io.yggdrash.proto.PeerGrpc;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GRpcPeerHandler implements PeerHandler {

    private static final Logger log = LoggerFactory.getLogger(GRpcPeerHandler.class);
    private static final int DEFAULT_LIMIT = 10000;

    private final ManagedChannel channel;
    private final PeerGrpc.PeerBlockingStub blockingPeerStub;
    private final BlockChainGrpc.BlockChainBlockingStub blockingBlockChainStub;
    private final BlockChainGrpc.BlockChainBlockingStub asyncBlockChainStub;
    private final Peer peer;

    GRpcPeerHandler(Peer peer) {
        this(ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort()).usePlaintext()
                .build(), peer);
    }

    GRpcPeerHandler(ManagedChannel channel, Peer peer) {
        this.channel = channel;
        this.peer = peer;
        this.blockingPeerStub = PeerGrpc.newBlockingStub(channel);
        this.blockingBlockChainStub = BlockChainGrpc.newBlockingStub(channel);
        this.asyncBlockChainStub = BlockChainGrpc.newBlockingStub(channel);
    }

    @Override
    public List<Peer> findPeers(Peer peer) {
        Proto.RequestPeer requestPeer = Proto.RequestPeer.newBuilder()
                .setPubKey(peer.getPubKey().toString())
                .setIp(peer.getHost())
                .setPort(peer.getPort())
                .addAllBestBlocks(bestBlocksByPeer(peer))
                .build();
        return blockingPeerStub.findPeers(requestPeer).getPeersList().stream()
                .map(peerInfo -> Peer.valueOf(peerInfo.getUrl()))
                .collect(Collectors.toList());
    }

    private List<Proto.BestBlock> bestBlocksByPeer(Peer peer) {
        List<Proto.BestBlock> bestBlocks = new ArrayList<>();
        for (BestBlock bb : peer.getBestBlocks()) {
            Proto.BestBlock bestBlock = Proto.BestBlock.newBuilder()
                    .setBranch(ByteString.copyFrom(bb.getBranchId().getBytes()))
                    .setIndex(bb.getIndex()).build();
            bestBlocks.add(bestBlock);
        }
        return bestBlocks;
    }

    @Override
    public String ping(String message, Peer peer) {
        Proto.Ping request = Proto.Ping.newBuilder().setPing(message)
                .setPeer(Proto.PeerInfo.newBuilder().setUrl(peer.getYnodeUri())).build();
        return blockingPeerStub.ping(request).getPong();
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

    /**
     * Sync block request
     *
     * @param offset the start block index to sync
     * @return the block list
     */
    @Override
    public List<BlockHusk> syncBlock(BranchId branchId, long offset) {
        SyncLimit syncLimit = SyncLimit.newBuilder()
                .setOffset(offset)
                .setLimit(DEFAULT_LIMIT)
                .setBranch(ByteString.copyFrom(branchId.getBytes())).build();
        return blockingBlockChainStub.syncBlock(syncLimit).getBlocksList().stream()
                .map(BlockHusk::new)
                .collect(Collectors.toList());
    }

    /**
     * Sync transaction request
     *
     * @return the transaction list
     */
    @Override
    public List<TransactionHusk> syncTransaction(BranchId branchId) {
        SyncLimit syncLimit = SyncLimit.newBuilder()
                .setBranch(ByteString.copyFrom(branchId.getBytes())).build();
        return blockingBlockChainStub.syncTransaction(syncLimit).getTransactionsList().stream()
                .map(TransactionHusk::new)
                .collect(Collectors.toList());
    }

    @Override
    public void broadcastTransaction(TransactionHusk tx) {
        log.trace("Broadcasting txs -> {}", tx.getHash());
        asyncBlockChainStub.broadcastTransaction(tx.getInstance());
    }

    @Override
    public void broadcastBlock(BlockHusk block) {
        log.trace("Broadcasting blocks -> {}", peer.getHost() + ":" + peer.getPort());
        asyncBlockChainStub.broadcastBlock(block.getInstance());
    }
}
