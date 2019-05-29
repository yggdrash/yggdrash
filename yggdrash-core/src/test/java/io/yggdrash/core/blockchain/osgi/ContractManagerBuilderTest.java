/*
 * Copyright 2019 Akashic Foundation
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

package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.ContractVersion;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ContractManagerBuilderTest {
    private static final Logger log = LoggerFactory.getLogger(ContractManagerBuilderTest.class);

    @Test
    public void build() {
        // check builder config
        DefaultConfig config = new DefaultConfig();
        ContractPolicyLoader loader = new ContractPolicyLoader();

        ContractManager manager = ContractManagerBuilder.newInstance()
                .withConfig(config)
                .withFrameworkFactory(loader.getFrameworkFactory())
                .withContractManagerConfig(loader.getContractManagerConfig())
                .withBranchId("test")
                .build();

        assert manager != null;
        assert manager.getContractExecutor() != null;


        // Contract File
        String filePath = getClass().getClassLoader()
                .getResource("contracts/96206ff28aead93a49272379a85191c54f7b33c0.jar")
                .getFile();
        File contractFile = new File(filePath);

        ContractVersion version = ContractVersion.of("TEST".getBytes());
        if (contractFile.exists() && !manager.checkExistContract(
                "io.yggdrash.contract.coin.CoinContract","1.0.0")) {
            long bundle = manager.installContract(version, contractFile, true);
            assert bundle > 0L;
        }

        for (ContractStatus cs : manager.searchContracts()) {
            log.debug("Description {}", cs.getDescription());
            log.debug("Location {}", cs.getLocation());
            log.debug("SymbolicName {}", cs.getSymbolicName());
            log.debug("Version {}", cs.getVersion());
            log.debug(Long.toString(cs.getId()));
        }

        ///manager.loadUserContract();
    }
}