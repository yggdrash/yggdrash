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
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ContractManagerTest {
    Logger log = LoggerFactory.getLogger(ContractManagerTest.class);

    private static DefaultConfig defaultConfig = new DefaultConfig();
    private static ContractManager contractManager;
    private static Map<ContractVersion, ContractMeta> contracts;


    @Before
    public void loadTest() {
        if (defaultConfig.isProductionMode()) {
            ContractClassLoader.copyResourcesToContractPath(defaultConfig.getContractPath());
        }
        this.contractManager = new ContractManager(defaultConfig.getContractPath());
        this.contracts = contractManager.getContracts();
    }

    @Test
    public void getContractById() {
        List<ContractVersion> sampleContractVersionList = contractSample();
        if (sampleContractVersionList == null || contracts == null) {
            return;
        }

        ContractMeta meta = ContractClassLoader.loadContractClass(StemContract.class);
        ContractMeta meta2 = contractManager.getContractByVersion(meta.getContractVersion());
        assertNotNull(meta);
        log.debug("StemContract.class id={}", meta.getContractVersion().toString());
        assertEquals(meta2.getContractVersion(), meta.getContractVersion());
        assertEquals(meta2.getContract().getName(), meta.getContract().getName());
    }

    @Test
    public void getContractIdList() {
        List<ContractVersion> sampleContractVersionList = contractSample();
        if (sampleContractVersionList == null || contracts == null) {
            return;
        }
        assertEquals(sampleContractVersionList.size(), contractManager.getContractVersionList().size());
    }

    @Test
    public void getContractList() {
        List<ContractVersion> sampleContractVersionList = contractSample();
        if (sampleContractVersionList == null || contracts == null) {
            return;
        }
        assertEquals(sampleContractVersionList.size(), contractManager.getContractList().size());
    }

    @Test
    public void isContract() {
        if (contracts == null) {
            return;
        }
        contracts.entrySet().forEach(set -> {
            if (set.getKey() != null) {
                assertEquals(true, contractManager.isContract(set.getKey()));
            }
        });
    }

    @Test
    public void convertContractToVersion() {
        ContractVersion version = contractManager.convertContractToVersion(TestContract.class);
        ContractMeta meta = ContractClassLoader.loadContractClass(TestContract.class);
        assertEquals(version, meta.getContractVersion());
    }

    @Test
    public void addContract() {
        Class<? extends Contract> contract = TestContract.class;
        if (contracts == null) {
            return;
        }
        long beforeSize = contracts.entrySet().size();
        contractManager.addContract(contract);
        assertEquals(beforeSize + 1, contracts.entrySet().size());

        ContractMeta contractMeta = ContractClassLoader.loadContractClass(contract);
        ContractVersion contractVersion = contractMeta.getContractVersion();
        contractManager.removeContract(contractVersion);
    }

    @Test
    public void decodingContract() throws UnsupportedEncodingException {
        if (contracts == null) {
            return;
        }
        contracts.entrySet().stream().findFirst();
        Optional<Map.Entry<ContractVersion, ContractMeta>> m = contracts.entrySet().stream().findFirst();
        ContractVersion version = m.get().getKey();

        byte[] targetBytes = version.toString().getBytes("UTF-8");

        Base64.Encoder encoder = Base64.getEncoder();
        String encodedString = encoder.encodeToString(targetBytes);

        ContractMeta meta = contractManager.decodingContract(encodedString);
        assertEquals(version, meta.getContractVersion());
    }

    private List<ContractVersion> contractSample() {
        DefaultConfig defaultConfig = new DefaultConfig();
        List<ContractVersion> contractsVersionSampleIds = new ArrayList<>();
        try (Stream<Path> filePathStream = Files.walk(Paths.get(String.valueOf(defaultConfig.getContractPath())))) {
            filePathStream.forEach(contractPath -> {
                File contractFile = new File(String.valueOf(contractPath));
                if (contractFile.isDirectory()) {
                    return;
                }
                byte[] contractBinary;
                try (FileInputStream inputStream = new FileInputStream(contractFile)) {
                    contractBinary = new byte[Math.toIntExact(contractFile.length())];
                    inputStream.read(contractBinary);

                    ContractVersion contractVersion = ContractVersion.of(contractBinary);
                    ContractMeta contractMeta = ContractClassLoader.loadContractById(
                            defaultConfig.getContractPath(), contractVersion);

                    if (contractMeta.getStateStore() != null
                            || contractMeta.getTxReceipt() != null) {
                        contractsVersionSampleIds.add(contractVersion);
                    }
                } catch (IOException e) {
                    log.warn(e.getMessage());
                }
            });
            return contractsVersionSampleIds;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}