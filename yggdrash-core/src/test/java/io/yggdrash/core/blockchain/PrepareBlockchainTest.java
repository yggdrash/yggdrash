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

package io.yggdrash.core.blockchain;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.ContractVersion;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.net.URL;

public class PrepareBlockchainTest {
    private static final Logger log = LoggerFactory.getLogger(PrepareBlockchainTest.class);

    @Test
    @Ignore
    public void loadContractFile() {
        PrepareBlockchain pb = new PrepareBlockchain(new DefaultConfig().getContractPath());
        ContractVersion version = ContractVersion.of("74df17611373672371cb3872e8a5d4a2e8733fb1");
        File f = pb.loadContractFile(version);
        boolean verify = pb.verifyContractFile(f, version);

        assert verify == true;

    }


    @Test
    public void verifyContractFile() {
        PrepareBlockchain pb = new PrepareBlockchain(new DefaultConfig().getContractPath());
        URL contractFile = getClass().getClassLoader()
                .getResource("contracts/74df17611373672371cb3872e8a5d4a2e8733fb1.jar");
        String file = contractFile.getFile();
        log.debug(file);
        File f = new File(file);
        ContractVersion version = ContractVersion.of("74df17611373672371cb3872e8a5d4a2e8733fb1");

        boolean verify = pb.verifyContractFile(f, version);
        assert verify == true;
    }
}