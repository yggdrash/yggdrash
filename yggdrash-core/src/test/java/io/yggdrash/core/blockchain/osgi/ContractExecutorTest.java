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

package io.yggdrash.core.blockchain.osgi;

import com.google.gson.JsonObject;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.crypto.HashUtil;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBuilder;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.framework.BootFrameworkConfig;
import io.yggdrash.core.blockchain.osgi.framework.BootFrameworkLauncher;
import io.yggdrash.core.blockchain.osgi.framework.BundleService;
import io.yggdrash.core.blockchain.osgi.framework.BundleServiceImpl;
import io.yggdrash.core.blockchain.osgi.framework.FrameworkConfig;
import io.yggdrash.core.blockchain.osgi.framework.FrameworkLauncher;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.errorcode.SystemError;
import io.yggdrash.core.runtime.result.BlockRuntimeResult;
import io.yggdrash.core.runtime.result.TransactionRuntimeResult;
import io.yggdrash.core.store.BlockChainStore;
import io.yggdrash.core.store.BlockChainStoreBuilder;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.PbftBlockStoreMock;
import io.yggdrash.core.store.ReceiptStore;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.PbftProto;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ContractExecutorTest {

    private static final Logger log = LoggerFactory.getLogger(ContractExecutorTest.class);
    private static final String BALANCE = "balance";

    private Wallet wallet;
    private ConsensusBlock<PbftProto.PbftBlock> genesisBlock;
    private Transaction genesisTx;
    private BranchId branchId;
    private ContractVersion contractVersion = ContractVersion.of("a88ae404e837cd1d6e8b9a5a91f188da835ccb56");
    private ContractStore contractStore;
    private ContractManager manager;
    private ContractExecutor executor;
    private String namespace;

    private GenesisBlock genesis;

    private static String VERSIONING_TX = "0000000000000001";

    @Before
    public void setUp() throws Exception {
        this.wallet = ContractTestUtils.createTestWallet("dea328146c7248231a5bcafdeea12019a2f5dc58.json");
        this.genesis = ContractTestUtils.createGenesis("branch-coin.json");
        this.genesisBlock = ContractTestUtils.createGenesisBlock("branch-coin.json");
        this.genesisTx = genesisBlock.getBody().getTransactionList().get(0); //txSize == 1
        this.branchId = genesisBlock.getBranchId();
        Map<ContractManager, ContractStore> map = ContractTestUtils.createContractManager(genesis);
        this.manager = map.keySet().stream().findFirst().get();
        this.namespace = ContractTestUtils.setNamespace(manager, contractVersion);
        this.contractStore = map.values().stream().findFirst().get();

        initGenesis(); //alloc process (executeTxs)
    }

    @Test
    public void executeTxTest() {
        //success tx
        Transaction tx = generateTx(BigInteger.valueOf(100)); //method => transfer
        TransactionRuntimeResult res = manager.executeTx(tx); //executeTx -> invoke -> callContractMethod

        assertEquals(ExecuteStatus.SUCCESS, res.getReceipt().getStatus());
        assertEquals(3, res.getChangeValues().size());
        assertEquals("100",
                res.getChangeValues().get(getNamespaceKey(TestConstants.TRANSFER_TO)).get(BALANCE).getAsString());
        assertEquals("999900",
                res.getChangeValues().get(getNamespaceKey(tx.getAddress().toString())).get(BALANCE).getAsString());

        //tx not yet committed
        assertEquals(0, contractStore.getTmpStateStore().changeValues().size()); //revert after checkTx
        assertFalse(contractStore.getReceiptStore().contains(tx.getHash().toString()));

        //error tx [insufficient funds of the sender]
        Transaction errTx = generateTx(BigInteger.valueOf(10000000));
        res = manager.executeTx(errTx);

        assertEquals(ExecuteStatus.ERROR, res.getReceipt().getStatus());
        assertEquals(0, res.getChangeValues().size());

        //tx not yet committed
        assertEquals(0, contractStore.getTmpStateStore().changeValues().size()); //revert after checkTx
        assertFalse(contractStore.getReceiptStore().contains(tx.getHash().toString()));
    }

    @Test
    public void doesNotReflectChangedStateOfErrorTx() {
        /*
        Burn function added to coinContract for testing and the business logic is not suitable for actually using.

        The purpose of this test is to ensure that if the result of the transaction is an Error,
        the contract is executed and does not reflect the changed state.

        If the result of the transaction is a failure,
        the changed status value will be saved unlike in the case of an error.

        [Test Scenario]
        The balance of issuer is 10000000.
        It returns an error if the amount of fee is greater than issuer balance.
        ContractExecutor should update the state except for the changed status value in case of error.
        */

        // Create a transaction list includes success and error ones.
        List<Transaction> txList = new ArrayList<>();

        // Add success transactions and error transactions
        txList.add(createBurnTx("100"));
        txList.add(createBurnTx("10000000"));
        txList.add(createBurnTx("100"));
        txList.add(createBurnTx("10000000"));

        // Create a block with the txList which should be added to blockChain.
        ConsensusBlock<PbftProto.PbftBlock> nextBlock = BlockChainTestUtils.createNextBlock(
                wallet, txList, genesisBlock, manager);

        // Execute the created block.
        BlockRuntimeResult res = manager.executeTxs(nextBlock);

        // Result
        assertEquals("TxReceipts size", 4, res.getReceipts().size());
        assertEquals("TxReceipt status", ExecuteStatus.SUCCESS, res.getReceipts().get(0).getStatus());
        assertEquals("TxReceipt status", ExecuteStatus.ERROR, res.getReceipts().get(1).getStatus());
        assertEquals("TxReceipt status", ExecuteStatus.SUCCESS, res.getReceipts().get(2).getStatus());
        assertEquals("TxReceipt status", ExecuteStatus.ERROR, res.getReceipts().get(3).getStatus());
        assertEquals("Tx blockHeight", Long.valueOf(1), res.getReceipts().get(0).getBlockHeight());

        Map<String, JsonObject> blockResult = res.getBlockResult();
        String issuerBalance = blockResult.get(getNamespaceKey(wallet.getHexAddress())).get("balance").getAsString();
        String totalBalance = blockResult.get(getNamespaceKey("TOTAL_SUPPLY")).get("balance").getAsString();

        assertEquals("999800", issuerBalance);
        assertEquals("1993999999999999999999700", totalBalance);
    }

    private Transaction createBurnTx(String fee) {
        JsonObject params = new JsonObject();
        params.addProperty("amount", "100");
        params.addProperty("fee", fee);

        JsonObject txBody = ContractTestUtils.txBodyJson(contractVersion, "burn", params, false);

        return new TransactionBuilder().setTxBody(txBody).setWallet(wallet).setBranchId(branchId).build();
    }

    @Test
    public void executeTxsExceptionTest1() {
        // Block contains same success txs
        List<Transaction> txs = IntStream.range(0, 10)
                .mapToObj(i -> generateTx(BigInteger.valueOf(100))).collect(Collectors.toList());
        ConsensusBlock<PbftProto.PbftBlock> nextBlock = BlockChainTestUtils.createNextBlock(
                wallet, txs, genesisBlock, manager);
        BlockRuntimeResult res = manager.executeTxs(nextBlock); // executeTxs contains commitBlockResult
        manager.commitBlockResult(res);

        res.getReceipts().forEach(r -> assertEquals(ExecuteStatus.SUCCESS, r.getStatus()));
        assertEquals("1000",
                res.getBlockResult().get(
                        getNamespaceKey(TestConstants.TRANSFER_TO)).get(BALANCE).getAsString());
        assertEquals("999000",
                res.getBlockResult().get(
                        getNamespaceKey(txs.get(0).getAddress().toString())).get(BALANCE).getAsString());
        assertEquals(10, res.getReceipts().size());
        assertEquals(3, res.getBlockResult().size()); // contains stateRoot

        //tx not yet committed
        assertEquals(0, contractStore.getTmpStateStore().changeValues().size()); //revert after checkTx
        assertTrue(contractStore.getReceiptStore().contains(txs.get(0).getHash().toString()));

        assertEquals(19, manager.getCurLogIndex());
        assertEquals(0, contractStore.getTmpStateStore().changeValues().size()); //revert after checkTx

        txs.stream().map(tx -> contractStore.getReceiptStore() //tx hashes have been stored in receiptStore
                .contains(tx.getHash().toString()))
                .forEach(Assert::assertTrue);
    }

    @Test
    public void executeTxsExceptionTest2() {
        // Block contains both successTxs and errorTx
        Transaction successTx1 = generateTx(BigInteger.valueOf(100));
        Transaction successTx2 = generateTx(BigInteger.valueOf(100));
        Transaction errTx1 = generateTx(BigInteger.valueOf(10000000)); //[insufficient funds of the sender]
        Transaction errTx2 = generateTx(BigInteger.valueOf(100), ContractVersion.of(
                Hex.encodeHexString("Wrong ContractVersion".getBytes()))); //[contract is not exist]

        List<Transaction> txs = new ArrayList<>();
        txs.add(successTx1);
        txs.add(errTx1);
        txs.add(errTx2);
        txs.add(successTx2);

        ConsensusBlock<PbftProto.PbftBlock> nextBlock = BlockChainTestUtils.createNextBlock(
                wallet, txs, genesisBlock, manager);
        BlockRuntimeResult res = manager.executeTxs(nextBlock);

        assertEquals(ExecuteStatus.SUCCESS, res.getReceipts().get(0).getStatus());
        assertEquals(ExecuteStatus.ERROR, res.getReceipts().get(1).getStatus());
        assertEquals(ExecuteStatus.ERROR, res.getReceipts().get(2).getStatus());
        assertEquals(ExecuteStatus.SUCCESS, res.getReceipts().get(3).getStatus());

        assertEquals("200",
                res.getBlockResult().get(
                        getNamespaceKey(TestConstants.TRANSFER_TO)).get(BALANCE).getAsString());
        assertEquals("999800",
                res.getBlockResult().get(
                        getNamespaceKey(txs.get(0).getAddress().toString())).get(BALANCE).getAsString());

        assertTrue(res.getReceipts().get(1).getLog().contains("Insufficient funds"));
        assertTrue(res.getReceipts().get(2).getLog().contains(SystemError.CONTRACT_VERSION_NOT_FOUND.toString()));
    }

    @Test
    public void executeTxsTest() {
        //error tx [contract is not exist]
        ContractVersion notExistedVersion = ContractVersion.of(
                Hex.encodeHexString("Wrong ContractVersion".getBytes()));
        Transaction errTx = generateTx(BigInteger.valueOf(100), notExistedVersion);

        ConsensusBlock<PbftProto.PbftBlock> nextBlock = BlockChainTestUtils.createNextBlock(
                wallet, Collections.singletonList(errTx), genesisBlock, manager);

        String errLog = SystemError.CONTRACT_VERSION_NOT_FOUND.toString();
        BlockRuntimeResult res = manager.executeTxs(nextBlock);
        manager.commitBlockResult(res);

        assertEquals(ExecuteStatus.ERROR, res.getReceipts().get(0).getStatus());

        Receipt errReceipt = res.getReceipts().get(0);

        assertTrue(errReceipt.getLog().contains(errLog));
        assertEquals(errTx.getHash().toString(), errReceipt.getTxId());
        assertEquals(errTx.getAddress().toString(), errReceipt.getIssuer());
        assertEquals(branchId.toString(), errReceipt.getBranchId());
        assertEquals(1, errReceipt.getBlockHeight().longValue());
        assertEquals(1, res.getReceipts().size());
        assertEquals(0, res.getBlockResult().size());

        assertEquals(9, manager.getCurLogIndex()); // errTx is not added
        assertEquals(0, contractStore.getTmpStateStore().changeValues().size());
        //ReceiptStore contains errorReceipt
        assertFalse(contractStore.getReceiptStore().contains(errTx.getHash().toString()));

        //success tx
        Transaction tx = generateTx(BigInteger.valueOf(100)); //method => transfer
        nextBlock = BlockChainTestUtils.createNextBlock(wallet, Collections.singletonList(tx), genesisBlock, manager);
        res = manager.executeTxs(nextBlock);
        manager.commitBlockResult(res);

        assertEquals(10, manager.getCurLogIndex());
        assertEquals(ExecuteStatus.SUCCESS, res.getReceipts().get(0).getStatus());
        assertEquals(3, res.getBlockResult().size());
        assertEquals(0, contractStore.getTmpStateStore().changeValues().size());
        assertFalse(contractStore.getReceiptStore().contains(errTx.getHash().toString()));
    }

    @Test
    public void executeVersionProposalTest() throws DecoderException {
        JsonObject txBody = ContractTestUtils.contractProposeTxBodyJson(contractVersion.toString(), "activate");

        Transaction tx = new TransactionBuilder()
                .setType(Hex.decodeHex(VERSIONING_TX))
                .setTxBody(txBody)
                .setWallet(wallet)
                .setBranchId(branchId)
                .build();

        TransactionRuntimeResult result = manager.executeTx(tx);

        Assert.assertNotNull("TransactionRuntimeResult is null", result);

        // Validator set not found in Branch Store.
        Assert.assertThat(result.getReceipt().getStatus(), CoreMatchers.is(ExecuteStatus.FALSE));
        Assert.assertThat(result.getReceipt().getLog().get(0), CoreMatchers.is("Validator verification failed"));

    }

    @Test
    public void executeVersionVoteTest() throws DecoderException {
        String txId = "34eec4dcb662e54492e3b69adb1d2dce5d7451ca6d22221c38ce5bc6f8871b51";
        JsonObject txBody = ContractTestUtils.contractVoteTxBodyJson(txId, true);
        Transaction tx = new TransactionBuilder()
                .setType(Hex.decodeHex(VERSIONING_TX))
                .setTxBody(txBody)
                .setWallet(wallet)
                .setBranchId(branchId)
                .build();

        TransactionRuntimeResult result = manager.executeTx(tx);

        Assert.assertNotNull("TransactionRuntimeResult is null", result);
        // UpdateProposal TX doesn't exist.
        Assert.assertThat(result.getReceipt().getStatus(), CoreMatchers.is(ExecuteStatus.FALSE));

    }

    private void buildExecutor() {
        DefaultConfig config = new DefaultConfig();
        BlockChainStore bcStore = BlockChainStoreBuilder.newBuilder(branchId)
                .withDataBasePath(config.getDatabasePath())
                .withProductionMode(config.isProductionMode())
                .setConsensusAlgorithm(null)
                .setBlockStoreFactory(PbftBlockStoreMock::new)
                .build();
        this.contractStore = bcStore.getContractStore();

        FrameworkConfig bootFrameworkConfig = new BootFrameworkConfig(config, branchId);
        FrameworkLauncher bootFrameworkLauncher = new BootFrameworkLauncher(bootFrameworkConfig);
        BundleService bundleService = new BundleServiceImpl(bootFrameworkLauncher.getBundleContext());

        SystemProperties systemProperties = BlockChainTestUtils.createDefaultSystemProperties();

        assert branchId.equals(genesis.getBranchId());

        this.manager = ContractManagerBuilder.newInstance()
                .withGenesis(genesis)
                .withBundleManager(bundleService)
                .withDefaultConfig(config)
                .withContractStore(contractStore)
                .withLogStore(bcStore.getLogStore()) // is this logstore for what?
                .withSystemProperties(systemProperties)
                .build();

        setNamespace();

    }

    private boolean checkExistContract(String contractVersion) {
        for (ContractStatus cs : manager.searchContracts()) {
            if (cs.getLocation().lastIndexOf(contractVersion) > 0) {
                return true;
            }
        }
        return false;
    }

    private void initGenesis() {
        BlockRuntimeResult res = manager.executeTxs(genesisBlock); // contains commitBlockResult
        Receipt receipt = res.getReceipts().get(0);

        assertEquals(0, contractStore.getTmpStateStore().changeValues().size());
        assertEquals(11, res.getBlockResult().size()); // contains stateRootHash
        assertEquals(Long.valueOf(0), receipt.getBlockHeight());
        assertEquals(Long.valueOf(genesisTx.getTransactionBody().getLength()), receipt.getTxSize());
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());
        assertEquals(genesisTx.getHash().toString(), receipt.getTxId());
        assertEquals(genesisBlock.getBranchId().toString(), receipt.getBranchId());
        assertEquals(contractVersion.toString(), receipt.getContractVersion());
        assertEquals("0000000000000000000000000000000000000000", receipt.getIssuer());

        assertCommitBlockResult(res);
    }

    private void assertCommitBlockResult(BlockRuntimeResult res) {
        log.debug("commitBlockResult : blockResultSize = {}", res.getBlockResult().size());
        manager.commitBlockResult(res);

        ReceiptStore receiptStore = contractStore.getReceiptStore();
        for (Receipt receipt : res.getReceipts()) {
            log.debug("commitBlockResult : txHash = {}, logSize = {}", receipt.getTxId(), receipt.getLog().size());
            IntStream.range(0, receipt.getLog().size()).forEach(
                    i -> assertEquals(receipt.getLog().get(i), manager.getLog(i).getMsg()));
            receipt.getLog().containsAll(manager.getLogs(0, receipt.getLog().size()));
            assertTrue(receiptStore.contains(receipt.getTxId()));
        }

        StateStore stateStore = contractStore.getStateStore();


        for (String alloc : genesisTx.getTransactionBody().getBody().getAsJsonObject("params").getAsJsonObject("alloc").keySet()) {
            stateStore.contains(alloc);
        }

        assertEquals(0, contractStore.getTmpStateStore().changeValues().size());
        assertEquals(9, manager.getCurLogIndex());
    }

    private Transaction generateTx(BigInteger amount) {
        return generateTx(amount, contractVersion);
    }

    private Transaction generateTx(BigInteger amount, ContractVersion contractVersion) {
        JsonObject txBody = ContractTestUtils.transferTxBodyJson(TestConstants.TRANSFER_TO, amount, contractVersion);
        TransactionBuilder builder = new TransactionBuilder();
        return builder.setTxBody(txBody).setWallet(wallet).setBranchId(branchId).build();
    }

    private void setNamespace() {
        Bundle bundle = manager.getBundle(contractVersion);
        String name = bundle.getSymbolicName();
        byte[] bundleSymbolicSha3 = HashUtil.sha3omit12(name.getBytes());
        this.namespace = new String(Base64.encodeBase64(bundleSymbolicSha3));
        log.debug("serviceName {} , nameSpace {}", name, this.namespace);
    }

    private String getNamespaceKey(String key) {
        return String.format("%s%s", namespace, key);
    }
}
