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

package io.yggdrash.core.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.core.blockchain.BlockChainSyncManager;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.p2p.BlockChainHandler;
import io.yggdrash.core.p2p.PeerHandlerMock;
import io.yggdrash.mock.ContractMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainSyncManagerTest {
    private static final Logger log = LoggerFactory.getLogger(BlockChainSyncManagerTest.class);

    private final BlockChainHandler handler = PeerHandlerMock.dummy();
    private BlockChainSyncManager syncManager;
    private BlockChain blockChain;
    private BlockChainManager blockChainManager;

    @Before
    public void setUp() {
        log.debug("init");
        BlockChainSyncManagerMock bcsmm = new BlockChainSyncManagerMock();

        syncManager = bcsmm.getMock();
        blockChain = BlockChainTestUtils.createBlockChain(false);
        blockChainManager = blockChain.getBlockChainManager();
    }

    @Ignore
    @Test
    public void syncBlock() {
        assertThat(blockChainManager.getLastIndex()).isEqualTo(0);

        syncManager.syncBlock(handler, blockChain);

        assertThat(blockChainManager.getLastIndex()).isEqualTo(33);
    }

    @Ignore
    @Test
    public void syncBlockFailed() {
        GenesisBlock genesisBlock = createGenesisBlock();
        BlockChain otherBlockChain = BlockChainTestUtils.createBlockChain(genesisBlock, false);

        syncManager.syncBlock(handler, otherBlockChain);
    }

    private GenesisBlock createGenesisBlock() {
        Branch branch = createTestBranch();
        return GenesisBlock.of(branch);
    }

    private Branch createTestBranch() {
        ContractMock contractMock = ContractMock.builder()
                .setName("TEST")
                .setSymbol("TEST")
                .setProperty("TEST")
                .setDescription("TEST")
                .setContractName("TEST_CONTRACT")
                .setContractInit(createAllocParam())
                .setContractDescription("TEST_CONTRACT")
                .build();
        return ContractTestUtils.createBranch(contractMock);
    }

    private JsonObject createAllocParam() {
        String allocStr = new StringBuilder()
                .append("{\"alloc\": {\n")
                .append("\"101167aaf090581b91c08480f6e559acdd9a3ddd\": {\n")
                .append("\"balance\": \"1000000000000000000000\"\n")
                .append("}")
                .append("}}")
                .toString();
        return new JsonParser().parse(allocStr).getAsJsonObject();
    }

    @Test
    @Ignore
    public void syncTransaction() {
        syncManager.syncTransaction(handler, blockChain);
        // Genesis Block's Init Tx size is Contract List size
        //int genesisBlockTxSize = blockChain.getBranchContracts().size();
        // TODO change
        //assertThat(blockChainManager.countOfTxs()).isEqualTo(genesisBlockTxSize);
    }
}