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
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockChainBuilder;
import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.core.blockchain.BlockChainManagerImpl;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.BlockImpl;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.PbftBlockChainMock;
import io.yggdrash.core.blockchain.PbftBlockMock;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBody;
import io.yggdrash.core.blockchain.TransactionBuilder;
import io.yggdrash.core.blockchain.TransactionHeader;
import io.yggdrash.core.blockchain.TransactionImpl;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.ContractManager;
import io.yggdrash.core.blockchain.osgi.ContractManagerBuilder;
import io.yggdrash.core.blockchain.osgi.framework.BootFrameworkConfig;
import io.yggdrash.core.blockchain.osgi.framework.BootFrameworkLauncher;
import io.yggdrash.core.blockchain.osgi.framework.BundleServiceImpl;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.InvalidSignatureException;
import io.yggdrash.core.store.BlockChainStore;
import io.yggdrash.core.store.BlockChainStoreBuilder;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.PbftBlockStoreMock;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.PbftProto;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BlockChainTestUtils {
    private static final GenesisBlock genesis;
    private static final Logger log = LoggerFactory.getLogger(BlockChainTestUtils.class);
    private ContractVersion stem;

    public static GenesisBlock getGenesis() {
        return genesis;
    }

    private BlockChainTestUtils() {
    }

    static {
        try {
            GenesisBlock genesisBlock = generateGenesisBlockByFile(TestConstants.branchFile);
            TestConstants.yggdrash();
            genesis = genesisBlock;
        } catch (Exception e) {
            throw new InvalidSignatureException(e);
        }
    }


    public static GenesisBlock generateGenesisBlockByFile(File file) {
        try (InputStream is = new FileInputStream(file)) {
            return generateGenesisBlockByInputStream(is);
        } catch (Exception e) {
            throw new InvalidSignatureException(e);
        }
    }

    public static GenesisBlock generateGenesisBlockByInputStream(InputStream is) {
        try {
            GenesisBlock genesisBlock = GenesisBlock.of(is);
            genesisBlock.toBlock(ContractTestUtils.calGenesisStateRoot(genesisBlock));
            return genesisBlock;
        } catch (IOException e) {
            throw new InvalidSignatureException(e);
        }

    }

    private static List<ConsensusBlock<PbftProto.PbftBlock>> sampleBlockList
            = createBlockListWithoutTxs(100, genesisBlock());

    public static List<ConsensusBlock<PbftProto.PbftBlock>> getSampleBlockList() {
        return sampleBlockList;
    }

    public static ConsensusBlock<PbftProto.PbftBlock> genesisBlock() {
        return new PbftBlockMock(genesis.getBlock());
    }

    public static ConsensusBlock<PbftProto.PbftBlock> genesisBlock(File file) {
        return new PbftBlockMock(generateGenesisBlockByFile(file).getBlock());
    }

    public static ConsensusBlock<PbftProto.PbftBlock> createNextBlock() {
        PbftBlockMock genesisBLock = new PbftBlockMock(genesis.getBlock());
        return new PbftBlockMock(BlockImpl.nextBlock(
                TestConstants.wallet(),
                Collections.emptyList(),
                genesisBLock.getHeader().getStateRoot(),
                genesisBLock));
    }

    public static ConsensusBlock<PbftProto.PbftBlock> createNextBlock(ConsensusBlock prevBlock) {
        return new PbftBlockMock(BlockImpl.nextBlock(
                TestConstants.wallet(),
                Collections.emptyList(),
                prevBlock.getHeader().getStateRoot(),
                prevBlock));
    }

    public static ConsensusBlock<PbftProto.PbftBlock> createNextBlock(List<Transaction> blockBody,
                                                                      ConsensusBlock prevBlock,
                                                                      ContractManager contractManager) {
        byte[] stateRoot = contractManager != null
                ? ContractTestUtils.calStateRoot(contractManager, blockBody).getBytes()
                : ContractTestUtils.calStateRoot(prevBlock, blockBody).getBytes();
        return new PbftBlockMock(BlockImpl.nextBlock(TestConstants.wallet(), blockBody, stateRoot, prevBlock));
    }

    public static ConsensusBlock<PbftProto.PbftBlock> createNextBlock(Wallet wallet,
                                                                      List<Transaction> blockBody,
                                                                      ConsensusBlock prevBlock,
                                                                      ContractManager contractManager) {
        byte[] stateRoot = contractManager != null
                ? ContractTestUtils.calStateRoot(contractManager, blockBody).getBytes()
                : ContractTestUtils.calStateRoot(prevBlock, blockBody).getBytes();
        return new PbftBlockMock(BlockImpl.nextBlock(wallet, blockBody, stateRoot, prevBlock));
    }

    public static ConsensusBlock<PbftProto.PbftBlock> createSpecificHeightBlock(long index,
                                                                                List<Transaction> blockBody,
                                                                                ConsensusBlock prevBlock,
                                                                                ContractManager contractManager) {
        ConsensusBlock<PbftProto.PbftBlock> nextBlock = createNextBlock(blockBody, prevBlock, contractManager);
        for (long i = 1L; i < index; i++) {
            nextBlock = createNextBlock(blockBody, nextBlock, contractManager);
        }
        return nextBlock;
    }

    public static ConsensusBlock<PbftProto.PbftBlock> createNextBlockByPrevHash(Sha3Hash specificPrevHash,
                                                                                ConsensusBlock prevBlock) {
        Block prevTmpBlock = prevBlock.getBlock();
        BlockHeader prevTmpBlockHeader = prevTmpBlock.getHeader();
        BlockBody blockBody = new BlockBody(new ArrayList<>()); // no txs

        BlockHeader newBlockHeader = new BlockHeader(
                prevTmpBlockHeader.getChain(),
                prevTmpBlockHeader.getVersion(),
                prevTmpBlockHeader.getType(),
                specificPrevHash.getBytes(),
                prevTmpBlockHeader.getIndex() + 1L,
                TimeUtils.time(),
                prevTmpBlockHeader.getStateRoot(),
                blockBody);

        Block block = new BlockImpl(newBlockHeader, TestConstants.wallet(), blockBody);
        return new PbftBlockMock(block);
    }

    public static Transaction createBranchTx() {
        JsonObject json = ContractTestUtils.createSampleBranchJson();

        return createBranchTx(json);
    }

    private static Transaction createBranchTx(JsonObject json) {
        //TODO stemContract test required
        TransactionBuilder builder = new TransactionBuilder();
        return builder.setTxBody(TestConstants.STEM_CONTRACT, "create", json, false)
                .setWallet(TestConstants.wallet())
                .setBranchId(genesis.getBranch().getBranchId())
                .build();
    }

    public static Transaction createBranchTx(BranchId branchId, String method,
                                             JsonObject branch) {
        TransactionBuilder builder = new TransactionBuilder();

        return builder.setTxBody(TestConstants.STEM_CONTRACT, method, branch, false)
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
        return createBlockChain(genesis, isProductionMode);
    }

    public static BlockChain createBlockChain(GenesisBlock genesis, boolean isProductionMode) {
        log.debug("createBlockChain isProdMode : {}", isProductionMode);
        log.debug("createBlockChain branchId : {}", genesis.getBranchId().toString());
        DefaultConfig config = new DefaultConfig();
        BlockChainStoreBuilder builder = BlockChainStoreBuilder.newBuilder(genesis.getBranchId())
                .setBlockStoreFactory(PbftBlockStoreMock::new)
                .withProductionMode(isProductionMode)
                .withDataBasePath(config.getDatabasePath());
        BlockChainStore bcStore = builder.build();

        ContractStore contractStore = bcStore.getContractStore();

        BootFrameworkConfig bootFrameworkConfig = new BootFrameworkConfig(config, genesis.getBranchId());
        BootFrameworkLauncher bootFrameworkLauncher = new BootFrameworkLauncher(bootFrameworkConfig);
        BundleServiceImpl bundleService = new BundleServiceImpl(bootFrameworkLauncher.getBundleContext());
        SystemProperties systemProperties = createDefaultSystemProperties();

        ContractManager contractManager = ContractManagerBuilder.newInstance()
                .withGenesis(genesis)
                .withBundleManager(bundleService)
                .withDefaultConfig(config)
                .withContractStore(contractStore)
                .withLogStore(bcStore.getLogStore())
                .withSystemProperties(systemProperties)
                .build();

        int numOfBundles =  contractManager.getBundles().length;
        int numOfBranchContracts = genesis.getBranch().getBranchContracts().size();

        // System bundle + contract bundle
        Assert.assertEquals(numOfBundles, numOfBranchContracts + 1);

        BlockChainManager blockChainManager = new BlockChainManagerImpl(bcStore);

        Sha3Hash genesisStateRootHash;
        if (genesis.getContractTxs().size() > 0) {
            genesisStateRootHash = new Sha3Hash(contractManager.executeTxs(genesis.getContractTxs())
                    .getBlockResult().get("stateRoot").get("stateHash").getAsString());
        } else {
            genesisStateRootHash = new Sha3Hash(Constants.EMPTY_HASH);
        }
        genesis.toBlock(genesisStateRootHash);

        return BlockChainBuilder.newBuilder()
                .setGenesis(genesis)
                .setBranchStore(contractStore.getBranchStore())
                .setBlockChainManager(blockChainManager)
                .setContractManager(contractManager)
                .setFactory(PbftBlockChainMock::new)
                .build();
    }

    public static BranchGroup createBranchGroup() {
        log.debug("createBranchGroup");
        BranchGroup branchGroup = new BranchGroup();
        BlockChain blockChain = createBlockChain(false);
        branchGroup.addBranch(blockChain);
        return branchGroup;
    }

    public static void generateBlock(BranchGroup branchGroup, BranchId branchId) {
        BlockChain branch = branchGroup.getBranch(branchId);
        List<Transaction> txs =
                branch.getBlockChainManager().getUnconfirmedTxsWithLimit(Constants.Limit.BLOCK_SYNC_SIZE);
        branch.addBlock(
                createNextBlock(
                        txs, branch.getBlockChainManager().getLastConfirmedBlock(), branch.getContractManager()));
    }

    public static void setBlockHeightOfBlockChain(BlockChain blockChain, int height) {
        List<ConsensusBlock<PbftProto.PbftBlock>> blockList = new ArrayList<>();
        ConsensusBlock<PbftProto.PbftBlock> curBlock
                = blockChain.getBlockChainManager().getBlockByIndex(blockChain.getBlockChainManager().getLastIndex());
        blockList = createBlockListWithoutTxs(height, curBlock);

        for (ConsensusBlock block : blockList) {
            blockChain.addBlock(block, false);
        }
    }

    public static List<ConsensusBlock<PbftProto.PbftBlock>> createBlockListWithTxs(
            int blockHeight, int txSize, ContractManager contractManager) {
        List<ConsensusBlock<PbftProto.PbftBlock>> blockList = new ArrayList<>();
        List<Transaction> blockBody =
                IntStream.range(0, txSize).mapToObj(i -> createTransferTx()).collect(Collectors.toList());
        blockList.add(createNextBlock(blockBody, genesisBlock(), contractManager));
        for (int i = 0; i < blockHeight - 1; i++) {
            blockList.add(createNextBlock(blockBody, blockList.get(i), contractManager));
        }
        return blockList;
    }

    public static List<ConsensusBlock<PbftProto.PbftBlock>> createBlockListWithoutTxs(
            int blockHeight, ConsensusBlock<PbftProto.PbftBlock> curBlock) {
        List<ConsensusBlock<PbftProto.PbftBlock>> blockList = new ArrayList<>();
        if (curBlock != null && curBlock.getIndex() != 0L) {
            blockList.add(createNextBlock(curBlock));
        } else {
            blockList.add(createNextBlock()); // nextBlock of genesisBlock
        }

        for (int i = 0; i < blockHeight - 1; i++) {
            blockList.add(createNextBlock(blockList.get(i)));
        }
        return blockList;
    }

    public static Transaction createContractProposeTx(String contractVersion, String proposalType) {
        return createContractProposeTx(TestConstants.transferWallet(), contractVersion, proposalType);
    }

    public static Transaction createContractProposeTx(Wallet wallet, String contractVersion, String proposalType) {
        return buildTx(ContractTestUtils.contractProposeTxBodyJson(contractVersion, proposalType),
                wallet, TestConstants.yggdrash());
    }

    public static Transaction createContractVoteTx(String txId, boolean agree) {
        return createContractVoteTx(TestConstants.transferWallet(), txId, agree);
    }

    public static Transaction createContractVoteTx(Wallet wallet, String txId, boolean agree) {
        return buildTx(ContractTestUtils.contractVoteTxBodyJson(txId, agree),
                wallet, TestConstants.yggdrash());
    }

    public static Transaction createTransferTx() {
        return createTransferTx(TestConstants.TRANSFER_TO, BigInteger.valueOf(100));
    }

    public static Transaction createTransferTx(String to, BigInteger amount) {
        JsonObject txBody = ContractTestUtils.transferTxBodyJson(to, amount);
        return buildTx(txBody, TestConstants.transferWallet(), TestConstants.yggdrash());
    }

    public static Transaction createTransferTx(BranchId branchId, ContractVersion contractVersion) {
        JsonObject txBody = ContractTestUtils.transferTxBodyJson(
                TestConstants.TRANSFER_TO, BigInteger.valueOf(100), contractVersion);
        return buildTx(txBody, TestConstants.transferWallet(), branchId);
    }

    public static Transaction createTransferTx(BigInteger amount, ContractVersion contractVersion) {
        JsonObject txBody = ContractTestUtils.transferTxBodyJson(TestConstants.TRANSFER_TO, amount, contractVersion);
        return buildTx(txBody, TestConstants.transferWallet(), TestConstants.yggdrash());
    }

    public static Transaction buildTx(JsonObject txBody, Wallet wallet, BranchId branchId) {
        TransactionBuilder builder = new TransactionBuilder();
        return builder.setTxBody(txBody)
                .setWallet(wallet)
                .setBranchId(branchId)
                .build();
    }

    public static Transaction createInvalidTransferTx(ContractVersion contractVersion) {
        JsonObject txBody = ContractTestUtils
                .transferTxBodyJson(TestConstants.TRANSFER_TO, BigInteger.valueOf(100), contractVersion);
        return createInvalidTx(TestConstants.yggdrash(), Constants.EMPTY_BYTE8, txBody);
    }

    public static Transaction createInvalidTransferTx(BranchId branchId, ContractVersion contractVersion) {
        JsonObject txBody = ContractTestUtils
                .transferTxBodyJson(TestConstants.TRANSFER_TO, BigInteger.valueOf(100), contractVersion);
        return createInvalidTx(branchId, Constants.EMPTY_BYTE8, txBody);
    }

    public static Transaction createInvalidTransferTx(
            BranchId branchId, ContractVersion contractVersion, BigInteger amount) {
        JsonObject txBody = ContractTestUtils
                .transferTxBodyJson(TestConstants.TRANSFER_TO, amount, contractVersion);
        return createInvalidTx(branchId, Constants.EMPTY_BYTE8, txBody);
    }

    public static Transaction createInvalidTransferTx() { //(timeout, invalid format, untrusted)
        JsonObject txBody = ContractTestUtils.transferTxBodyJson(TestConstants.TRANSFER_TO, BigInteger.valueOf(100));
        return createInvalidTx(TestConstants.yggdrash(), "invalid".getBytes(), txBody);
    }

    // Instead of TransactionBuilder's createTx
    private static Transaction createInvalidTx(BranchId branchId, byte[] txVersion, JsonObject txBody) {
        TransactionBody transactionBody = new TransactionBody(txBody);
        byte[] chain = branchId.getBytes();
        long twoHour = (1000 * 60 * 60) * 2;
        long timestamp = TimeUtils.time() + twoHour * 2;
        TransactionHeader txHeader = new TransactionHeader(
                chain, txVersion, Constants.EMPTY_BYTE8, timestamp, transactionBody);
        byte[] sign = Constants.EMPTY_SIGNATURE;
        return new TransactionImpl(txHeader, sign, transactionBody);
    }


    public static SystemProperties createDefaultSystemProperties() {
        return SystemProperties.SystemPropertiesBuilder
                .newBuilder()
                .setElasticsearchHost("127.0.0.1")
                .setElasticsearchPort(9200)
                .setEventStore(null)
                .build();

    }
}
