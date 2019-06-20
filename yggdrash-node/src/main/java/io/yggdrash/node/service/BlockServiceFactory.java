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

import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.net.BlockConsumer;
import io.yggdrash.core.net.BlockServiceConsumer;
import io.yggdrash.core.net.CatchUpSyncEventListener;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.proto.EbftServiceGrpc;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.PbftServiceGrpc;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.data.pbft.PbftBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;

@ConditionalOnProperty(name = "yggdrash.node.chain.enabled", matchIfMissing = true)
public class BlockServiceFactory {
    private static final CommonProto.Empty EMPTY = CommonProto.Empty.getDefaultInstance();

    private BlockServiceFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static BindableService create(String consensusAlgorithm, BranchGroup branchGroup,
                                         CatchUpSyncEventListener listener) {
        switch (consensusAlgorithm) {
            case "pbft":
                return new PbftBlockService(branchGroup, listener);
            case "ebft":
                return new EbftBlockService(branchGroup, listener);
            default:
        }
        throw new NotValidateException("Algorithm is not valid.");
    }

    private static class PbftBlockService extends PbftServiceGrpc.PbftServiceImplBase {
        private static final Logger log = LoggerFactory.getLogger(PbftBlockService.class);
        private final BlockConsumer<PbftProto.PbftBlock> blockConsumer;

        private PbftBlockService(BranchGroup branchGroup, CatchUpSyncEventListener listener) {
            this.blockConsumer = new BlockServiceConsumer<>(branchGroup);
            blockConsumer.setListener(listener);
        }

        /**
         * Sync block response
         *
         * @param request          the start branch id, block index and limit to sync
         * @param responseObserver the observer response to the block list
         */
        @Override
        public void getPbftBlockList(CommonProto.Offset request,
                                     StreamObserver<PbftProto.PbftBlockList> responseObserver) {
            long offset = request.getIndex();
            long limit = request.getCount();
            BranchId branchId = BranchId.of(request.getChain().toByteArray());
            log.debug("Received syncBlock request branch={} offset={}, limit={}", branchId, offset, limit);
            List<ConsensusBlock<PbftProto.PbftBlock>> blockList = blockConsumer.syncBlock(branchId, offset, limit);
            PbftProto.PbftBlockList.Builder builder = PbftProto.PbftBlockList.newBuilder();
            for (ConsensusBlock<PbftProto.PbftBlock> block : blockList) {
                builder.addPbftBlock(block.getInstance());
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void broadcastPbftBlock(PbftProto.PbftBlock request,
                                       StreamObserver<CommonProto.Empty> responseObserver) {
            PbftBlock block = new PbftBlock(request);
            log.debug("Received block: id=[{}], hash={}", block.getIndex(), block.getHash());
            blockConsumer.broadcastBlock(block);
            responseObserver.onNext(EMPTY);
            responseObserver.onCompleted();
        }
    }

    private static class EbftBlockService extends EbftServiceGrpc.EbftServiceImplBase {
        private static final Logger log = LoggerFactory.getLogger(EbftBlockService.class);
        private final BlockConsumer<EbftProto.EbftBlock> blockConsumer;

        private EbftBlockService(BranchGroup branchGroup, CatchUpSyncEventListener listener) {
            this.blockConsumer = new BlockServiceConsumer<>(branchGroup);
            blockConsumer.setListener(listener);
        }

        /**
         * Sync block response
         *
         * @param request          the start branch id, block index and limit to sync
         * @param responseObserver the observer response to the block list
         */
        @Override
        public void getEbftBlockList(CommonProto.Offset request,
                                     StreamObserver<EbftProto.EbftBlockList> responseObserver) {
            long offset = request.getIndex();
            long limit = request.getCount();
            BranchId branchId = BranchId.of(request.getChain().toByteArray());
            log.debug("Received syncBlock request branch={} offset={}, limit={}", branchId, offset, limit);
            List<ConsensusBlock<EbftProto.EbftBlock>> blockList = blockConsumer.syncBlock(branchId, offset, limit);
            EbftProto.EbftBlockList.Builder builder = EbftProto.EbftBlockList.newBuilder();
            for (ConsensusBlock<EbftProto.EbftBlock> block : blockList) {
                builder.addEbftBlock(block.getInstance());
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void broadcastEbftBlock(EbftProto.EbftBlock request,
                                       StreamObserver<CommonProto.Empty> responseObserver) {
            EbftBlock block = new EbftBlock(request);
            log.debug("Received block: id=[{}], hash={}", block.getIndex(), block.getHash());
            blockConsumer.broadcastBlock(block);
            responseObserver.onNext(EMPTY);
            responseObserver.onCompleted();
        }
    }
}
