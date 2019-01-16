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
import io.yggdrash.common.util.ContractUtils;
import io.yggdrash.core.runtime.annotation.ContractQuery;
import io.yggdrash.core.runtime.annotation.InvokeTransction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class ContractManager<T> extends ClassLoader {
    private static final Logger log = LoggerFactory.getLogger(ContractManager.class);

    private final File contractRoot;
    private final List<ContractId> contractIds = new ArrayList<>();
    private final List<Object> contracts = new ArrayList<>();
    private Map<ContractId, Object> contractList = new Hashtable<>();
    private Map<String, Method> invokeMethod = new Hashtable<>();
    private Map<String, Method> queryMethod = new Hashtable<>();
    private Map<ContractId, Map<Map<String, Method>, JsonObject>> contractCall = new Hashtable<>();

    public ContractManager(String contractPath) {
        this.contractRoot = new File(contractPath);
        if (!contractRoot.exists()) {
            contractRoot.mkdirs();
        }

        load();
    }

    private void load() {
        try (Stream<Path> filePathStream = Files.walk(Paths.get(String.valueOf(contractRoot)))) {
            filePathStream.forEach(contractPath -> {
                File contractFile = new File(String.valueOf(contractPath));
                byte[] contractBinary;
                try (FileInputStream inputStream = new FileInputStream(contractFile)) {
                    contractBinary = new byte[Math.toIntExact(contractFile.length())];
                    inputStream.read(contractBinary);

                    Class contractClass = defineClass(null, contractBinary, 0, contractBinary.length);
                    ContractMeta contractMeta = new ContractMeta(contractBinary, contractClass);

                    if (Files.isRegularFile(contractPath)) {
                        contractIds.add(contractMeta.getContractId());
                        contracts.add(contractClass.getDeclaredConstructor().newInstance());
                        contractList.put(contractMeta.getContractId(), contractClass.getDeclaredConstructor().newInstance());
                        invokeMethod = getInvokeMethods(contractClass.getDeclaredConstructor().newInstance());
                        queryMethod = getQueryMethods(contractClass.getDeclaredConstructor().newInstance());
                    }
                } catch (IOException | NoSuchMethodException | InstantiationException | IllegalAccessException
                        | InvocationTargetException e) {
                    log.warn(e.getMessage());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Method> getInvokeMethods(Object contract) {
//        Set<Map.Entry<ContractId, Object>> set = contractList.entrySet();
//        Iterator<Map.Entry<ContractId, Object>> itr = set.iterator();
//
//        while (itr.hasNext()) {
//            Map.Entry<ContractId, Object> e = itr.next();
//            System.out.println("contract id : " + e.getKey() + ", contract class : " + e.getValue());
//        }
        return ContractUtils.contractMethods(contract, InvokeTransction.class);
    }

    private Map<String, Method> getQueryMethods(Object contract) {
        return ContractUtils.contractMethods(contract, ContractQuery.class);
    }

}
