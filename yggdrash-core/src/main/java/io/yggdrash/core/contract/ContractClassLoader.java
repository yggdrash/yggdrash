/*
 * Copyright 2018 Akashic Foundation
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
import io.yggdrash.core.exception.NonExistObjectException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ContractClassLoader extends ClassLoader {
    private static final Logger log = LoggerFactory.getLogger(ContractClassLoader.class);
    private static final Class[] CONTRACTS = {StemContract.class,
            NoneContract.class, CoinStandardContract.class};
    private static final Long MAX_FILE_LENGTH = 5242880L; // default 5MB bytes

    static {
        copyResourcesToContractPath(new DefaultConfig().getContractPath());
    }

    private ContractClassLoader(ClassLoader parent) {
        super(parent);
    }

    private ContractMeta loadContract(String contractFullName, File contractFile) {
        // contract max file length is 5mb TODO change max byte
        if (contractFile.length() > ContractClassLoader.MAX_FILE_LENGTH) {
            return null;
        }
        byte[] classData;
        try (FileInputStream inputStream = new FileInputStream(contractFile)) {
            classData = new byte[Math.toIntExact(contractFile.length())];
            inputStream.read(classData);
        } catch (IOException e) {
            log.warn(e.getMessage());
            return null;
        }

        return loadContract(contractFullName, classData);
    }

    private ContractMeta loadContract(String contractFullName, byte[] b) {
        Class contract = defineClass(contractFullName, b, 0, b.length);
        return new ContractMeta(b, contract);
    }

    public static void copyResourcesToContractPath(String contractPath) {
        File targetDir = new File(contractPath);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new RuntimeException("Failed to create=" + targetDir.getAbsolutePath());
        }
        for (Class contract : CONTRACTS) {
            log.debug("copyResourcesToContractPath :: contract => " + contract);
            ContractMeta contractMeta = loadContractClass(contract);
            String contractId = contractMeta.getContractId().toString();
            log.debug("copyResourcesToContractPath :: contractId => " + contractId);
            File contractFile = ContractMeta.contractFile(contractPath, contractId);
            if (!contractFile.exists()) {
                try {
                    FileUtils.writeByteArrayToFile(contractFile, contractMeta.getContractBinary());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    static ContractMeta loadContractClass(Class<? extends Contract> clazz) {
        String resourcePath = ContractMeta.classAsResourcePath(clazz);
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(resourcePath)) {
            byte[] bytes = IOUtils.toByteArray(is);
            return new ContractMeta(bytes, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static ContractMeta loadContractClass(String contractFullName, File contractFile) {
        ContractClassLoader loader =
                new ContractClassLoader(ContractClassLoader.class.getClassLoader());
        return loader.loadContract(contractFullName, contractFile);
    }

    public static ContractMeta loadContractById(String contractPath, String contractId) {
        File contractFile = ContractMeta.contractFile(contractPath, contractId);
        log.debug("Load contract={}", contractFile.getAbsolutePath());
        if (contractFile.exists()) {
            return ContractClassLoader.loadContractClass(null, contractFile);
        } else {
            throw new NonExistObjectException(contractFile.getAbsolutePath());
        }
    }
}
