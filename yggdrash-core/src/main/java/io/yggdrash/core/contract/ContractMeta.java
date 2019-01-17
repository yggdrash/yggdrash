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

import io.yggdrash.common.util.ContractUtils;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.runtime.annotation.ContractQuery;
import io.yggdrash.core.runtime.annotation.InvokeTransction;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ContractMeta {
    private static final String SUFFIX = ".class";

    private final Class<? extends Contract> contractClass;
    private final byte[] contractBinary;
    private final String contractClassName;
    private final ContractId contractId;
    private Map<String, Method> invokeMethod;
    private Map<String, Method> queryMethod;

    ContractMeta(byte[] contractBinary, Class<? extends Contract> contractClass) {
        this.contractBinary = contractBinary;
        this.contractClass = contractClass;
        this.contractClassName = contractClass.getName();
        this.contractId = ContractId.of(contractBinary);
        this.queryMethod = getQueryMethods();
        this.invokeMethod = getInvokeMethods();
    }

    public Class<? extends Contract> getContract() {
        return contractClass;
    }

    ContractId getContractId() {
        return contractId;
    }

    byte[] getContractBinary() {
        return contractBinary;
    }

    static File contractFile(String rootPath, ContractId contractId) {
        String filePath = contractId.toString().substring(0, 2) + File.separator
                + contractId + SUFFIX;
        return new File(rootPath + File.separator + filePath);
    }

    static String classAsResourcePath(Class<? extends Contract> clazz) {
        return clazz.getName().replace(".", "/") + SUFFIX;
    }

    public Contract getContractInstance() {
        Contract contract;
        try {
            contract = getContract().getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
                | InvocationTargetException e) {
            throw new FailedOperationException(e);
        }
        return contract;
    }

    public Map getMethods() {
        Map<String, List<Map<String, String>>> methods = new HashMap<>();

        methods.put("invoke", ContractUtils.methodInfo(invokeMethod));
        methods.put("query", ContractUtils.methodInfo(queryMethod));
        return methods;
    }

    public Map<String, Method> getInvokeMethods() {
        return ContractUtils.contractMethods(getContractInstance(), InvokeTransction.class);
    }


    public Map<String, Method> getQueryMethods() {
        return ContractUtils.contractMethods(getContractInstance(), ContractQuery.class);
    }
}
