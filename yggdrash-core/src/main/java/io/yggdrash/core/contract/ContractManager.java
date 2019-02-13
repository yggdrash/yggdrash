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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
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
    private Map<ContractVersion, ContractMeta> contracts = new HashMap<>();
    private String contractPath;

    public ContractManager(String contractPath) {
        this.contractPath = contractPath;
        load();
    }

    private void load() {
        File targetDir = new File(this.contractPath);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new RuntimeException("Failed to create=" + targetDir.getAbsolutePath());
        }
        log.info("ContractManager load path : {} ", contractPath);
        try (Stream<Path> filePathStream = Files.walk(Paths.get(String.valueOf(this.contractPath)))) {
            filePathStream.forEach(p -> {
                File contractFile = new File(String.valueOf(p));
                if(contractFile.isDirectory()) return;
                byte[] contractBinary;
                try (FileInputStream inputStream = new FileInputStream(contractFile)) {
                    contractBinary = new byte[Math.toIntExact(contractFile.length())];
                    inputStream.read(contractBinary);

                    ContractVersion contractVersion = ContractVersion.of(contractBinary);
                    ContractMeta contractMeta = ContractClassLoader.loadContractById(
                            this.contractPath, contractVersion);

                    if (Files.isRegularFile(p) && validation(contractMeta)) {
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

    public Map<ContractVersion, ContractMeta> getContracts() {
        return this.contracts;
    }

    public List<ContractVersion> getContractIdList() {
        return this.contracts.entrySet().stream().map(set -> set.getKey())
                .collect(Collectors.toList());
    }

    public List<ContractMeta> getContractList() {
        return this.contracts.entrySet().stream().map(set -> set.getValue())
                .collect(Collectors.toList());
    }

    public ContractMeta getContractById(ContractVersion version) {
        return this.contracts.get(version);
    }

    public Boolean isContract(ContractVersion version) {
        return this.contracts.containsKey(version);
    }

    /**
     * Check the requirements required by the contract
     */
    public Boolean validation(ContractMeta contractMeta) {
        if (contractMeta.getStateStore() == null) {
            throw new IllegalArgumentException("Contract does not have required filed state store.");
        }
        if (contractMeta.getTxReceipt() == null) {
            throw new IllegalArgumentException("Contract does not have required filed transaction receipt.");
        }

        for (Map.Entry<String, ContractMethod> elem :
                contractMeta.getQueryMethods().entrySet()) {
            if (elem.getValue().getMethod().getReturnType().equals(Void.TYPE)) {
                throw new IllegalArgumentException("Invoke method should not return void.");
            }
        }

        for (Map.Entry<String, ContractMethod> elem :
                contractMeta.getInvokeMethods().entrySet()) {
            if (elem.getValue().getMethod().getParameterTypes().length < 1) {
                throw new IllegalArgumentException("The query method must have a return type.");
            }
        }

        //TODO whitelist sandBox validtaion
        // Check the Project Jigsaw
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
        ContractVersion contractVersion = contractMeta.getContractVersion();
        File contractFile = ContractMeta.contractFile(contractPath, contractVersion);
        if (!contractFile.exists()) {
            try {
                FileUtils.writeByteArrayToFile(contractFile, contractMeta.getContractBinary());
                this.contracts.put(contractVersion, contractMeta);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Boolean removeContract(ContractVersion contractVersion) {
        //TODO check the node admin
        String directoryPath = contractVersion.toString().substring(0, 2);
        String filePath = contractVersion.toString().substring(0, 2) + File.separator
                + contractVersion + ".class";
        File file = new File(contractPath + File.separator + filePath);
        File directory = new File(contractPath + File.separator + directoryPath);
        if (file.exists()){
            if(file.isDirectory()){
                File[] files = file.listFiles();
                Arrays.stream(files)
                        .forEach(f -> f.delete());
            }
            file.delete();
            directory.delete();
            this.contracts.remove(contractVersion);
            return true;
        }
        return false;
    }

    /**
     * Change the contract to the contract version.
     */
    public ContractVersion convertContractToVersion(Class<? extends Contract> contract) {
        ContractMeta contractMeta = ContractClassLoader.loadContractClass(contract);
        return contractMeta.getContractVersion();
    }

    /**
     * Decompiling a contract file
     */
    public ContractMeta decompileContract(String encodedString) throws UnsupportedEncodingException {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] decodedBytes = decoder.decode(encodedString);
        ContractVersion contractVersion = ContractVersion.of(new String(decodedBytes, "UTF-8"));
        return contracts.get(contractVersion);
    }

    //TODO
    // contract request to another node

}
