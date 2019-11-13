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

package io.yggdrash.core.store;

import com.google.gson.JsonObject;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.consensus.ConsensusBlock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchStoreTest {
    private static final Logger log = LoggerFactory.getLogger(BranchStoreTest.class);
    private BranchStore branchStore;

    @Before
    public void setUp() {
        this.branchStore = new BranchStore(new StateStore(new HashMapDbSource()));
    }

    @After
    public void tearDown() {
        this.branchStore.close();
    }

    @Test
    public void shouldBeLoaded() {
        ConsensusBlock block = BlockChainTestUtils.genesisBlock();
        branchStore.setBestBlockHash(block.getHash());

        Sha3Hash sha3Hash = branchStore.getBestBlockHash();
        assertThat(sha3Hash).isEqualTo(block.getHash());

        Sha3Hash sha3HashAgain = branchStore.getBestBlockHash();
        assertThat(sha3HashAgain).isEqualTo(sha3Hash);
    }

    @Test
    public void shouldBePutMeta() {
        ConsensusBlock block = BlockChainTestUtils.genesisBlock();
        branchStore.setBestBlock(block);
        Long bestBlock = branchStore.getBestBlock();

        assertThat(bestBlock).isEqualTo(block.getIndex());
    }

    @Test
    public void getSetGenesisBlock() {
        ConsensusBlock block = BlockChainTestUtils.genesisBlock();
        branchStore.setGenesisBlockHash(block.getHash());

        Sha3Hash genesis = branchStore.getGenesisBlockHash();
        log.debug(block.getHash().toString());
        log.debug(genesis.toString());

        assertThat(genesis).isEqualTo(block.getHash());

        Sha3Hash otherGenesisBlock = new Sha3Hash("TEST".getBytes());
        assertThat(branchStore.setGenesisBlockHash(otherGenesisBlock)).isFalse();

        assertThat(branchStore.getGenesisBlockHash()).isEqualTo(genesis);
    }

    @Test
    public void getSetBranch() {
        TestConstants.yggdrash();
        Branch branch = TestConstants.TEST_BRANCH;
        branchStore.setBranch(branch);

        Branch loadBranch = branchStore.getBranch();
        assertThat(branch.getBranchId()).isEqualTo(loadBranch.getBranchId());
        assertThat(branchStore.getBranchId()).isEqualTo(branch.getBranchId());
    }

    @Test
    public void getSetValidators() {
        Map<String, Validator> validatorMap = new HashMap<>();
        validatorMap.put("TEST1",
                new Validator("a2b0f5fce600eb6c595b28d6253bed92be0568ed"));
        validatorMap.put("TEST2",
                new Validator("a2b0f5fce600eb6c595b28d6253bed92be0568ed"));
        validatorMap.put("TEST3",
                new Validator("a2b0f5fce600eb6c595b28d6253bed92be0568ed"));
        ValidatorSet validatorSet = new ValidatorSet();
        validatorSet.setValidatorMap(validatorMap);

        branchStore.setValidators(validatorSet);

        assertThat(branchStore.getValidators().getValidatorMap().containsKey("TEST1")).isTrue();
    }

    @Test
    public void branchContracts() {
        TestConstants.yggdrash();
        Branch branch = TestConstants.TEST_BRANCH;
        List<BranchContract> bc = branch.getBranchContracts();
        branchStore.setBranchContracts(bc);

        List<BranchContract> bc2 = branchStore.getBranchContacts();

        assertThat(bc).isNotEqualTo(bc2);
        assertThat(bc.size()).isEqualTo(bc2.size());
        assertThat(bc.get(0).getContractVersion()).isEqualTo(bc2.get(0).getContractVersion());
        assertThat(bc.get(bc.size() - 1).getInit().toString())
                .isEqualTo((bc2.get(bc2.size() - 1).getInit().toString()));
    }

    @Test
    public void addAndRemoveBranchContracts() {
        TestConstants.yggdrash();
        Branch branch = TestConstants.TEST_BRANCH;
        List<BranchContract> bc = branch.getBranchContracts();
        branchStore.setBranchContracts(bc);

        branchStore.addBranchContract(createBranchContract("COIN", "a88ae404e837cd1d6e8b9a5a91f188da835ccb56"));
        branchStore.addBranchContract(createBranchContract("YEED", "f8f7c637abbd33422f966974663c2d73280840f3"));
        branchStore.addBranchContract(createBranchContract("YEED", "c3c2721803e7099b7ae0d0fc2af7dc4455dda65e"));

        Assert.assertEquals(6, branchStore.getBranchContacts().size());
        Assert.assertEquals("YEED", branchStore.getContractName("c3c2721803e7099b7ae0d0fc2af7dc4455dda65e"));
        Assert.assertEquals("YEED", branchStore.getContractName("f8f7c637abbd33422f966974663c2d73280840f3"));
        Assert.assertEquals("COIN", branchStore.getContractName("a88ae404e837cd1d6e8b9a5a91f188da835ccb56"));
        Assert.assertEquals("c3c2721803e7099b7ae0d0fc2af7dc4455dda65e", branchStore.getContractVersion("YEED"));
        Assert.assertEquals("a88ae404e837cd1d6e8b9a5a91f188da835ccb56", branchStore.getContractVersion("COIN"));

        branchStore.removeBranchContract("c3c2721803e7099b7ae0d0fc2af7dc4455dda65e");

        Assert.assertEquals(5, branchStore.getBranchContacts().size());
        Assert.assertEquals("f8f7c637abbd33422f966974663c2d73280840f3", branchStore.getContractVersion("YEED"));
        Assert.assertNull(branchStore.getContractName("c3c2721803e7099b7ae0d0fc2af7dc4455dda65e"));
        Assert.assertNull(branchStore.getContractVersion("TEST"));
    }

    private BranchContract createBranchContract(String name, String contractVersion) {
        JsonObject branchContractJson = new JsonObject();
        branchContractJson = new JsonObject();
        branchContractJson.add("init", new JsonObject());
        branchContractJson.addProperty("name", name);
        branchContractJson.addProperty("description", name);
        branchContractJson.addProperty("property", "");
        branchContractJson.addProperty("isSystem", false);
        branchContractJson.addProperty("contractVersion", contractVersion);

        return BranchContract.of(branchContractJson);
    }
}
