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
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBuilder;
import io.yggdrash.core.consensus.ConsensusBlock;
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
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

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
    private ContractVersion contractVersion = ContractVersion.of("748c36013fd609409827744eee8279fd2baa042f");
    private ContractStore contractStore;
    private ContractManager manager;
    private ContractExecutor executor;
    private String namespace;

    @Before
    public void setUp() throws Exception {
        String path = Objects.requireNonNull(getClass().getClassLoader()
                .getResource("keys/dea328146c7248231a5bcafdeea12019a2f5dc58.json")).getPath();
        this.wallet = new Wallet(path, "Aa1234567890!");

        generateGenesisBlock();
        buildExecutor();
        createBundle();
        initGenesis(); //alloc process (executeTxs)
    }

    @Test
    public void executeTxTest() {
        // success tx
        Transaction tx = generateTx(100); //method => transfer
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

        long amount = Long.valueOf("10000000");
        Transaction errTx = generateTx(amount); // insufficient funds of the sender
        res = manager.executeTx(errTx);

        assertEquals(ExecuteStatus.ERROR, res.getReceipt().getStatus());
        assertEquals(0, res.getChangeValues().size());

        //tx not yet committed
        assertEquals(0, contractStore.getTmpStateStore().changeValues().size()); //revert after checkTx
        assertEquals(10, contractStore.getStateStore().getStateSize()); //same with origin state
        assertFalse(contractStore.getTransactionReceiptStore().contains(tx.getHash().toString()));
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

        ContractPolicyLoader loader = new ContractPolicyLoader();
        this.manager = ContractManagerBuilder.newInstance()
                .withFrameworkFactory(loader.getFrameworkFactory())
                .withContractManagerConfig(loader.getContractManagerConfig())
                .withBranchId(branchId.toString())
                .withContractStore(contractStore)
                .withConfig(config)
                .build();
        this.executor = manager.getContractExecutor();

    }

    private void createBundle() throws Exception {
        String contractVersion = "748c36013fd609409827744eee8279fd2baa042f"; //coinContract
        String filePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource(String.format("contracts/%s.jar", contractVersion))).getFile();
        File coinContractFile = new File(filePath);

        assert coinContractFile.exists();

        if (!checkExistContract(contractVersion)) {
            long bundle = manager.installContract(ContractVersion.of(contractVersion), coinContractFile, true);
        } else {
            manager.reloadInject();
        }

        for (ContractStatus cs : manager.searchContracts()) {
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

    private void generateGenesisBlock() {
        String filePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource("branch-coin.json")).getFile();
        File coinBranchFile = new File(filePath);
        this.genesisBlock = BlockChainTestUtils.genesisBlock(coinBranchFile);
        this.branchId = genesisBlock.getBranchId();
        this.genesisTx = genesisBlock.getBody().getTransactionList().get(0);
    }

    private void initGenesis() {
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
        manager.commitBlockResult(res);

        TransactionReceiptStore receiptStore = contractStore.getTransactionReceiptStore();
        for (TransactionReceipt receipt : res.getTxReceipts()) {
            assertTrue(receiptStore.contains(receipt.getTxId()));
        }

        StateStore stateStore = contractStore.getStateStore();
        assertEquals(10, stateStore.getStateSize());

        for (String alloc : genesisTx.getBody().getBody().getAsJsonObject("params").getAsJsonObject("alloc").keySet()) {
            stateStore.contains(alloc);
        }

        assertEquals(0, contractStore.getTmpStateStore().changeValues().size());
    }

    private Transaction generateTx(long amount) {
        JsonObject txBody = ContractTestUtils.transferTxBodyJson(TestConstants.TRANSFER_TO, amount, contractVersion);
        TransactionBuilder builder = new TransactionBuilder();
        return builder.setTxBody(txBody).setWallet(wallet).setBranchId(branchId).build();
    }

    private String getNamespaceKey(String key) {
        return String.format("%s%s", namespace, key);
    }
}
