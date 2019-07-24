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
import io.yggdrash.contract.core.TransactionReceipt;
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
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.PbftProto;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private ContractVersion contractVersion = ContractVersion.of("8c65bc05e107aab9ceaa872bbbb2d96d57811de4");
    private ContractStore contractStore;
    private ContractManager manager;
    private ContractExecutor executor;
    private String namespace;

    private GenesisBlock genesis;

    @Before
    public void setUp() throws Exception {
        String path = Objects.requireNonNull(getClass().getClassLoader()
                .getResource("keys/dea328146c7248231a5bcafdeea12019a2f5dc58.json")).getPath();
        this.wallet = new Wallet(path, "Aa1234567890!");

        generateGenesisBlock();
        buildExecutor();
//        createBundle();
        initGenesis(); //alloc process (executeTxs)
    }

    @Test
    public void executeTxTest() {
        //success tx
        Transaction tx = generateTx(BigInteger.valueOf(100)); //method => transfer
        TransactionRuntimeResult res = manager.executeTx(tx); //executeTx -> invoke -> callContractMethod

        assertEquals(ExecuteStatus.SUCCESS, res.getReceipt().getStatus());
        assertEquals(2, res.getChangeValues().size());
        assertEquals("100",
                res.getChangeValues().get(getNamespaceKey(TestConstants.TRANSFER_TO)).get(BALANCE).getAsString());
        assertEquals("999900",
                res.getChangeValues().get(getNamespaceKey(tx.getAddress().toString())).get(BALANCE).getAsString());

        //tx not yet committed
        assertEquals(0, contractStore.getTmpStateStore().changeValues().size()); //revert after checkTx
        assertEquals(10, contractStore.getStateStore().getStateSize()); //same with origin state
        assertFalse(contractStore.getTransactionReceiptStore().contains(tx.getHash().toString()));

        //error tx [insufficient funds of the sender]
        Transaction errTx = generateTx(BigInteger.valueOf(10000000));
        res = manager.executeTx(errTx);

        assertEquals(ExecuteStatus.ERROR, res.getReceipt().getStatus());
        assertEquals(0, res.getChangeValues().size());

        //tx not yet committed
        assertEquals(0, contractStore.getTmpStateStore().changeValues().size()); //revert after checkTx
        assertEquals(10, contractStore.getStateStore().getStateSize()); //same with origin state
        assertFalse(contractStore.getTransactionReceiptStore().contains(tx.getHash().toString()));
    }

    @Test
    public void executeTxsExceptionTest1() {
        // Block contains same success txs
        List<Transaction> txs = IntStream.range(0, 10)
                .mapToObj(i -> generateTx(BigInteger.valueOf(100))).collect(Collectors.toList());
        ConsensusBlock<PbftProto.PbftBlock> nextBlock = BlockChainTestUtils.createNextBlock(wallet, txs, genesisBlock);
        BlockRuntimeResult res = manager.executeTxs(nextBlock);

        res.getTxReceipts().forEach(r -> assertEquals(ExecuteStatus.SUCCESS, r.getStatus()));
        assertEquals("1000",
                res.getBlockResult().get(
                        getNamespaceKey(TestConstants.TRANSFER_TO)).get(BALANCE).getAsString());
        assertEquals("999000",
                res.getBlockResult().get(
                        getNamespaceKey(txs.get(0).getAddress().toString())).get(BALANCE).getAsString());
        assertEquals(10, res.getTxReceipts().size());
        assertEquals(2, res.getBlockResult().size());

        //tx not yet committed
        assertEquals(0, contractStore.getTmpStateStore().changeValues().size()); //revert after checkTx
        assertEquals(10, contractStore.getStateStore().getStateSize()); //same with origin state
        assertFalse(contractStore.getTransactionReceiptStore().contains(txs.get(0).getHash().toString()));

        manager.commitBlockResult(res);

        assertEquals(20, manager.getCurLogIndex());
        assertEquals(0, contractStore.getTmpStateStore().changeValues().size()); //revert after checkTx
        //changed values have been updated (issuer had been allocated coin when initializing genesis)
        assertEquals(11, contractStore.getStateStore().getStateSize());

        txs.stream().map(tx -> contractStore.getTransactionReceiptStore() //tx hashes have been stored in receiptStore
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

        ConsensusBlock<PbftProto.PbftBlock> nextBlock = BlockChainTestUtils.createNextBlock(wallet, txs, genesisBlock);
        BlockRuntimeResult res = manager.executeTxs(nextBlock);

        assertEquals(ExecuteStatus.SUCCESS, res.getTxReceipts().get(0).getStatus());
        assertEquals(ExecuteStatus.ERROR, res.getTxReceipts().get(1).getStatus());
        assertEquals(ExecuteStatus.ERROR, res.getTxReceipts().get(2).getStatus());
        assertEquals(ExecuteStatus.SUCCESS, res.getTxReceipts().get(3).getStatus());

        assertEquals("200",
                res.getBlockResult().get(
                        getNamespaceKey(TestConstants.TRANSFER_TO)).get(BALANCE).getAsString());
        assertEquals("999800",
                res.getBlockResult().get(
                        getNamespaceKey(txs.get(0).getAddress().toString())).get(BALANCE).getAsString());

        assertTrue(res.getTxReceipts().get(1).getTxLog().contains("Error Code:34002, Msg:Insufficient funds"));
    }

    @Test
    public void executeTxsTest() {
        //error tx [contract is not exist]
        ContractVersion notExistedVersion = ContractVersion.of(
                Hex.encodeHexString("Wrong ContractVersion".getBytes()));
        Transaction errTx = generateTx(BigInteger.valueOf(100), notExistedVersion);

        ConsensusBlock<PbftProto.PbftBlock> nextBlock = BlockChainTestUtils.createNextBlock(
                wallet, Collections.singletonList(errTx), genesisBlock);

        String errLog = SystemError.CONTRACT_VERSION_NOT_FOUND.toString();
        BlockRuntimeResult res = manager.executeTxs(nextBlock);

        assertEquals(ExecuteStatus.ERROR, res.getTxReceipts().get(0).getStatus());

        TransactionReceipt errReceipt = res.getTxReceipts().get(0);

        assertTrue(errReceipt.getTxLog().contains(errLog));
        assertEquals(errTx.getHash().toString(), errReceipt.getTxId());
        assertEquals(errTx.getAddress().toString(), errReceipt.getIssuer());
        assertEquals(nextBlock.getHash().toString(), errReceipt.getBlockId());
        assertEquals(branchId.toString(), errReceipt.getBranchId());
        assertEquals(1, errReceipt.getBlockHeight().longValue());
        assertEquals(1, res.getTxReceipts().size());
        assertEquals(0, res.getBlockResult().size());

        manager.commitBlockResult(res);

        assertEquals(11, manager.getCurLogIndex());
        assertEquals(0, contractStore.getTmpStateStore().changeValues().size());
        //TransactionReceiptStore contains errorReceipt
        assertTrue(contractStore.getTransactionReceiptStore().contains(errTx.getHash().toString()));

        //success tx
        Transaction tx = generateTx(BigInteger.valueOf(100)); //method => transfer
        nextBlock = BlockChainTestUtils.createNextBlock(wallet, Collections.singletonList(tx), genesisBlock);
        res = manager.executeTxs(nextBlock);

        manager.commitBlockResult(res);

        assertEquals(12, manager.getCurLogIndex());
        assertEquals(ExecuteStatus.SUCCESS, res.getTxReceipts().get(0).getStatus());
        assertEquals(2, res.getBlockResult().size());
        assertEquals(0, contractStore.getTmpStateStore().changeValues().size());
        assertTrue(contractStore.getTransactionReceiptStore().contains(errTx.getHash().toString()));
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
        BundleService bundleService = new BundleServiceImpl();

        SystemProperties systemProperties = BlockChainTestUtils.createDefaultSystemProperties();

        this.manager = ContractManagerBuilder.newInstance()
                .withGenesis(genesis)
                .withBootFramework(bootFrameworkLauncher)
                .withBundleManager(bundleService)
                .withDefaultConfig(config)
                .withContractStore(contractStore)
                .withLogStore(bcStore.getLogStore()) // is this logstore for what?
                .withSystemProperties(systemProperties)
                .build();

        this.executor = manager.getContractExecutor();

        Bundle bundle = manager.getBundle(branchId.toString(), genesis.getBranch().getBranchContracts().get(0).getContractVersion());
        setNamespace(bundle.getSymbolicName());

    }

    private void createBundle() throws Exception {
        String filePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource(String.format("contracts/%s.jar", contractVersion))).getFile();
        File coinContractFile = new File(filePath);

        assert coinContractFile.exists();

//        manager.installTest(branchId.toString(), contractVersion, true);

//        if (!checkExistContract(contractVersion.toString())) {
//            long bundle = manager.installTest(contractVersion, coinContractFile, true);
//        } else {
//            manager.reloadInject();
//        }

        for (ContractStatus cs : manager.searchContracts(branchId.toString())) {
            String bundleSymbolicName = cs.getSymbolicName();
            byte[] bundleSymbolicSha3 = HashUtil.sha3omit12(bundleSymbolicName.getBytes());
            this.namespace = new String(Base64.encodeBase64(bundleSymbolicSha3));

            log.debug("Description {}", cs.getDescription());
            log.debug("Location {}", cs.getLocation());
            log.debug("SymbolicName {}", cs.getSymbolicName());
            log.debug("Version {}", cs.getVersion());
            log.debug(Long.toString(cs.getId()));
        }
    }

    private boolean checkExistContract(String contractVersion) {
        for (ContractStatus cs : manager.searchContracts()) {
            if (cs.getLocation().lastIndexOf(contractVersion) > 0) {
                return true;
            }
        }
        return false;
    }

    private void generateGenesisBlock() throws IOException {
        String filePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource("branch-coin.json")).getFile();
        File coinBranchFile = new File(filePath);
        this.genesis = GenesisBlock.of(new FileInputStream(coinBranchFile));
        this.genesisBlock = BlockChainTestUtils.genesisBlock(coinBranchFile);
        this.branchId = genesisBlock.getBranchId();
        this.genesisTx = genesisBlock.getBody().getTransactionList().get(0);
    }

    private void initGenesis() {

        List<Transaction> txList = genesisBlock.getBody().getTransactionList();
        Map<String, Object> serviceMap = manager.getServiceMap();
        String contractVersion = txList.get(0).getBody().getBody().get("contractVersion").getAsString();
        Object service = serviceMap.get(contractVersion);

        assertEquals(1 ,txList.size());
        assertEquals(1, serviceMap.size());
        assert service != null;

        BlockRuntimeResult res = manager.executeTxs(genesisBlock);
        TransactionReceipt receipt = res.getTxReceipts().get(0);

        assertEquals(0, contractStore.getTmpStateStore().changeValues().size());
        assertEquals(10, res.getBlockResult().size());
        assertEquals(Long.valueOf(0), receipt.getBlockHeight());
        assertEquals(Long.valueOf(genesisTx.getBody().getLength()), receipt.getTxSize());
        assertEquals(ExecuteStatus.SUCCESS, receipt.getStatus());
        assertEquals(genesisTx.getHash().toString(), receipt.getTxId());
        assertEquals(genesisBlock.getBranchId().toString(), receipt.getBranchId());
        assertEquals(contractVersion.toString(), receipt.getContractVersion());
        assertEquals("0000000000000000000000000000000000000000", receipt.getIssuer());

        commitBlockResult(res);
    }

    private void commitBlockResult(BlockRuntimeResult res) {
        log.debug("commitBlockResult : blockResultSize = {}", res.getBlockResult().size());
        manager.commitBlockResult(res);

        TransactionReceiptStore receiptStore = contractStore.getTransactionReceiptStore();
        for (TransactionReceipt receipt : res.getTxReceipts()) {
            log.debug("commitBlockResult : txHash = {}, logSize = {}", receipt.getTxId(), receipt.getTxLog().size());
            IntStream.range(0, receipt.getTxLog().size()).forEach(
                    i -> assertEquals(receipt.getTxLog().get(i), manager.getLog(i)));
            receipt.getTxLog().containsAll(manager.getLogs(0, receipt.getTxLog().size()));
            assertTrue(receiptStore.contains(receipt.getTxId()));
        }

        StateStore stateStore = contractStore.getStateStore();
        assertEquals(10, stateStore.getStateSize());

        for (String alloc : genesisTx.getBody().getBody().getAsJsonObject("params").getAsJsonObject("alloc").keySet()) {
            stateStore.contains(alloc);
        }

        assertEquals(0, contractStore.getTmpStateStore().changeValues().size());
        assertEquals(10, manager.getCurLogIndex());
    }

    private Transaction generateTx(BigInteger amount) {
        return generateTx(amount, contractVersion);
    }

    private Transaction generateTx(BigInteger amount, ContractVersion contractVersion) {
        JsonObject txBody = ContractTestUtils.transferTxBodyJson(TestConstants.TRANSFER_TO, amount, contractVersion);
        TransactionBuilder builder = new TransactionBuilder();
        return builder.setTxBody(txBody).setWallet(wallet).setBranchId(branchId).build();
    }

    private void setNamespace(String symbolicName) {
        byte[] bundleSymbolicSha3 = HashUtil.sha3omit12(symbolicName.getBytes());
        this.namespace = new String(Base64.encodeBase64(bundleSymbolicSha3));
        log.debug("bundleSymbolicName {} , nameSpace {}", symbolicName, this.namespace);

    }

    private String getNamespaceKey(String key) {
        return String.format("%s%s", namespace, key);
    }
}
