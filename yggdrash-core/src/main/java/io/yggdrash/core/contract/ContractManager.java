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

import com.google.gson.JsonObject;
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

    private Map<ContractId,ContractMeta> contractMap = new HashMap<>();

    public ContractManager(String contractPath) {
        load(contractPath);
    }

    private void load(String contractRoot) {
        try (Stream<Path> filePathStream = Files.walk(Paths.get(String.valueOf(contractRoot)))) {
            filePathStream.forEach(contractPath -> {
                File contractFile = new File(String.valueOf(contractPath));
                if(contractFile.isDirectory()) {
                    return;
                }
                byte[] contractBinary;
                try (FileInputStream inputStream = new FileInputStream(contractFile)) {
                    contractBinary = new byte[Math.toIntExact(contractFile.length())];
                    inputStream.read(contractBinary);

                    ContractId contractId = ContractId.of(contractBinary);
                    ContractMeta contractMeta = ContractClassLoader.loadContractById(
                            contractRoot, contractId);

                    if (Files.isRegularFile(contractPath)) {
                        contractMap.put(contractId, contractMeta);
                    }

                } catch (IOException e) {
                    log.warn(e.getMessage());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ContractMeta getContractById(ContractId cId) {
        ContractMeta meta = contractMap.get(cId);
        return  meta;
    }

    public ContractMeta getContractById(JsonObject params) {
        return getContractById(ContractId.of(params.get("contractId").getAsString()));
    }

    public Map<String, String> getMethod(String method) {
        return null;
    }

    public Boolean isContract(JsonObject params) {
        ContractId contractId = ContractId.of(params.get("contractId").getAsString());
        return contractMap.get(contractId) != null;
    }


    public TransactionReceipt deploy() {
        return null;
    }

    public Boolean selfdestruct() {
        return null;
    }

}
