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

package io.yggdrash.core.contract;

import io.yggdrash.common.config.DefaultConfig;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContractManagerTest {
    Logger log = LoggerFactory.getLogger(ContractManagerTest.class);



    @Test
    public void loadTest() {
        DefaultConfig defaultConfig = new DefaultConfig();
        log.debug(defaultConfig.getContractPath());
        ContractManager manager = new ContractManager(defaultConfig.getContractPath());

        ContractMeta meta = manager.getContractById(
                ContractId.of("1378d5ac6e6b7b536165a9a9225684dc93206261"));
        assert "1378d5ac6e6b7b536165a9a9225684dc93206261".equals(meta.getContractId().toString());

        log.debug(meta.getContract().getSimpleName());

    }

}
