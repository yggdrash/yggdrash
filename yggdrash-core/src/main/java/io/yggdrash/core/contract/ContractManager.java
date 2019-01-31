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

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.yggdrash.core.contract.methods.ContractMethod;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class ContractManager extends ClassLoader {
    private static final Logger log = LoggerFactory.getLogger(ContractManager.class);
    private Map<ContractId, ContractMeta> contracts = new HashMap<>();
    private String contractPath;

    public ContractManager(String contractPath) {
        this.contractPath = contractPath;
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

                    ContractId contractVersion = ContractId.of(contractBinary);
                    ContractMeta contractMeta = ContractClassLoader.loadContractById(
                            contractRoot, contractVersion);

                    if (Files.isRegularFile(contractPath) && validation(contractMeta)) {
                        contracts.put(contractVersion, contractMeta);
                    }

                } catch (IOException e) {
                    log.warn(e.getMessage());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<ContractId, ContractMeta> getContracts() {
        return this.contracts;
    }

    public List<ContractId> getContractIdList() {
        return this.contracts.entrySet().stream().map(set -> set.getKey())
                .collect(Collectors.toList());
    }

    public List<ContractMeta> getContractList() {
        return this.contracts.entrySet().stream().map(set -> set.getValue())
                .collect(Collectors.toList());
    }

    public ContractMeta getContractById(ContractId id) {
        return this.contracts.get(id);
    }

    public Boolean isContract(ContractId id) {
        return this.contracts.containsKey(id);
    }

    /**
     * Check the requirements required by the contract
     */
    public Boolean validation(ContractMeta contractMeta) {
        if(contractMeta.getStateStore() == null) {
            log.error("Contract does not have required filed state store");
            return false;
        }
        if(contractMeta.getTxReceipt() == null) {
            log.error("Contract does not have required filed transaction receipt");
            return false;
        }

        for (Map.Entry<String, ContractMethod> elem :
                contractMeta.getQueryMethods().entrySet()) {
            if (elem.getValue().getMethod().getReturnType().equals(Void.TYPE)) {
                return false;
            }
        }

        for (Map.Entry<String, ContractMethod> elem :
                contractMeta.getInvokeMethods().entrySet()) {
            if(elem.getValue().getMethod().getParameterTypes().length < 1) {
                return false;
            }
        }

        //TODO whitelist sandBox validtaion
        return true;
    }

    /**
     * Add a contract that the manager does not have
     * or Adding a contract from contractRequest
     */
    public void addContract(Class<? extends Contract> contract) {
        //TODO check the node admin
        File targetDir = new File(contractPath);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new RuntimeException("Failed to create=" + targetDir.getAbsolutePath());
        }
        ContractMeta contractMeta = ContractClassLoader.loadContractClass(contract);
        ContractId contractId = contractMeta.getContractId();
        File contractFile = ContractMeta.contractFile(contractPath, contractId);
        if (!contractFile.exists()) {
            try {
                FileUtils.writeByteArrayToFile(contractFile, contractMeta.getContractBinary());
                this.contracts.put(contractId, contractMeta);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Change the contract to the contract version.
     */
    public ContractId convertContractToVersion(Class<? extends Contract> contract) {
        ContractMeta contractMeta = ContractClassLoader.loadContractClass(contract);
        return contractMeta.getContractId();
    }

    public Boolean removeContract(ContractId contractVersion) {
        //TODO check the node admin
        String directoryPath = contractVersion.toString().substring(0, 2);
        String filePath = contractVersion.toString().substring(0, 2) + File.separator
                + contractVersion + ".class";
        File file = new File(contractPath + File.separator + filePath);
        File directory = new File(contractPath + File.separator + directoryPath);
        if (file.exists()){
            if(file.isDirectory()){
                File[] files = file.listFiles();
                for( int i=0; i<files.length; i++){
                    files[i].delete();
                }
            }
            file.delete();
            directory.delete();
            this.contracts.remove(contractVersion);
            return true;
        }
        return false;
    }

    //TODO
    // 1. contract request to another node
    // 2. Query to branch

}
