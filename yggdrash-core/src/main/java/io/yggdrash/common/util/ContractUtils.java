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

package io.yggdrash.common.util;

import io.yggdrash.core.contract.ContractMeta;
import io.yggdrash.core.contract.Contract;
import io.yggdrash.core.contract.methods.ContractMethod;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.runtime.annotation.ContractStateStore;
import io.yggdrash.core.runtime.annotation.ContractTransactionReceipt;
import io.yggdrash.core.runtime.annotation.ParamValidation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class ContractUtils {

    public static Boolean contractValidation(Object contract) {
        Map<String, ContractMethod> validationMethods =
                ContractUtils.contractMethods(contract, ParamValidation.class);
        if (validationMethods == null) return false;
        return true;
    }

    public static List<Field> txReceiptFields(Object contract) {
        return ContractUtils.contractFields(contract, ContractTransactionReceipt.class);
    }

    public static List<Field> stateStoreFields(Object contract) {
        return ContractUtils.contractFields(contract, ContractStateStore.class);
    }

    public static void updateContractFields(Contract contract, List<Field> fields, Object store) {
        // init state Store
        for(Field f : fields) {
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

    public static  Map<String, ContractMethod> contractMethods(Object contract, Class<? extends Annotation> annotationClass) {
        return Arrays.stream(contract.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotationClass))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .collect(
                        Collectors.toMap(m -> m.getName(), m -> new ContractMethod(m))
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

    public static List<Map<String, String>> methodInfo(Map<String, ContractMethod> method) {
        List<Map<String, String>> methodList = new ArrayList<>();
        for(Map.Entry<String, ContractMethod> elem : method.entrySet()){
            Map<String, String> methodInfo = new HashMap<>();
            methodInfo.put("name", elem.getKey());
            methodInfo.put("outputType", elem.getValue().getMethod().getReturnType().getSimpleName());
            if (elem.getValue().isParams()) {
                methodInfo.put("inputType", elem.getValue()
                        .getMethod().getParameterTypes()[0].getSimpleName());
            }
            methodList.add(methodInfo);
        }
        return methodList;
    }

}
