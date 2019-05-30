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

package io.yggdrash;

import com.google.gson.JsonObject;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockChainBuilder;
import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.core.blockchain.BlockChainManagerImpl;
import io.yggdrash.core.blockchain.BlockImpl;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.PbftBlockChainMock;
import io.yggdrash.core.blockchain.PbftBlockMock;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBody;
import io.yggdrash.core.blockchain.TransactionBuilder;
import io.yggdrash.core.blockchain.TransactionHeader;
import io.yggdrash.core.blockchain.TransactionImpl;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.ContractManager;
import io.yggdrash.core.blockchain.osgi.ContractManagerBuilder;
import io.yggdrash.core.blockchain.osgi.ContractPolicyLoader;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.PbftBlockStoreMock;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.proto.PbftProto;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockChainTestUtils {
    private static GenesisBlock genesis;

    private BlockChainTestUtils() {
    }

    static {
        generateGenesisBlockByFile();
        /*
        try (InputStream is = new FileInputStream(TestConstants.branchFile)) {
            genesis = GenesisBlock.of(is);
        } catch (Exception e) {
            throw new InvalidSignatureException(e);
        }
        */
    }

    private static void generateGenesisBlockByFile() {
        generateGenesisBlockByFile(TestConstants.branchFile);
    }

    private static void generateGenesisBlockByFile(File file) {
        try (InputStream is = new FileInputStream(file)) {
            genesis = GenesisBlock.of(is);
        } catch (Exception e) {
            throw new InvalidSignatureException(e);
        }
    }

    private static List<ConsensusBlock<PbftProto.PbftBlock>> sampleBlockList = createBlockList(
            new ArrayList<>(), genesisBlock(), null, 100);

    public static List<ConsensusBlock<PbftProto.PbftBlock>> getSampleBlockList() {
        return sampleBlockList;
    }

    public static ConsensusBlock<PbftProto.PbftBlock> genesisBlock() {
        return new PbftBlockMock(genesis.getBlock());
    }

    public static ConsensusBlock<PbftProto.PbftBlock> genesisBlock(File file) {
        generateGenesisBlockByFile(file);
        return new PbftBlockMock(genesis.getBlock());
    }

    public static ConsensusBlock<PbftProto.PbftBlock> createNextBlock() {
        return createNextBlock(new PbftBlockMock(genesis.getBlock()));
    }

    public static ConsensusBlock<PbftProto.PbftBlock> createNextBlock(ConsensusBlock prevBlock) {
        return createNextBlock(Collections.emptyList(), prevBlock);
    }

    public static ConsensusBlock<PbftProto.PbftBlock> createNextBlock(List<Transaction> blockBody,
                                                                      ConsensusBlock prevBlock) {
        return new PbftBlockMock(BlockImpl.nextBlock(TestConstants.wallet(), blockBody, prevBlock));
    }

    public static Transaction createBranchTx() {
        JsonObject json = ContractTestUtils.createSampleBranchJson();

        return createBranchTx(json);
    }

    private static Transaction createBranchTx(JsonObject json) {
        TransactionBuilder builder = new TransactionBuilder();
        return builder.setTxBody(Constants.STEM_CONTRACT_VERSION, "create", json, false)
                .setWallet(TestConstants.wallet())
                .setBranchId(genesis.getBranch().getBranchId())
                .build();
    }

    public static Transaction createBranchTx(BranchId branchId, String method,
                                             JsonObject branch) {
        TransactionBuilder builder = new TransactionBuilder();

        return builder.setTxBody(Constants.STEM_CONTRACT_VERSION, method, branch, false)
                .setWallet(TestConstants.wallet())
                .setBranchId(branchId)
                .build();
    }

    public static Transaction createTx(BranchId branchId, JsonObject txBody) {
        TransactionBuilder builder = new TransactionBuilder();
        return builder.setTxBody(txBody)
                .setWallet(TestConstants.wallet())
                .setBranchId(branchId)
                .build();
    }

    public static BlockChain createBlockChain(boolean isProductionMode) {
        StoreBuilder storeBuilder;
        if (isProductionMode) {
            storeBuilder = StoreTestUtils.getProdMockBuilder();
        } else {
            storeBuilder = StoreBuilder.newBuilder().setConfig(new DefaultConfig());
        }
        storeBuilder.setBranchId(genesis.getBranch().getBranchId())
                .setBlockStoreFactory(PbftBlockStoreMock::new);

        ContractStore contractStore = storeBuilder.buildContractStore();
        ContractPolicyLoader contractPolicyLoader = new ContractPolicyLoader();

        ContractManager contractManager = ContractManagerBuilder.newInstance()
                .withFrameworkFactory(contractPolicyLoader.getFrameworkFactory())
                .withContractManagerConfig(contractPolicyLoader.getContractManagerConfig())
                .withBranchId(genesis.getBranch().getBranchId().toString())
                .withContractStore(contractStore)
                .withConfig(new DefaultConfig())
                .build();

        BlockChainManager blockChainManager = new BlockChainManagerImpl(
                storeBuilder.buildBlockStore(),
                storeBuilder.buildTransactionStore(),
                contractStore.getTransactionReceiptStore());

        return BlockChainBuilder.newBuilder()
                .setGenesis(genesis)
                .setBranchStore(contractStore.getBranchStore())
                .setBlockChainManager(blockChainManager)
                .setContractManager(contractManager)
                .setFactory(PbftBlockChainMock::new)
                .build();
    }

    public static BranchGroup createBranchGroup() {
        BranchGroup branchGroup = new BranchGroup();
        BlockChain blockChain = createBlockChain(false);
        branchGroup.addBranch(blockChain);
        return branchGroup;
    }

    public static void generateBlock(BranchGroup branchGroup, BranchId branchId) {
        BlockChain branch = branchGroup.getBranch(branchId);
        List<Transaction> txs =
                branch.getBlockChainManager().getUnconfirmedTxsWithLimit(Constants.Limit.BLOCK_SYNC_SIZE);
        branch.addBlock(createNextBlock(txs, branch.getBlockChainManager().getLastConfirmedBlock()));
    }

    public static void setBlockHeightOfBlockChain(BlockChain blockChain, int height) {
        List<ConsensusBlock<PbftProto.PbftBlock>> blockList = new ArrayList<>();
        ConsensusBlock<PbftProto.PbftBlock> curBlock
                = blockChain.getBlockChainManager().getBlockByIndex(blockChain.getBlockChainManager().getLastIndex());
        ConsensusBlock<PbftProto.PbftBlock> nextBlock = createNextBlock(curBlock);
        blockList = createBlockList(blockList, nextBlock, null, height);

        for (ConsensusBlock block : blockList) {
            blockChain.addBlock(block, false);
        }
    }

    public static List<ConsensusBlock<PbftProto.PbftBlock>> createBlockListFilledWithTx(int height, int txSize) {
        List<ConsensusBlock<PbftProto.PbftBlock>> blockList = new ArrayList<>();
        List<Transaction> blockBody = new ArrayList<>();

        for (int i = 0; i < txSize; i++) {
            blockBody.add(createTransferTx());
        }

        return createBlockList(blockList, createNextBlock(blockBody, genesisBlock()), blockBody, height);
    }

    private static List<ConsensusBlock<PbftProto.PbftBlock>> createBlockList(
                                                        List<ConsensusBlock<PbftProto.PbftBlock>> blockList,
                                                        ConsensusBlock<PbftProto.PbftBlock> prevBlock,
                                                        List<Transaction> blockBody,
                                                        int height) {
        while (blockList.size() < height) {
            blockList.add(prevBlock);
            if (blockBody != null) {
                createBlockList(blockList, createNextBlock(blockBody, prevBlock), blockBody, height);
            } else {
                createBlockList(blockList, createNextBlock(prevBlock), null, height);
            }
        }
        return blockList.size() > 0 ? blockList : Collections.emptyList();
    }

    public static Transaction createTransferTx() {
        return createTransferTx(TestConstants.TRANSFER_TO, 100);
    }

    private static Transaction createTransferTx(String to, int amount) {
        JsonObject txBody = ContractTestUtils.transferTxBodyJson(to, amount);
        TransactionBuilder builder = new TransactionBuilder();
        return builder.setTxBody(txBody)
                .setWallet(TestConstants.wallet())
                .setBranchId(TestConstants.yggdrash())
                .build();
    }

    public static Transaction createTransferTx(BranchId branchId, ContractVersion contractVersion) {
        JsonObject txBody = ContractTestUtils.transferTxBodyJson(
                TestConstants.TRANSFER_TO, 100, contractVersion);
        TransactionBuilder builder = new TransactionBuilder();
        return builder.setTxBody(txBody)
                .setWallet(TestConstants.wallet())
                .setBranchId(branchId)
                .build();
    }

    public static Transaction createInvalidTransferTx(ContractVersion contractVersion) {
        JsonObject txBody = ContractTestUtils
                .invalidTransferTxBodyJson(TestConstants.TRANSFER_TO, 100, contractVersion);
        return createInvalidTx(TestConstants.yggdrash(), Constants.EMPTY_BYTE8, txBody);
    }

    public static Transaction createInvalidTransferTx(BranchId branchId, ContractVersion contractVersion) {
        JsonObject txBody = ContractTestUtils
                .invalidTransferTxBodyJson(TestConstants.TRANSFER_TO, 100, contractVersion);
        return createInvalidTx(branchId, Constants.EMPTY_BYTE8, txBody);
    }

    public static Transaction createInvalidTransferTx(BranchId branchId, ContractVersion contractVersion, long amount) {
        JsonObject txBody = ContractTestUtils
                .invalidTransferTxBodyJson(TestConstants.TRANSFER_TO, amount, contractVersion);
        return createInvalidTx(branchId, Constants.EMPTY_BYTE8, txBody);
    }

    public static Transaction createInvalidTransferTx() { //(timeout, invalid format, untrusted)
        JsonObject txBody = ContractTestUtils.transferTxBodyJson(TestConstants.TRANSFER_TO, 100);
        return createInvalidTx(TestConstants.yggdrash(), "invalid".getBytes(), txBody);
    }

    // Instead of TransactionBuilder's createTx
    private static Transaction createInvalidTx(BranchId branchId, byte[] txVersion, JsonObject txBody) {
        TransactionBody transactionBody = new TransactionBody(txBody);
        byte[] chain = branchId.getBytes();
        long hour = (1000 * 60 * 60);
        long timestamp = TimeUtils.time() + hour * 2;
        TransactionHeader txHeader = new TransactionHeader(
                chain, txVersion, Constants.EMPTY_BYTE8, timestamp, transactionBody);
        byte[] sign = Constants.EMPTY_SIGNATURE;
        return new TransactionImpl(txHeader, sign, transactionBody);
    }
}
