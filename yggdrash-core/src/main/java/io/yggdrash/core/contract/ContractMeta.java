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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.ContractUtils;
import io.yggdrash.core.contract.methods.ContractMethod;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.runtime.annotation.ContractQuery;
import io.yggdrash.core.runtime.annotation.InvokeTransaction;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;

public class ContractMeta {
    private static final String SUFFIX = ".class";

    private final Class<? extends Contract> contractClass;
    private final byte[] contractClassBinary;
    private final String contractClassName;
    private final ContractVersion contractVersion;
    private Map<String, ContractMethod> invokeMethod;
    private Map<String, ContractMethod> queryMethod;
    private Field transactionReceiptField;
    private Field contractStateStoreFiled;

    ContractMeta(byte[] contractClassBinary, Class<? extends Contract> contractClass) {
        this.contractClassBinary = contractClassBinary;
        this.contractClass = contractClass;
        this.contractClassName = contractClass.getName();
        this.contractVersion = ContractVersion.of(contractClassBinary);
        this.queryMethod = getQueryMethods();
        this.invokeMethod = getInvokeMethods();
    }

    public Class<? extends Contract> getContract() {
        return contractClass;
    }

    public ContractVersion getContractVersion() {
        return contractVersion;
    }

    byte[] getContractClassBinary() {
        return contractClassBinary;
    }

    static File contractFile(String rootPath, ContractVersion contractVersion) {
        String filePath = contractVersion.toString().substring(0, 2) + File.separator
                + contractVersion + SUFFIX;
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

    public Field getTxReceipt() {
        for (Field f : ContractUtils.txReceiptFields(getContractInstance())) {
            transactionReceiptField = f;
            f.setAccessible(true);
        }
        return transactionReceiptField;
    }

    public Field getStateStore() {
        for (Field f : ContractUtils.stateStoreFields(getContractInstance())) {
            contractStateStoreFiled = f;
            f.setAccessible(true);
        }
        return contractStateStoreFiled;
    }

    public Map<String, ContractMethod> getInvokeMethods() {
        return ContractUtils.contractMethods(getContractInstance(), InvokeTransaction.class);
    }


    public Map<String, ContractMethod> getQueryMethods() {
        return ContractUtils.contractMethods(getContractInstance(), ContractQuery.class);
    }

    /**
     * Convert from ContractMeta.class to JsonObject.
     * @return ContractMeta as JsonObject
     */
    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("contractVersion", this.contractVersion.toString());
        jsonObject.addProperty("name", this.contractClassName);

        JsonArray invokeArray = new JsonArray();
        JsonArray queryArray = new JsonArray();

        invokeMethod.forEach((key, value) -> {
            JsonObject invokeProperty = new JsonObject();

            invokeProperty.addProperty("name", key);

            if (value.isParams()) {
                JsonArray inputArray = new JsonArray();
                Arrays.stream(value.getMethod().getParameterTypes())
                        .forEach(type -> inputArray.add(type.getSimpleName()));
                invokeProperty.addProperty("params",
                        value.getMethod().getParameterTypes().length);
                invokeProperty.add("inputType", inputArray);
            }

            invokeProperty.addProperty("outputType",
                    value.getMethod().getReturnType().getSimpleName());
            invokeArray.add(invokeProperty);
        });


        queryMethod.forEach((key, value) -> {
            JsonObject queryProperty = new JsonObject();

            queryProperty.addProperty("name", key);
            queryProperty.addProperty("outputType",
                    value.getMethod().getReturnType().getSimpleName());
            queryArray.add(queryProperty);
        });


        jsonObject.add("invokeMethods", invokeArray);
        jsonObject.add("queryMethods", queryArray);

        return jsonObject;
    }

    /**
     * Print ContractMeta.
     */
    public String toString() {
        return this.toJsonObject().toString();
    }

    /**
     * Print ContractMeta to pretty JsonObject.
     */
    String toStringPretty() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this.toJsonObject());
    }

}
