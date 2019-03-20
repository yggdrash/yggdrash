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

package io.yggdrash.common.utils;

import io.yggdrash.common.contract.Contract;
import io.yggdrash.common.contract.methods.ContractMethod;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.contract.core.annotation.ContractStateStore;
import io.yggdrash.contract.core.annotation.ContractTransactionReceipt;
import io.yggdrash.contract.core.annotation.ParamValidation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContractUtils {

    public static Boolean contractValidation(Object contract) {
        Map<String, ContractMethod> validationMethods =
                ContractUtils.contractMethods(contract, ParamValidation.class);
        return validationMethods != null;
    }

    public static List<Field> txReceiptFields(Object contract) {
        return ContractUtils.contractFields(contract, ContractTransactionReceipt.class);
    }

    public static List<Field> stateStoreFields(Object contract) {
        return ContractUtils.contractFields(contract, ContractStateStore.class);
    }

    public static void updateContractFields(Contract contract, List<Field> fields, Object store) {
        // init state Store
        for (Field f : fields) {
            try {
                f.setAccessible(true);
                f.set(contract, store);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<Field> contractFields(Object contract, Class<? extends Annotation> annotationClass) {
        List<Field> fields = Arrays.stream(contract.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(annotationClass))
                .collect(Collectors.toList());
        return fields;
    }

    public static Map<String, ContractMethod> contractMethods(
            Object contract, Class<? extends Annotation> annotationClass) {
        return Arrays.stream(contract.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotationClass))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .collect(
                        Collectors.toMap(Method::getName, ContractMethod::new)
                );
    }

    public static Contract contractInstance(Contract contract) throws FailedOperationException {
        try {
            Contract instance = contract.getClass().getDeclaredConstructor().newInstance();
            return instance;
        } catch (Exception e) {
            e.printStackTrace();
            throw new FailedOperationException("Contract instance Fail");
        }
    }

//    public static byte[] convertVersionToBase64(String contractPath, BranchId branchId) {
//        File contractFile = contractFile(contractPath, branchId);
//        if (contractFile.exists()) {
//            byte[] fileArray = loadContract(contractFile);
//            return base64Enc(fileArray);
//        }
//        return null;
//    }
//
//    static File contractFile(String Path, BranchId branchId) {
//        String containerPath = String.format("%s/%s", Path, branchId);
//        String systemContractPath = String.format("%s/bundles%s", containerPath, "/system-contracts");
//
//        String result = String.format("%s/%s", systemContractPath, "system-coin-contract-1.0.0.jar");
//        return new File(result);
//    }
//
//    private static byte[] loadContract(File contractFile) {
//        byte[] contractBinary;
//        try (FileInputStream inputStream = new FileInputStream(contractFile)) {
//            contractBinary = new byte[Math.toIntExact(contractFile.length())];
//            inputStream.read(contractBinary);
//        } catch (IOException e) {
//            return null;
//        }
//        return contractBinary;
//    }
//
//    public static byte[] base64Enc(byte[] buffer) {
//        return Base64.encodeBase64(buffer);
//    }

}
