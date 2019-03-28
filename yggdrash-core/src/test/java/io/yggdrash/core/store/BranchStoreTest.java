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
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchContract;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BranchStoreTest {

    private BranchStore ms;
    Logger log = LoggerFactory.getLogger(TempStateStoreTest.class);

    @Before
    public void setUp() {
        this.ms = new BranchStore(new HashMapDbSource());
    }

    @After
    public void tearDown() {
        this.ms.close();
    }

    @Test
    public void shouldBeLoaded() {
        BlockHusk blockHusk = BlockChainTestUtils.genesisBlock();
        ms.setBestBlockHash(blockHusk.getHash());

        Sha3Hash sha3Hash = ms.getBestBlockHash();
        Assertions.assertThat(sha3Hash).isEqualTo(blockHusk.getHash());

        Sha3Hash sha3HashAgain = ms.getBestBlockHash();
        Assertions.assertThat(sha3HashAgain).isEqualTo(sha3Hash);
    }

    @Test
    public void shouldBePutMeta() {
        BlockHusk blockHusk = BlockChainTestUtils.genesisBlock();
        ms.setBestBlock(blockHusk);
        Long bestBlock = ms.getBestBlock();

        Assertions.assertThat(bestBlock).isEqualTo(blockHusk.getIndex());
    }

    @Test
    public void getSetGenesisBlock() {
        BlockHusk blockHusk = BlockChainTestUtils.genesisBlock();
        ms.setGenesisBlockHash(blockHusk.getHash());

        Sha3Hash genesis = ms.getGenesisBlockHash();
        log.debug(blockHusk.getHash().toString());
        log.debug(genesis.toString());

        assert genesis.equals(blockHusk.getHash());

        Sha3Hash otherGenesisBlock = new Sha3Hash("TEST".getBytes());
        assert !ms.setGenesisBlockHash(otherGenesisBlock);

        assert ms.getGenesisBlockHash().equals(genesis);
    }

    @Test
    public void getSetBranch() {
        TestConstants.yggdrash();
        Branch branch = TestConstants.TEST_BRANCH;
        ms.setBranch(branch);

        Branch loadBranch = ms.getBranch();
        assert branch.getBranchId().equals(loadBranch.getBranchId());
        assert ms.getBranchId().equals(branch.getBranchId());
    }


    @Test
    public void getSetValidators() {
        ValidatorSet validatorSet = new ValidatorSet();
        Map<String, Validator> validatorMap = new HashMap<>();
        validatorMap.put("TEST1",
                new Validator("a2b0f5fce600eb6c595b28d6253bed92be0568ed"));
        validatorMap.put("TEST2",
                new Validator("a2b0f5fce600eb6c595b28d6253bed92be0568ed"));
        validatorMap.put("TEST3",
                new Validator("a2b0f5fce600eb6c595b28d6253bed92be0568ed"));
        validatorSet.setValidatorMap(validatorMap);

        ms.setValidators(validatorSet);

        assert ms.getValidators().getValidatorMap().containsKey("TEST1");
    }


    @Test
    public void branchContracts() {
        TestConstants.yggdrash();
        Branch branch = TestConstants.TEST_BRANCH;
        List<BranchContract> bc = branch.getBranchContracts();
        ms.setBranchContracts(bc);

        List<BranchContract> bc2 = ms.getBranchContacts();

        assert !bc.equals(bc2);
        assert bc.size() == bc2.size();
        assert bc.get(0).getContractVersion().equals(bc2.get(0).getContractVersion());
        assert bc.get(bc.size() - 1).getInit().toString()
                .equals(bc2.get(bc2.size() - 1).getInit().toString());
    }

}
