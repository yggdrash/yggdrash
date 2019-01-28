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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class ContractManagerTest {
    Logger log = LoggerFactory.getLogger(ContractManagerTest.class);

    private static DefaultConfig defaultConfig = new DefaultConfig();
    private static ContractManager contractManager = new ContractManager(defaultConfig.getContractPath());


    @Test
    public void loadTest() {
        if (defaultConfig.isProductionMode()) {
            ContractClassLoader.copyResourcesToContractPath(defaultConfig.getContractPath());
        }
        new ContractManager(defaultConfig.getContractPath());
    }

    @Test
    public void getContractsTest() {
        contractManager.getContracts();
    }


    @Test
    public void getContractById() {
        List<ContractId> sampleContractIdList = contractSample();
        Map<ContractId, ContractMeta> contracts = contractManager.getContracts();
        if (!sampleContractIdList.isEmpty() && !contracts.isEmpty()) return;
        sampleContractIdList.forEach((id) -> {
            if (id.getBytes().length > 0) {
                assertEquals(true, contracts.containsKey(id));
            }
        });
    }

    @Test
    public void isContract() {
        Map<ContractId, ContractMeta> contracts = contractManager.getContracts();
        if (contracts.isEmpty()) return;
        for (Map.Entry<ContractId, ContractMeta> elem : contracts.entrySet()) {
            if (elem.getKey() != null){
                assertEquals(true, contractManager.isContract(elem.getKey()));
            }
        }
    }

    private List<ContractId> contractSample() {
        DefaultConfig defaultConfig = new DefaultConfig();
        List<ContractId> cIds = new ArrayList<>();
        try (Stream<Path> filePathStream = Files.walk(Paths.get(String.valueOf(defaultConfig.getContractPath())))) {
            filePathStream.forEach(contractPath -> {
                File contractFile = new File(String.valueOf(contractPath));
                if(contractFile.isDirectory()) return;
                byte[] contractBinary;
                try (FileInputStream inputStream = new FileInputStream(contractFile)) {
                    contractBinary = new byte[Math.toIntExact(contractFile.length())];
                    inputStream.read(contractBinary);

                    ContractId contractId = ContractId.of(contractBinary);
                    ContractMeta contractMeta = ContractClassLoader.loadContractById(
                            defaultConfig.getContractPath(), contractId);

                    if(contractMeta.getStateStore() !=null || contractMeta.getTxReceipt() !=null) {
                        cIds.add(contractId);
                    }
                } catch (IOException e) {
                    log.warn(e.getMessage());
                }
            });
            return cIds;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}


