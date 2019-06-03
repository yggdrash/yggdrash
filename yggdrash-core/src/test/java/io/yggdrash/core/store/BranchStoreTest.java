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

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.core.consensus.ConsensusBlock;
import org.junit.After;
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
    private BranchStore ms;

    @Before
    public void setUp() {
        this.ms = new BranchStore(new StateStore(new HashMapDbSource()));
    }

    @After
    public void tearDown() {
        this.ms.close();
    }

    @Test
    public void shouldBeLoaded() {
        ConsensusBlock block = BlockChainTestUtils.genesisBlock();
        ms.setBestBlockHash(block.getHash());

        Sha3Hash sha3Hash = ms.getBestBlockHash();
        assertThat(sha3Hash).isEqualTo(block.getHash());

        Sha3Hash sha3HashAgain = ms.getBestBlockHash();
        assertThat(sha3HashAgain).isEqualTo(sha3Hash);
    }

    @Test
    public void shouldBePutMeta() {
        ConsensusBlock block = BlockChainTestUtils.genesisBlock();
        ms.setBestBlock(block);
        Long bestBlock = ms.getBestBlock();

        assertThat(bestBlock).isEqualTo(block.getIndex());
    }

    @Test
    public void getSetGenesisBlock() {
        ConsensusBlock block = BlockChainTestUtils.genesisBlock();
        ms.setGenesisBlockHash(block.getHash());

        Sha3Hash genesis = ms.getGenesisBlockHash();
        log.debug(block.getHash().toString());
        log.debug(genesis.toString());

        assertThat(genesis).isEqualTo(block.getHash());

        Sha3Hash otherGenesisBlock = new Sha3Hash("TEST".getBytes());
        assertThat(ms.setGenesisBlockHash(otherGenesisBlock)).isFalse();

        assertThat(ms.getGenesisBlockHash()).isEqualTo(genesis);
    }

    @Test
    public void getSetBranch() {
        TestConstants.yggdrash();
        Branch branch = TestConstants.TEST_BRANCH;
        ms.setBranch(branch);

        Branch loadBranch = ms.getBranch();
        assertThat(branch.getBranchId()).isEqualTo(loadBranch.getBranchId());
        assertThat(ms.getBranchId()).isEqualTo(branch.getBranchId());
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

        ms.setValidators(validatorSet);

        assertThat(ms.getValidators().getValidatorMap().containsKey("TEST1")).isTrue();
    }

    @Test
    public void branchContracts() {
        TestConstants.yggdrash();
        Branch branch = TestConstants.TEST_BRANCH;
        List<BranchContract> bc = branch.getBranchContracts();
        ms.setBranchContracts(bc);

        List<BranchContract> bc2 = ms.getBranchContacts();

        assertThat(bc).isNotEqualTo(bc2);
        assertThat(bc.size()).isEqualTo(bc2.size());
        assertThat(bc.get(0).getContractVersion()).isEqualTo(bc2.get(0).getContractVersion());
        assertThat(bc.get(bc.size() - 1).getInit().toString())
                .isEqualTo((bc2.get(bc2.size() - 1).getInit().toString()));
    }

}
