/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.config;

import com.google.gson.JsonObject;
import io.yggdrash.contract.CoinContract;
import io.yggdrash.contract.Contract;
import io.yggdrash.contract.NoneContract;
import io.yggdrash.contract.StemContract;
import io.yggdrash.core.BlockChain;
import io.yggdrash.core.BlockChainLoader;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.Runtime;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.core.store.TransactionStore;
import io.yggdrash.core.store.datasource.DbSource;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.util.Arrays;

@Configuration
public class BranchConfiguration {
    private static final String STEM = "stem";
    private static final String YEED = "yeed";
    private static final String FORMAT = "classpath:/branch-%s.json";

    private final BranchProperties branchProperties;
    private final ResourceLoader resourceLoader;
    private boolean isProduction = false;

    BranchConfiguration(BranchProperties branchProperties, ResourceLoader resourceLoader,
                        Environment env) {
        this.branchProperties = branchProperties;
        this.resourceLoader = resourceLoader;
        if (Arrays.asList(env.getActiveProfiles()).contains("prod")) {
            isProduction = true;
        }
    }

    @Bean
    BranchGroup branchGroup() throws IOException {
        BranchGroup banchGroup = new BranchGroup();
        BlockChain stem = getBlockChain(STEM);
        banchGroup.addBranch(stem.getBranchId(), stem);
        for (String branchName : branchProperties.getNameList()) {
            BlockChain branch = getBlockChain(branchName);
            banchGroup.addBranch(branch.getBranchId(), branch);
        }
        return banchGroup;
    }

    private BlockChain getBlockChain(String branchName) throws IOException {
        Resource resource = resourceLoader.getResource(String.format(FORMAT, branchName));
        BlockHusk genesis = new BlockChainLoader(resource.getInputStream()).getGenesis();
        BlockStore blockStore = new BlockStore(getDbSource(genesis.getBranchId() + "/blocks"));
        TransactionStore txStore =
                new TransactionStore(getDbSource(genesis.getBranchId() + "/txs"));
        Contract contract = getContract(branchName);
        Runtime<?> runtime = getRunTime(branchName);
        return new BlockChain(genesis, blockStore, txStore, contract, runtime);
    }

    private DbSource<byte[], byte[]> getDbSource(String path) {
        if (isProduction) {
            return new LevelDbDataSource(path);
        } else {
            return new HashMapDbSource();
        }
    }

    private Contract getContract(String branchName) {
        if (STEM.equalsIgnoreCase(branchName)) {
            return new StemContract();
        } else if (YEED.equalsIgnoreCase(branchName)) {
            return new CoinContract();
        } else {
            return new NoneContract();
        }
    }

    private Runtime<?> getRunTime(String branchName) {
        if (STEM.equalsIgnoreCase(branchName)) {
            return getRunTime(JsonObject.class);
        } else if (YEED.equalsIgnoreCase(branchName)) {
            return getRunTime(Long.class);
        } else {
            return getRunTime(String.class);
        }
    }

    private <T> Runtime<T> getRunTime(Class<T> clazz) {
        return new Runtime<>(new StateStore<>(), new TransactionReceiptStore());
    }
}
