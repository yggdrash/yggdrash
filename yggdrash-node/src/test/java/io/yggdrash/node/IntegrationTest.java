/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.node;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.PeerTestUtils;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.p2p.BlockChainDialer;
import io.yggdrash.core.p2p.BlockChainHandlerFactory;
import io.yggdrash.core.p2p.DiscoveryHandler;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerDialer;
import io.yggdrash.node.service.PeerHandlerProvider;
import io.yggdrash.proto.PbftProto;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IntegrationTest extends TcpNodeTesting {

    private static final Scanner scan = new Scanner(System.in);

    private static final Integer BOOTSTRAP = 0;
    private static final Integer PROXY = 1;
    private static final Integer GENERAL = 2;
    private static final Integer VALIDATOR1 = 3;
    private static final Integer VALIDATOR2 = 4;
    private static final Integer VALIDATOR3 = 5;
    private static final Integer VALIDATOR4 = 6;
    private static final Integer VALIDATOR5 = 7;

    private static Integer DEFAULT_PORT_NUM = 33333;

    //TODO These Uris/ BranchId/ ContractVersion should be settable
    private static String bootStrapNodeUri = "ynode://00@127.0.0.1:32918";
    private static String proxyNodeUri = "ynode://f871740aedb55e0d4f110502d5221c4a648f4c27@127.0.0.1:32911";
    private static String generalNodeUri = "ynode://d5acd6a61102604b1ca4fbd88c17b631e0de3877@127.0.0.1:32919";
    private static String validatorNode1Uri = "ynode://77283a04b3410fe21ba5ed04c7bd3ba89e70b78c@127.0.0.1:32901";
    private static String validatorNode2Uri = "ynode://9911fb4663637706811a53a0e0b4bcedeee38686@127.0.0.1:32902";
    private static String validatorNode3Uri = "ynode://2ee2eb80c93d031147c21ba8e2e0f0f4a33f5312@127.0.0.1:32903";
    private static String validatorNode4Uri = "ynode://51e2128e8deb622c2ec6dc38f9d895f0be044eb4@127.0.0.1:32904";
    private static String validatorNode5Uri = "ynode://047269a50640ed2b0d45d461488c13abad1e0fac@127.0.0.1:32905";

    private static BranchId branchId = BranchId.of("3b898581ef0a6f172d31740c9de024101f1293a6");
    private static ContractVersion yeedContract = ContractVersion.of("30783a1311b9c68dd3a92596d650ae6914b01658");

    //private static TestNode proxyTestNode;

    private static List<Peer> peers;
    private static List<DiscoveryHandler> peerDiscoveryHandlers;
    private static List<PeerHandlerProvider.PbftPeerHandler> peerPbftHandlers;

    public IntegrationTest() {
        init();
    }

    // Create a peer cluster for generating and broadcasting blocks
    private List<Peer> createPeers() {
        Peer bootstrapNode = Peer.valueOf(bootStrapNodeUri);
        Peer proxyNode = Peer.valueOf(proxyNodeUri);
        Peer generalNode = Peer.valueOf(generalNodeUri);
        Peer validatorNode1 = Peer.valueOf(validatorNode1Uri);
        Peer validatorNode2 = Peer.valueOf(validatorNode2Uri);
        Peer validatorNode3 = Peer.valueOf(validatorNode3Uri);
        Peer validatorNode4 = Peer.valueOf(validatorNode4Uri);
        Peer validatorNode5 = Peer.valueOf(validatorNode5Uri);

        List<Peer> peerCluster = new ArrayList<>();
        peerCluster.add(bootstrapNode);
        peerCluster.add(proxyNode);
        peerCluster.add(generalNode);
        peerCluster.add(validatorNode1);
        peerCluster.add(validatorNode2);
        peerCluster.add(validatorNode3);
        peerCluster.add(validatorNode4);
        peerCluster.add(validatorNode5);
        peers = peerCluster;

        return peerCluster;
    }

    // Create peerHandler cluster to send rpc msg (findPeers, ping)
    private List<PeerHandlerProvider.PbftPeerHandler> createPbftHandlers() {
        return createPeers().stream()
                .map(p -> new PeerHandlerProvider.PbftPeerHandler(createChannel(p), p)).collect(Collectors.toList());
    }

    // Create peerHandler cluster to send rpc msg (syncBlock, syncTx, broadcastBlock, broadcastTx)
    private List<DiscoveryHandler> createDiscoveryHandlers() {
        return peers.stream()
                .map((Function<Peer, DiscoveryHandler>) DiscoveryHandler::new)
                .collect(Collectors.toList());
    }

    // TODO doesn't work (ERR:Call Interrupted Exception)
    private TestNode createProxyTestNode() {
        //BlockChainHandlerFactory factory = PeerHandlerProvider.factory();
        //return TestNode.createProxyNode(factory, Collections.singletonList(bootStrapNodeUri), getValidatorUris());
        return createAndStartNode(
                32920, true, Collections.singletonList(bootStrapNodeUri), getValidatorUris());
    }

    private List<String> getValidatorUris() {
        List<String> validators = new ArrayList<>();
        validators.add(validatorNode1Uri);
        validators.add(validatorNode2Uri);
        validators.add(validatorNode3Uri);
        validators.add(validatorNode4Uri);
        validators.add(validatorNode5Uri);
        return validators;
    }

    private List<TestNode> createTestNodes(int nodeCnt, boolean enableBranch) {
        return createTestNodes(nodeCnt, enableBranch, Collections.singletonList(bootStrapNodeUri));
    }

    private List<TestNode> createTestNodes(int nodeCnt, boolean enableBranch, List<String> seedPeerList) {
        return IntStream.range(DEFAULT_PORT_NUM, DEFAULT_PORT_NUM + nodeCnt)
                .mapToObj(i -> createAndStartNode(i, enableBranch, seedPeerList))
                .collect(Collectors.toList());
    }

    private void init() {
        peerPbftHandlers = createPbftHandlers();
        peerDiscoveryHandlers = createDiscoveryHandlers();
        //proxyTestNode = createProxyTestNode();
    }

    /* Test */

    //@Test
    public void shouldNotNull() {
        Assert.assertNotNull(peers);
        Assert.assertNotNull(peerDiscoveryHandlers);
        Assert.assertEquals(peers.size(), peerDiscoveryHandlers.size());
        //Assert.assertNotNull(proxyTestNode);
    }

    //@Test
    public void creatTestNodesTest() {
        List<TestNode> nodes = createTestNodes(2, true);
        Assert.assertNotNull(nodes);
        Assert.assertEquals(2, nodes.size());

        nodes.forEach(TestNode::bootstrapping);
        Integer port = DEFAULT_PORT_NUM;
        for (TestNode n : nodes) {
            isInitializedProperly(n);
            Assert.assertEquals(0, n.getActivePeerCount()); // no validator peerHandlers

            String testNodeUri = PeerTestUtils.NODE_URI_PREFIX + port;
            Assert.assertEquals(testNodeUri, n.peerTableGroup.getOwner().getYnodeUri());
            port++;
        }
    }

    /*
    @Test
    public void proxyNodeTest() {
        isInitializedProperly(proxyTestNode);
        Assert.assertEquals(5, proxyTestNode.getActivePeerCount()); // validator peerHandlers count

        String proxyTestNodeUri = PeerTestUtils.NODE_URI_PREFIX + PeerTestUtils.OWNER_PORT;
        Assert.assertEquals(proxyTestNodeUri, proxyTestNode.peerTableGroup.getOwner().getYnodeUri());
    }
    */

    private void isInitializedProperly(TestNode n) {
        Assert.assertFalse(n.isSeed());
        Assert.assertTrue(n.peerTableGroup.getSeedPeerList().contains(bootStrapNodeUri));

        BlockChain branch = n.getBranchGroup().getBranch(branchId);
        Assert.assertNotNull(branch);
        Assert.assertTrue(branch.containBranchContract(yeedContract));

        BlockChainManager blockChainManager = branch.getBlockChainManager();
        if (blockChainManager.getLastIndex() == 0) { // not synced yet
            Assert.assertEquals(3, blockChainManager.countOfTxs()); // genesis txs
            Assert.assertEquals(1, blockChainManager.countOfBlocks()); // only genesis block
        }
    }

    /* Getter */

    private PeerDialer createPeerDialer() {
        BlockChainHandlerFactory factory = PeerHandlerProvider.factory();
        PeerDialer peerDialer = new BlockChainDialer(factory);
        peerDialer.addConsensus(branchId, "pbft");
        return peerDialer;
    }

    private List<Peer> getPeers() {
        return peers;
    }

    private List<PeerHandlerProvider.PbftPeerHandler> getPbftPeerHandlers() {
        return peerPbftHandlers;
    }

    private List<DiscoveryHandler> getDiscoveryHandlers() {
        return peerDiscoveryHandlers;
    }

    /* Main Test */

    private static IntegrationTest test;

    public static void main(String[] args) throws Exception {
        test = new IntegrationTest();
        // print basic node config info
        test.getPeers().stream().map(Peer::getYnodeUri).forEach(System.out::println);

        // TODO query whether to overwrite node configuration

        run();
    }

    private static void run() throws Exception {
        System.out.println("[1] CatchUpRequest Test");
        switch (scan.nextLine()) {
            case "1":
                String catchUpTestDescription = "CatchUpRequest 는 syncBlock, ping, broadcastBlock 시 발생합니다. "
                        + "즉, 요청한 블록의 높이가 더 높을 경우, healthCheck 시 peer 의 bestBlock 이 더 높은 경우, "
                        + "브로드캐스트 받은 블록의 높이가 더 높은 경우에 발생해야 합니다. "
                        + "CatchUpRequest Test 는 3가지 상황에서 모두 요청이 발생하는지 확인하기 위함입니다.";
                System.out.println(catchUpTestDescription);
                validateCatchupRequest();
                break;
            default:
                break;
        }
    }

    /* Methods for TestNodes and Handlers */

    private static void broadcastTxs(PeerHandlerProvider.PbftPeerHandler t, int cnt) {
        IntStream.range(0, cnt)
                .mapToObj(i -> BlockChainTestUtils.createTransferTx(branchId, yeedContract))
                .collect(Collectors.toList())
                .forEach(t::broadcastTx);
    }

    private static void broadcastBlocks(PeerHandlerProvider.PbftPeerHandler t, int cnt, int txSize) {
        // contractManager is null
        BlockChainTestUtils.createBlockListWithTxs(cnt, txSize, null).forEach(t::broadcastBlock);
    }

    private static void broadcastBlock(PeerHandlerProvider.PbftPeerHandler t,
                                       ConsensusBlock<PbftProto.PbftBlock> block) {
        t.broadcastBlock(block);
    }

    private static void findPeers(TestNode n, Peer to, Peer target) {
        addPeer(n, to);
        List<Peer> res = new ArrayList<>();
        n.getPeerNetwork()
                .getHandlerList(branchId)
                .stream()
                .map(blockChainHandler -> blockChainHandler.findPeers(branchId, target))
                .forEach(res::addAll);
        //from.peerTableGroup.getPeerTable(branchId).dropPeer(to);
    }

    private static void findPeers(DiscoveryHandler t, Peer target) {
        t.findPeers(branchId, target);
    }

    private static void addPeer(TestNode n, Peer p) {
        n.peerTableGroup.addPeer(branchId, p);
    }

    public static void addPeer(TestNode n, List<Peer> peerList) {
        peerList.forEach(p -> addPeer(n, p));
    }

    private static void ping(TestNode n, Peer t) {
        n.peerTask.healthCheck();
    }

    private static void ping(DiscoveryHandler targetHandler, Peer from) {
        targetHandler.ping(branchId, from, "Ping");
        //createPeerDialer().healthCheck(
        //        BranchId.of("a1d0ade6dcb5db356c2e8a5ee09d2568072eef09"), from, handler.getPeer());
    }

    private static void pingWithBlockHeight(TestNode n, DiscoveryHandler t, long blockHeight) {
        if (blockHeight > 0) {
            n.getPeer().setBestBlock(blockHeight);
        }
        ping(n, t.getPeer());
    }

    private static void syncBlock(TestNode n, PeerHandlerProvider.PbftPeerHandler targetHandler) throws Exception {
        n.getSyncManger().syncBlock(targetHandler, n.getBranchGroup().getBranch(branchId));
    }

    private static void syncBlock(PeerHandlerProvider.PbftPeerHandler targetHandler, long offset) {
        targetHandler.syncBlock(branchId, offset);
    }

    private static void syncBlock(TestNode n, PeerHandlerProvider.PbftPeerHandler target, int cnt) {
        IntStream.range(0, cnt).forEach(i -> {
            try {
                syncBlock(n, target);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void syncTx(TestNode n, PeerHandlerProvider.PbftPeerHandler targetHandler) {
        n.getSyncManger().syncTransaction(targetHandler, n.getBranchGroup().getBranch(branchId));
    }

    private static void syncTx(PeerHandlerProvider.PbftPeerHandler targetHandler) {
        targetHandler.syncTx(branchId);
    }

    private static void shutdown(TestNode n) {
        n.shutdown();
    }

    private void stopHandler(DiscoveryHandler targetHandler) {
        targetHandler.stop();
    }

    /* Test Scenarios (something bad requests or process validation) */

    private static void validateCatchupRequest() throws Exception {
        TestNode node = test.createTestNodes(1, true).get(0);
        test.isInitializedProperly(node);
        node.bootstrapping();

        // Make sure that catchupRequest works well (when syncBlock, ping, broadcastBlock)
        addPeer(node, test.getPeers().get(PROXY));
        //Set blockHeight higher than the targets' will result in a catchup request.
        pingWithBlockHeight(node, test.getDiscoveryHandlers().get(PROXY), 700);
        //If the height of the transferred block is higher than the target's bestBlock will result in a catchup request.
        broadcastBlocks(test.getPbftPeerHandlers().get(PROXY), 700, 0);
        //If the height of the requested block is higher than the target's bestBlock will result in a catchup request.
        syncBlock(node, test.getPbftPeerHandlers().get(PROXY), 700);
    }

    private static void tooMuchFindPeersRequest(TestNode n, Peer to, Peer target, int cnt) {
        IntStream.range(0, cnt).forEach(i -> findPeers(n, to, target));
        /*
        //Example
        TestNode proxyTestNode = test.getProxyTestNode();
        proxyTestNode.bootstrapping();
        ping(test.getDiscoveryHandlers().get(PROXY), node.getPeer());
        broadcastTxs(test.getPbftPeerHandlers().get(PROXY), 2);
        findPeers(node, test.getPeers().get(PROXY), test.getPeers().get(GENERAL));
        findPeers(test.getDiscoveryHandlers().get(GENERAL), node.getPeer());
        findPeers(test.getDiscoveryHandlers().get(PROXY), node.getPeer());
        syncBlock(node, test.getPbftPeerHandlers().get(PROXY));
        syncBlock(test.getPbftPeerHandlers().get(PROXY), 30);
        syncTx(node, test.getPbftPeerHandlers().get(PROXY));
        syncTx(test.getPbftPeerHandlers().get(PROXY));
        */
    }
}