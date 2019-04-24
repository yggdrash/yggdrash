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

package io.yggdrash.node.service;

import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.p2p.BlockChainHandler;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.PbftServiceGrpc;
import io.yggdrash.proto.Proto;
import io.yggdrash.validator.data.pbft.PbftBlock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PbftPeerHandlerTest extends TestConstants.CiTest {

    private static final Peer TARGET = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");

    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

    @Spy
    private PbftServiceGrpc.PbftServiceImplBase pbftService;

    @Captor
    private ArgumentCaptor<CommonProto.Offset> offsetCaptor;

    @Captor
    private ArgumentCaptor<PbftProto.PbftBlock> blockCaptor;

    private BlockChainHandler peerHandler;

    private BranchId yggdrash;

    @Before
    public void setUp() {
        yggdrash = TestConstants.yggdrash();
        peerHandler = new PeerHandlerProvider.PbftPeerHandler(grpcServerRule.getChannel(), TARGET);
        grpcServerRule.getServiceRegistry().addService(pbftService);
        assertEquals(TARGET, peerHandler.getPeer());
    }

    @Test
    public void getPbftBlockListTest() {
        doAnswer((invocationOnMock) -> {
            StreamObserver<Proto.BlockList> argument = invocationOnMock.getArgument(1);
            argument.onNext(null);
            argument.onCompleted();
            return null;
        }).when(pbftService).getPbftBlockList(offsetCaptor.capture(), any());

        peerHandler.syncBlock(yggdrash, 1);

        verify(pbftService).getPbftBlockList(offsetCaptor.capture(), any());

        assertEquals(1, offsetCaptor.getValue().getIndex());
    }

    @Test
    public void broadcastPbftBlockTest() {
        doAnswer((invocationOnMock) -> {
            StreamObserver<Proto.BlockList> argument = invocationOnMock.getArgument(1);
            argument.onNext(null);
            argument.onCompleted();
            return null;
        }).when(pbftService).broadcastPbftBlock(blockCaptor.capture(), any());

        Block block = BlockChainTestUtils.genesisBlock();

        PbftBlock pbftBlock = new PbftBlock(PbftProto.PbftBlock.newBuilder().setBlock(block.getProtoBlock()).build());
        peerHandler.broadcastBlock(pbftBlock);

        verify(pbftService).broadcastPbftBlock(blockCaptor.capture(), any());

        assertEquals(block.getBody().getCount(), blockCaptor.getValue().getBlock().getBody().getTransactionsCount());
    }
}
