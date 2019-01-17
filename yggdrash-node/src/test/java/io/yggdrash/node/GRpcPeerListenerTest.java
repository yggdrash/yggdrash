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
import io.grpc.testing.GrpcServerRule;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerTable;
import io.yggdrash.node.service.BlockChainService;
import io.yggdrash.node.service.PeerService;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.PeerGrpc;
import io.yggdrash.proto.Proto;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GRpcPeerListenerTest {

    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

    @Mock
    private PeerTable peerTableMock;

    @Mock
    private BranchGroup branchGroupMock;

    private TransactionHusk tx;
    private BlockHusk block;
    private BranchId branchId;

    @Before
    public void setUp() {
        grpcServerRule.getServiceRegistry().addService(new PeerService(peerTableMock));
        grpcServerRule.getServiceRegistry().addService(new BlockChainService(branchGroupMock)
        );

        tx = BlockChainTestUtils.createTransferTxHusk();
        when(branchGroupMock.addTransaction(any())).thenReturn(tx);
        block = BlockChainTestUtils.genesisBlock();
        branchId = block.getBranchId();
    }

    @Test
    public void play() {
        PeerGrpc.PeerBlockingStub blockingStub = PeerGrpc.newBlockingStub(
                grpcServerRule.getChannel());

        Proto.PeerInfo peerInfo = Proto.PeerInfo.newBuilder()
                .setUrl("ynode://75bff16c@127.0.0.1:32918")
                .build();
        Proto.Ping ping = Proto.Ping.newBuilder().setPing("Ping").setPeer(peerInfo).build();

        Proto.Pong pong = blockingStub.play(ping);

        assertEquals("Pong", pong.getPong());
    }


    @Test
    public void findPeers() {
        PeerGrpc.PeerBlockingStub blockingStub = PeerGrpc.newBlockingStub(
                grpcServerRule.getChannel());

        Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        Proto.BestBlock bestBlock = Proto.BestBlock.newBuilder()
                .setBranch(ByteString.copyFrom(branchId.getBytes()))
                .setIndex(0).build();
        Proto.RequestPeer requestPeer = Proto.RequestPeer.newBuilder()
                .setPubKey(peer.getPubKey().toString())
                .setIp(peer.getHost())
                .setPort(peer.getPort())
                .addBestBlocks(bestBlock)
                .build();

        Proto.PeerList peerList = blockingStub.findPeers(requestPeer);

        assertEquals(0, peerList.getPeersCount());
    }

    @Test
    public void broadcastBlock() {
        BlockChainGrpc.BlockChainBlockingStub blockChainBlockingStub
                = BlockChainGrpc.newBlockingStub(grpcServerRule.getChannel());
        NetProto.Empty empty = NetProto.Empty.newBuilder().build();
        Proto.Block block = BlockChainTestUtils.genesisBlock().getInstance();
        assertEquals(empty, blockChainBlockingStub.broadcastBlock(block));
    }

    @Test
    public void broadcastTransaction() {
        BlockChainGrpc.BlockChainBlockingStub blockChainBlockingStub
                = BlockChainGrpc.newBlockingStub(grpcServerRule.getChannel());
        NetProto.Empty empty = NetProto.Empty.newBuilder().build();
        Proto.Transaction tx = BlockChainTestUtils.createTransferTxHusk().getInstance();
        assertEquals(empty, blockChainBlockingStub.broadcastTransaction(tx));
    }

    @Test
    public void syncBlock() {
        Set<BlockHusk> blocks = new HashSet<>();
        blocks.add(block);
        when(branchGroupMock.getBlockByIndex(branchId, 0L)).thenReturn(block);
        when(branchGroupMock.getBranch(any()))
                .thenReturn(BlockChainTestUtils.createBlockChain(false));

        BlockChainGrpc.BlockChainBlockingStub blockingStub
                = BlockChainGrpc.newBlockingStub(grpcServerRule.getChannel());
        ByteString branch = ByteString.copyFrom(branchId.getBytes());
        NetProto.SyncLimit syncLimit = NetProto.SyncLimit.newBuilder().setOffset(0).setLimit(10000)
                .setBranch(branch).build();
        Proto.BlockList list = blockingStub.syncBlock(syncLimit);
        assertEquals(1, list.getBlocksCount());
    }

    @Test
    public void syncTransaction() {
        when(branchGroupMock.getUnconfirmedTxs(branchId))
                .thenReturn(Collections.singletonList(tx));

        BlockChainGrpc.BlockChainBlockingStub blockingStub
                = BlockChainGrpc.newBlockingStub(grpcServerRule.getChannel());
        ByteString branch = ByteString.copyFrom(branchId.getBytes());
        NetProto.SyncLimit syncLimit
                = NetProto.SyncLimit.newBuilder().setBranch(branch).build();
        Proto.TransactionList list = blockingStub.syncTransaction(syncLimit);
        assertEquals(1, list.getTransactionsCount());
    }
}
