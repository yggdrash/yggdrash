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

package io.yggdrash.contract;

import io.yggdrash.config.Constants;
import io.yggdrash.config.DefaultConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

public class ContractClassLoader extends ClassLoader {
    private static final Logger log = LoggerFactory.getLogger(ContractClassLoader.class);

    private static final Long MAX_FILE_LENGTH = 5242880L; // default 5MB bytes
    private static final String CONTRACT_PATH;

    static {
        CONTRACT_PATH =
                new DefaultConfig().getConfig().getString(Constants.CONTRACT_PATH);
        copyResoucesToContractPath();
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

    private static void copyResoucesToContractPath() {
        File targetDir = new File(CONTRACT_PATH);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new RuntimeException("Failed to create=" + targetDir.getAbsolutePath());
        }
        String[] contractFiles = {"4fc0d50cba2f2538d6cda789aa4955e88c810ef5.class",
                "9607aea1d4e358a594006c7926a07262b5258c31.class"};
        for (String contractFile : contractFiles) {
            URL resource = ContractClassLoader.class.getResource("/contract/" + contractFile);
            try {
                FileUtils.copyURLToFile(resource, new File(targetDir, contractFile));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static ContractMeta loadContractClass(String contractFullName, File contractFile) {
        ContractClassLoader loader =
                new ContractClassLoader(ContractClassLoader.class.getClassLoader());
        return loader.loadContract(contractFullName, contractFile);
    }

    public static ContractMeta loadContractById(String contractId) {
        File contractFile = new File(CONTRACT_PATH, contractId + ".class");
        log.debug("Load contract={}", contractFile.getAbsolutePath());
        if (contractFile.exists()) {
            return ContractClassLoader.loadContractClass(null, contractFile);
        } else {
            return null;
        }
    }
}
