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
import io.yggdrash.common.store.StateStore;
import io.yggdrash.common.store.datasource.HashMapDbSource;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContractContainerBuilderTest {
    private static final Logger log = LoggerFactory.getLogger(ContractContainerBuilderTest.class);

    @Test
    public void build() {
        // check builder config
        StateStore store = new StateStore(new HashMapDbSource());
        DefaultConfig config = new DefaultConfig();
        ContractPolicyLoader loader = new ContractPolicyLoader();
        Map output = new HashMap();

        ContractContainer container = ContractContainerBuilder.newInstance()
                .withConfig(config)
                .withFrameworkFactory(loader.getFrameworkFactory())
                .withContainerConfig(loader.getContainerConfig())
                .withStateStore(store)
                .withOutputStore(output)
                .withBranchId("test")
                .build();

        assert container != null;
        assert container.getContractManager() != null;


        // Contract File
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("contracts/96206ff28aead93a49272379a85191c54f7b33c0.jar");

        String filePath = getClass().getClassLoader()
                .getResource("contracts/96206ff28aead93a49272379a85191c54f7b33c0.jar")
                .getFile();
        File contractFile = new File(filePath);


        ContractVersion version = ContractVersion.of("TEST".getBytes());
        if (!container.getContractManager().checkExistContract(
                "io.yggdrash.contract.coin.CoinContract","1.0.0")) {
            long bundle = container.installContract(version, contractFile, true);
            assert bundle > 0L;
        }
        ContractManager manager = container.getContractManager();
        for (ContractStatus cs : manager.searchContracts()) {
            log.debug("Description {}", cs.getDescription());
            log.debug("Location {}", cs.getLocation());
            log.debug("SymbolicName {}", cs.getSymbolicName());
            log.debug("Version {}", cs.getVersion());
            log.debug(Long.toString(cs.getId()));
        }


        ///container.loadUserContract();

    }
}