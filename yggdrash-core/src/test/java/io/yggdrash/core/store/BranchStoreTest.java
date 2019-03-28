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
import io.yggdrash.common.store.datasource.HashMapDbSource;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchContract;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchStoreTest {
    private static final Logger log = LoggerFactory.getLogger(BranchStoreTest.class);
    private BranchStore ms;

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
        assertThat(sha3Hash).isEqualTo(blockHusk.getHash());

        Sha3Hash sha3HashAgain = ms.getBestBlockHash();
        assertThat(sha3HashAgain).isEqualTo(sha3Hash);
    }

    @Test
    public void shouldBePutMeta() {
        BlockHusk blockHusk = BlockChainTestUtils.genesisBlock();
        ms.setBestBlock(blockHusk);
        Long bestBlock = ms.getBestBlock();

        assertThat(bestBlock).isEqualTo(blockHusk.getIndex());
    }

    @Test
    public void getSetGenesisBlock() {
        BlockHusk blockHusk = BlockChainTestUtils.genesisBlock();
        ms.setGenesisBlockHash(blockHusk.getHash());

        Sha3Hash genesis = ms.getGenesisBlockHash();
        log.debug(blockHusk.getHash().toString());
        log.debug(genesis.toString());

        assertThat(genesis).isEqualTo(blockHusk.getHash());

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
    public void getSetValidators() throws IOException {
        Set<String> validators = new HashSet<>();
        validators.add("TEST1");
        validators.add("TEST2");
        validators.add("TEST3");

        ms.setValidators(validators);
        assertThat(ms.getValidators()).contains("TEST1");

        validators.remove("TEST1");
        assertThat(ms.getValidators()).contains("TEST1");

        ms.setValidators(validators);
        assertThat(ms.getValidators()).doesNotContain("TEST1");

        ms.addValidator("TEST1");
        assertThat(ms.getValidators()).contains("TEST1");

        ms.removeValidator("TEST1");
        assertThat(ms.getValidators()).doesNotContain("TEST1");
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
