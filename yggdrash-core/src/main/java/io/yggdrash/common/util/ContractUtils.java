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

import io.yggdrash.core.contract.Contract;
import io.yggdrash.core.contract.methods.ContractMethod;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.runtime.annotation.ContractStateStore;
import io.yggdrash.core.runtime.annotation.ContractTransactionReceipt;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContractUtils {

    public static List<Field> txReceipt(Object contract) {
        return ContractUtils.contractFields(contract, ContractTransactionReceipt.class);
    }

    public static List<Field> stateStore(Object contract) {
        return ContractUtils.contractFields(contract, ContractStateStore.class);
    }


    public static List<Field> contractFields(Object contract, Class<? extends Annotation> annotationClass) {
        List<Field> txReceipt = Arrays.stream(contract.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(annotationClass))
                .collect(Collectors.toList());
        return txReceipt;
    }


    public static  Map<String, ContractMethod> contractMethods(Object contract, Class<? extends Annotation> annotationClass) {
        return Arrays.stream(contract.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotationClass))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .collect(
                        Collectors.toMap(m -> m.getName().toLowerCase(), m -> new ContractMethod(m))
                );
    }

    public static Contract contractInstance(Contract contract) throws FailedOperationException {
        try {
            Contract instance = contract.getClass().getDeclaredConstructor().newInstance();
            return instance;
        } catch (Exception e) {
            e.printStackTrace();
            throw new FailedOperationException("Contract instance");
        }
    }

}
