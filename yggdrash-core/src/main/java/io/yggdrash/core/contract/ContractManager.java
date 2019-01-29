/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.contract;

import io.yggdrash.common.util.ContractUtils;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class ContractManager extends ClassLoader {
    private static final Logger log = LoggerFactory.getLogger(ContractManager.class);
    private Map<ContractVersion, ContractMeta> contracts = new HashMap<>();

    public ContractManager(String contractPath) {
        load(contractPath);
    }

    private void load(String contractRoot) {
        try (Stream<Path> filePathStream = Files.walk(Paths.get(String.valueOf(contractRoot)))) {
            filePathStream.forEach(contractPath -> {
                File contractFile = new File(String.valueOf(contractPath));
                if(contractFile.isDirectory()) return;
                byte[] contractBinary;
                try (FileInputStream inputStream = new FileInputStream(contractFile)) {
                    contractBinary = new byte[Math.toIntExact(contractFile.length())];
                    inputStream.read(contractBinary);

                    ContractVersion contractVersion = ContractVersion.of(contractBinary);
                    ContractMeta contractMeta = ContractClassLoader.loadContractById(
                            contractRoot, contractVersion);

                    if (Files.isRegularFile(contractPath)) {
                        if(contractMeta.getStateStore() !=null || contractMeta.getTxReceipt() !=null) {
                            contracts.put(contractVersion, contractMeta);
                        }
                    }

                } catch (IOException e) {
                    log.warn(e.getMessage());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<ContractVersion, ContractMeta> getContracts() {
        return contracts;
    }

    public List<ContractVersion> getAllContractIds() {
        return this.contracts.entrySet().stream().map(set -> set.getKey())
                .collect(Collectors.toList());
    }


    public ContractMeta getContractByVersion(ContractVersion id) {
        return contracts.get(id);
    }

    public Boolean isContract(ContractVersion id) {
        return contracts.containsKey(id);
    }

    //TODO validation
    public Boolean paramsValidation(String contractId, String method, Map params) {
        // TODO params validation
        ContractVersion id = ContractVersion.of(contractId);
        Boolean validationMethods = ContractUtils.contractValidation(
                contracts.get(id).getContractInstance());

        if (!isContract(id)) {
            //TODO add contract
            return false;
        } else {
            if (validationMethods) {

            }
        }
        return true;
    }

    //TODO query to contract store
    public void query(byte[] contract) {

    }
}
