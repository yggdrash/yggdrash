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

package io.yggdrash.node.service;

import com.google.protobuf.ByteString;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.p2p.AbstractPeerHandler;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerHandlerFactory;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.proto.EbftServiceGrpc;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.PbftServiceGrpc;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.data.pbft.PbftBlock;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ConsensusHandlerFactory {

    private ConsensusHandlerFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static PeerHandlerFactory factory() {
        return (consensusAlgorithm, peer) -> {

            switch (consensusAlgorithm) {
                case "pbft":
                    return new PbftPeerHandler(peer);
                case "ebft":
                    return new EbftPeerHandler(peer);
                default:
            }
            throw new NotValidateException("Algorithm is not valid.");
        };
    }

    public static class PbftPeerHandler extends AbstractPeerHandler<PbftProto.PbftBlock> {
        private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftPeerHandler.class);

        private final PbftServiceGrpc.PbftServiceBlockingStub blockingStub;

        PbftPeerHandler(Peer peer) {
            this(ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort()).usePlaintext().build(), peer);
        }

        public PbftPeerHandler(ManagedChannel channel, Peer peer) {
            super(channel, peer);
            blockingStub = PbftServiceGrpc.newBlockingStub(channel);
        }

        @Override
        public Future<List<ConsensusBlock<PbftProto.PbftBlock>>> syncBlock(BranchId branchId, long offset) {
            log.debug("Requesting sync block: branchId={}, offset={}", branchId, offset);

            CommonProto.Offset request = CommonProto.Offset.newBuilder()
                    .setIndex(offset)
                    .setCount(DEFAULT_LIMIT)
                    .setChain(ByteString.copyFrom(branchId.getBytes()))
                    .build();

            PbftProto.PbftBlockList protoEbftBlockList = blockingStub.getPbftBlockList(request);

            CompletableFuture<List<ConsensusBlock<PbftProto.PbftBlock>>> futureBlockList = new CompletableFuture<>();
            List<ConsensusBlock<PbftProto.PbftBlock>> newEbftBlockList = new ArrayList<>();
            for (PbftProto.PbftBlock block : protoEbftBlockList.getPbftBlockList()) {
                newEbftBlockList.add(new PbftBlock(block));
            }

            futureBlockList.complete(newEbftBlockList);
            return futureBlockList;
        }

        // When we send a (single) block to the server and get back a (single) empty.
        // Use the asynchronous stub for this method.
        @Override
        public void broadcastBlock(ConsensusBlock<PbftProto.PbftBlock> block) {
            log.debug("Broadcasting blocks -> {}", getPeer().getYnodeUri());

            blockingStub.withDeadlineAfter(3, TimeUnit.SECONDS).broadcastPbftBlock(block.getInstance());
        }
    }

    public static class EbftPeerHandler extends AbstractPeerHandler<EbftProto.EbftBlock> {
        private static final org.slf4j.Logger log = LoggerFactory.getLogger(EbftPeerHandler.class);

        private final EbftServiceGrpc.EbftServiceBlockingStub blockingStub;

        EbftPeerHandler(Peer peer) {
            this(ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort()).usePlaintext()
                    .build(), peer);
        }

        public EbftPeerHandler(ManagedChannel channel, Peer peer) {
            super(channel, peer);
            blockingStub = EbftServiceGrpc.newBlockingStub(channel);
        }

        @Override
        public Future<List<ConsensusBlock<EbftProto.EbftBlock>>> syncBlock(BranchId branchId, long offset) {
            log.debug("Requesting sync block: branchId={}, offset={}", branchId, offset);

            CommonProto.Offset request = CommonProto.Offset.newBuilder()
                    .setIndex(offset)
                    .setCount(DEFAULT_LIMIT)
                    .setChain(ByteString.copyFrom(branchId.getBytes()))
                    .build();

            EbftProto.EbftBlockList protoEbftBlockList = blockingStub.getEbftBlockList(request);

            CompletableFuture<List<ConsensusBlock<EbftProto.EbftBlock>>> futureBlockList = new CompletableFuture<>();
            List<ConsensusBlock<EbftProto.EbftBlock>> newEbftBlockList = new ArrayList<>();
            if (!Context.current().isCancelled()) {
                for (EbftProto.EbftBlock block : protoEbftBlockList.getEbftBlockList()) {
                    newEbftBlockList.add(new EbftBlock(block));
                }
            }

            futureBlockList.complete(newEbftBlockList);
            return futureBlockList;
        }

        // When we send a (single) block to the server and get back a (single) empty.
        // Use the asynchronous stub for this method.
        @Override
        public void broadcastBlock(ConsensusBlock<EbftProto.EbftBlock> block) {
            log.debug("Broadcasting blocks -> {}", getPeer().getYnodeUri());

            blockingStub.withDeadlineAfter(3, TimeUnit.SECONDS).broadcastEbftBlock(block.getInstance());
        }
    }

}
