/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.e2e;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.core.blockchain.BranchEventListener;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.gateway.dto.TransactionDto;
import io.yggdrash.node.ContractDemoClientUtils;
import io.yggdrash.node.YggdrashNodeApp;
import io.yggdrash.node.api.BlockApi;
import io.yggdrash.node.api.ContractApi;
import io.yggdrash.node.api.ContractApiImplTest;
import io.yggdrash.node.api.JsonRpcConfig;
import io.yggdrash.node.api.TransactionApi;
import io.yggdrash.proto.PbftProto;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = YggdrashNodeApp.class, webEnvironment = RANDOM_PORT,
        properties = {"yggdrash.node.chain.gen=true"})
@ActiveProfiles("debug")
public class YeedContractE2ETest extends TestConstants.SlowTest {

    private static final Logger log = LoggerFactory.getLogger(YeedContractE2ETest.class);

    private static BranchId branchId = TestConstants.yggdrash();
    private static Wallet wallet = ContractDemoClientUtils.getWallet();

    private BlockChain bc;
    private BlockChainManager mgr;

    private TransactionApi txJsonRpc;
    private ContractApi contractJsonRpc;
    private BlockApi blockJsonRpc;

    @LocalServerPort
    private int randomServerPort;

    @Autowired
    private BranchGroup branchGroup;

    @BeforeClass
    public static void init() throws Exception {
        // copy contract to yggdrash-node/.yggdrash/contract
        File srcDir = new File("../resources");
        File destDir = new File(".yggdrash");
        FileUtils.copyDirectory(srcDir, destDir);
    }

    @Before
    public void setUp() {
        String server = String.format("http://localhost:%d/api", randomServerPort);
        txJsonRpc = new JsonRpcConfig().proxyOf(server, TransactionApi.class);
        contractJsonRpc = new JsonRpcConfig().proxyOf(server, ContractApi.class);
        blockJsonRpc = new JsonRpcConfig().proxyOf(server, BlockApi.class);

        bc = branchGroup.getBranch(branchId);
        mgr = bc.getBlockChainManager();
    }

    @Test
    public void addBlockWithoutSendTransaction() {
        ConsensusBlock<PbftProto.PbftBlock> genesis = BlockChainTestUtils.genesisBlock();

        assertEquals(1, mgr.countOfBlocks());
        assertEquals(genesis.getBody().getTransactionList().size(), mgr.countOfTxs());
        assertEquals(genesis.getBody().getTransactionList().size(), mgr.getRecentTxs().size());

        ConsensusBlock<PbftProto.PbftBlock> block = BlockChainTestUtils.createNextBlock(createTxs(10), genesis);

        bc.addBlock(block, false);

        assertEquals(2, mgr.countOfBlocks());
        assertEquals(13, bc.getBlockChainManager().countOfTxs());
        assertEquals(13, bc.getBlockChainManager().getRecentTxs().size());
        assertEquals(0, bc.getBlockChainManager().getUnconfirmedTxs().size());

        block.getBody().getTransactionList().forEach(tx -> assertTrue(bc.getBlockChainManager().contains(tx)));
    }

    @Test
    public void sendUnExecutableTxs() {
        List<Transaction> txs = new ArrayList<>();
        txs.addAll(createTxs(200));
        txs.addAll(createInvalidFormatTxs(100));
        txs.addAll(createInvalidMethodTxs(100));

        txs.forEach(tx -> txJsonRpc.sendTransaction(TransactionDto.createBy(tx)));

        Utils.sleep(10000);

        assertEquals(203, mgr.getRecentTxs().size());
        assertEquals(203, mgr.countOfTxs());
        assertEquals(0, mgr.getUnconfirmedTxs().size());
        BigInteger frontierExpected = new BigInteger("1000000000000000000000");
        frontierExpected = frontierExpected.subtract(BigInteger.valueOf(200));
        assertEquals(frontierExpected, balanceOf(wallet.getHexAddress()));
        assertEquals(BigInteger.valueOf(200), balanceOf(TestConstants.TRANSFER_TO));
    }

    @Test
    public void shouldGetFrontierBalance() {
        log.debug("Wallet Address is {}", wallet.getHexAddress());
        // act
        BigInteger balance = balanceOf(wallet.getHexAddress());

        // assert
        assertEquals(new BigInteger("1000000000000000000000"), balance);
    }

    @Test
    public void shouldTransferredYeed() {
        int txSendCount = 300;
        List<Transaction> txs = createTxs(txSendCount);

        // arrange
        bc.addListener(new BranchEventListener() {
            @Override
            public void chainedBlock(ConsensusBlock block) {
                assertNotNull(block);
                log.debug("The txs size of chained block : {}", block.getBlock().getBody().getTransactionList().size());
                log.debug("The last index : {}", mgr.getLastIndex());
            }

            @Override
            public void receivedTransaction(Transaction tx) {
                assertNotNull(tx);
                assertTrue(txs.contains(tx));
            }
        });

        txs.forEach(tx -> txJsonRpc.sendTransaction(TransactionDto.createBy(tx)));

        Utils.sleep(10000);

        assertEquals(mgr.getLastIndex(), blockJsonRpc.blockNumber(branchId.toString()));
        assertEquals(303, mgr.getRecentTxs().size());
        assertEquals(303, mgr.countOfTxs());
        assertEquals(0, mgr.getUnconfirmedTxs().size());

        // assert
        BigInteger frontierExpected = new BigInteger("1000000000000000000000");
        frontierExpected = frontierExpected.subtract(BigInteger.valueOf(txSendCount));

        assertEquals(frontierExpected, balanceOf(wallet.getHexAddress()));
        assertEquals(BigInteger.valueOf(txSendCount), balanceOf(TestConstants.TRANSFER_TO));
    }

    private List<Transaction> createTxs(int cnt) {
        return IntStream.range(0, cnt)
                .mapToObj(i -> ContractTestUtils.transferTxBodyJson(TestConstants.TRANSFER_TO, BigInteger.ONE))
                .map(txBody -> BlockChainTestUtils.buildTx(txBody, wallet, branchId))
                .collect(Collectors.toList());
    }

    private List<Transaction> createInvalidFormatTxs(int cnt) {
        List<Transaction> txs = new ArrayList<>();
        IntStream.range(0, cnt)
                .mapToObj(i -> BlockChainTestUtils.createInvalidTransferTx())
                .forEach(txs::add);
        return txs;
    }

    private List<Transaction> createInvalidMethodTxs(int cnt) {
        return IntStream.range(0, cnt)
                .mapToObj(i -> ContractTestUtils.invalidMethodTransferTxBodyJson(
                        TestConstants.TRANSFER_TO, BigInteger.ONE))
                .map(txBody -> BlockChainTestUtils.buildTx(txBody, wallet, branchId))
                .collect(Collectors.toList());
    }

    private BigInteger balanceOf(String address) {
        Map params = ContractApiImplTest.createParams("address", address);
        return (BigInteger) contractJsonRpc.query(branchId.toString(),
                TestConstants.YEED_CONTRACT.toString(), "balanceOf", params);
    }
}
