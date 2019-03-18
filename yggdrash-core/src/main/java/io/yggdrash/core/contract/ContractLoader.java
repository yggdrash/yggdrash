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

import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.exception.NonExistObjectException;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ContractLoader {
    private static final Logger log = LoggerFactory.getLogger(ContractLoader.class);
    private static final String SUFFIX = ".jar";
    private static final String SUFFIX_SYSTEM_CONTRACT = "/system-contracts";
    private static final String SUFFIX_USER_CONTRACT = "/user-contracts";
    private static final String SUFFIX_UPDATE_CONTRACT = "/update-temp-contracts";
    private static String systemContractPath;
    private static String userContractPath;
    private static String containerPath;
    private static final Long MAX_FILE_LENGTH = 3145728L; // default 3MB bytes

    private static byte[] loadContract(File contractFile) {
        // contract max file length is 5mb TODO change max byte
        if (contractFile.length() > ContractLoader.MAX_FILE_LENGTH) {
            return null;
        }
        byte[] contractBinary;
        try (FileInputStream inputStream = new FileInputStream(contractFile)) {
            contractBinary = new byte[Math.toIntExact(contractFile.length())];
            inputStream.read(contractBinary);
        } catch (IOException e) {
            log.warn(e.getMessage());
            return null;
        }
        return contractBinary;
    }

    public static byte[] convertVersionToBase64(String contractPath, BranchId branchId) {
        File contractFile = contractFile(contractPath, branchId);
        log.debug("Load contract={}", contractFile.getAbsolutePath());
        if (contractFile.exists()) {
            byte[] fileArray = ContractLoader.loadContract(contractFile);
            return base64Enc(fileArray);
        } else {
            throw new NonExistObjectException(contractFile.getAbsolutePath());
        }
    }

    public static String convertVersionToBase64String(String contractPath, BranchId branchId) {
        File contractFile = contractFile(contractPath, branchId);
        log.debug("Load contract={}", contractFile.getAbsolutePath());
        if (contractFile.exists()) {
            byte[] fileArray = ContractLoader.loadContract(contractFile);
            return new String(base64Enc(fileArray));

        } else {
            throw new NonExistObjectException(contractFile.getAbsolutePath());
        }
    }

    static File contractFile(String Path, BranchId branchId) {
        containerPath = String.format("%s/%s", Path, branchId);
        systemContractPath = String.format("%s/bundles%s", containerPath, SUFFIX_SYSTEM_CONTRACT);

        String result = String.format("%s/%s", systemContractPath, "system-coin-contract-1.0.0.jar");
        return new File(result);
    }

//    public static File binaryToFile(byte[] binaryFile) {
//        if ((binaryFile == null)) {
//            return null;
//        }
//        FileOutputStream fos;
//
//        String tempContractPath = String.format("%s/bundles%s", containerPath, SUFFIX_UPDATE_CONTRACT);
//
//        File fileDir = new File(tempContractPath);
//        if (!fileDir.exists()) {
//            fileDir.mkdirs();
//        }
//
//        ContractVersion version = ContractVersion.of(binaryFile);
//
//        File destFile = new File(tempContractPath + File.separator + version + SUFFIX);
//        byte[] b64dec = base64Dec(binaryFile);
//
//        try {
//            fos = new FileOutputStream(destFile);
//            fos.write(binaryFile);
//            fos.close();
//        } catch (IOException e) {
//            log.error(e.toString());
//        }
//
//        return destFile;
//    }

    public static byte[] base64Dec(byte[] buffer) {
        return Base64.decodeBase64(buffer);
    }

    public static byte[] base64Enc(byte[] buffer) {
        return Base64.encodeBase64(buffer);
    }
}