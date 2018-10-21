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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ContractClassLoader extends ClassLoader {
    private static final Logger log = LoggerFactory.getLogger(ContractClassLoader.class);

    static Long MAX_FILE_LENGTH = 5242880L; // default 5MB bytes
    static String CONTRACT_PATH = "./resources/contract/";

    public ContractClassLoader(ClassLoader parent) {
        super(parent);
    }

    public ContractMeta loadContract(String contractFullName, File contractFile) {
        byte[] classData = null;
        try {
            // contract max file length is 5mb TODO change max byte
            if (contractFile.length() > ContractClassLoader.MAX_FILE_LENGTH) {
                return null;
            }
            FileInputStream inputStream = new FileInputStream(contractFile);
            classData = new byte[Math.toIntExact(contractFile.length())];
            inputStream.read(classData);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (classData == null) {
            return null;
        } else {
            return loadContract(contractFullName, classData);
        }
    }

    public ContractMeta loadContract(String contractFullName, byte[] b) {
        Class contract = defineClass(contractFullName, b, 0, b.length);
        return new ContractMeta(b, contract);

    }

    public static ContractMeta loadContractClass(String contractFullName, File contractFile) {
        ContractClassLoader loader =
                new ContractClassLoader(ContractClassLoader.class.getClassLoader());
        return loader.loadContract(contractFullName, contractFile);
    }

    public static ContractMeta loadContractById(String contractId) {
        // TODO Change Contract Path by resource path
        File contractFile = new File(CONTRACT_PATH + contractId + ".class");
        log.debug(contractFile.getAbsolutePath());
        if (contractFile.exists()) {
            return ContractClassLoader.loadContractClass(null, contractFile);
        } else {
            return null;
        }

    }
}
