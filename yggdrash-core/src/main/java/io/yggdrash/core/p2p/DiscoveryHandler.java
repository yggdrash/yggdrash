/*
 * Copyright 2019 Akashic Foundation
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

package io.yggdrash.core.p2p;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.proto.DiscoveryServiceGrpc;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.yggdrash.common.config.Constants.TIMEOUT_PING;

public class DiscoveryHandler<T> implements BlockChainHandler<T> {
    private static final FailedOperationException NOT_IMPLEMENTED = new FailedOperationException("Not implemented");
    private static final Logger log = LoggerFactory.getLogger(DiscoveryHandler.class);

    private final Peer peer;
    private final ManagedChannel channel;

    private DiscoveryServiceGrpc.DiscoveryServiceBlockingStub peerBlockingStub;
    private int failCount = 0;

    public DiscoveryHandler(Peer peer) {
        this(ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort()).usePlaintext().build(), peer);
    }

    public DiscoveryHandler(ManagedChannel channel, Peer peer) {
        this.channel = channel;
        this.peer = peer;
        this.peerBlockingStub = DiscoveryServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public int getFailCount() {
        return failCount;
    }

    @Override
    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    @Override
    public Peer getPeer() {
        return peer;
    }

    @Override
    public String gerConnectivityState() {
        return channel.getState(false).toString();
    }


    @Override
    public void stop() {
        log.debug("Stop for peer={}", peer.getYnodeUri());

        if (channel != null) {
            channel.shutdown();
        }
    }

    @Override
    public List<Peer> findPeers(BranchId branchId, Peer peer) {
        log.trace("findPeers() Branch({}) Peer({})", branchId.toString(), peer.toString());
        Proto.TargetPeer targetPeer = Proto.TargetPeer.newBuilder()
                .setPubKey(peer.getPubKey().toString())
                .setIp(peer.getHost())
                .setPort(peer.getPort())
                .setBranch(ByteString.copyFrom(branchId.getBytes()))
                .build();
        List<Peer> peerList = peerBlockingStub.withDeadlineAfter(TIMEOUT_PING, TimeUnit.SECONDS)
                .findPeers(targetPeer).getPeersList().stream()
                .map(peerInfo -> Peer.valueOf(peerInfo.getUrl()))
                .collect(Collectors.toList());

        log.trace("findPeers() result: {}", peerList.toString());

        return peerList;
    }

    @Override
    public String ping(BranchId branchId, Peer owner, String message) {
        Proto.Ping request = Proto.Ping.newBuilder().setPing(message)
                .setFrom(owner.getYnodeUri())
                .setTo(peer.getYnodeUri())
                .setBranch(ByteString.copyFrom(branchId.getBytes()))
                .setBestBlock(owner.getBestBlock())
                .build();
        return peerBlockingStub.withDeadlineAfter(TIMEOUT_PING, TimeUnit.SECONDS).ping(request).getPong();
    }

    @Override
    public long pingPong(BranchId branchId, Peer owner, String message) {
        Proto.Ping request = Proto.Ping.newBuilder().setPing(message)
                .setFrom(owner.getYnodeUri())
                .setTo(peer.getYnodeUri())
                .setBranch(ByteString.copyFrom(branchId.getBytes()))
                .setBestBlock(owner.getBestBlock())
                .build();

        Proto.Pong response = peerBlockingStub.withDeadlineAfter(TIMEOUT_PING, TimeUnit.SECONDS).ping(request);
        if (Arrays.equals(branchId.getBytes(), request.getBranch().toByteArray())) {
            return response.getBestBlock();
        }

        return -1;
    }

    @Override
    public Future<List<ConsensusBlock<T>>> syncBlock(BranchId branchId, long offset) {
        return null;
    }

    @Override
    public Future<List<Transaction>> syncTx(BranchId branchId) {
        throw NOT_IMPLEMENTED;
    }

    @Override
    public void broadcastBlock(ConsensusBlock<T> block) {
        throw NOT_IMPLEMENTED;
    }

    @Override
    public void broadcastTx(Transaction tx) {
        throw NOT_IMPLEMENTED;
    }
}
