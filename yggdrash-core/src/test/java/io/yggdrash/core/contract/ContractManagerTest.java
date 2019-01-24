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

package io.yggdrash.core.contract;

import com.google.gson.JsonObject;
import io.yggdrash.common.config.DefaultConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import io.yggdrash.common.util.ContractUtils;
import io.yggdrash.common.util.JsonUtil;
import io.yggdrash.core.store.StoreBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class ContractManagerTest {
    Logger log = LoggerFactory.getLogger(ContractManagerTest.class);

    public static DefaultConfig defaultConfig = new DefaultConfig();
    public static ContractManager contractManager = new ContractManager(defaultConfig.getContractPath());

    @Test
    public void loadTest() {
        if (defaultConfig.isProductionMode()) {
            ContractClassLoader.copyResourcesToContractPath(defaultConfig.getContractPath());
        }
        new ContractManager(defaultConfig.getContractPath());
    }

    @Test
    public void getContractsTest() {
        contractManager.getContracts();
    }

    @Test
    public void getContractIds() {
        List<String> contractIdList = contractManager.getContractIds();
        List<String> sampleContractIdList = contractSample();
        if (!contractIdList.isEmpty() && !sampleContractIdList.isEmpty()) {
            if (contractIdList == null || sampleContractIdList == null) return;
            contractIdList.sort(Comparator.naturalOrder());
            sampleContractIdList.sort(Comparator.naturalOrder());
            Boolean is = listEquals(contractIdList, sampleContractIdList);
            assertEquals(true, is);

            contractIdList.add("1378d5ac6e6b7b536165a9a9225684dc93206262");
            Boolean isnt = listEquals(contractIdList, sampleContractIdList);
            assertEquals(false, isnt);
        }
    }

    @Test
    public void getContractById() {
        List<String> contractIdList = contractManager.getContractIds();
        if (!contractIdList.isEmpty()) {
            if (contractIdList == null) return;
            String id = contractIdList.get(0);
            String paramStr = "{\"contractId\" :" + "\"" + id + "\""  +"}";
            contractManager.getContractById(createParams(paramStr));
        }
    }

    @Test
    public void getMethod() {
        List<String> contractIdList = contractManager.getContractIds();
        if (!contractIdList.isEmpty()) {
            if (contractIdList == null) return;
            String id = contractIdList.get(0);
            String paramStr = "{\"contractId\" :" + "\"" + id + "\"" +"}";
            contractManager.getMethod(createParams(paramStr));
        }
    }

    @Test
    public void isContract() {
        List<String> contractIdList = contractManager.getContractIds();
        if (!contractIdList.isEmpty()) {
            if (contractIdList == null) return;
            String id = contractIdList.get(0);
            String paramStr = "{\"contractId\" :" + "\"" + id + "\""  +"}";
            Boolean is = contractManager.isContract(createParams(paramStr));
            assertEquals(true, is);

            String paramStr2 = "{\"contractId\" : \"1378d5ac6e6b7b536165a9a9225684dc93206262\"}";
            Boolean isnt = contractManager.isContract(createParams(paramStr2));
            assertEquals(false, isnt);
        }
    }

    @Test
    public void contractValidation() {
        Map params = createParams("owner", "cee3d4755e47055b530deeba062c5bd0c17eb00f");
        params.put("spender", "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e");
        contractManager.paramsValidation("allowance", params);
    }

    private static Map createParams(String key, String value) {
        Map params = new HashMap();
        params.put(key, value);
        return params;
    }

    private JsonObject createParams(String paramStr) {
        return JsonUtil.parseJsonObject(paramStr);
    }

    private static boolean listEquals(List<String> list1, List<String> list2) {
        if(list1.size() != list2.size())
            return false;

        for (String id : list1) {
            if(!list2.contains(id))
                return false;
        }
        return true;
    }

    private List<String> contractSample() {
        DefaultConfig defaultConfig = new DefaultConfig();
        List<String> cIds = new ArrayList<>();
        try (Stream<Path> filePathStream = Files.walk(Paths.get(String.valueOf(defaultConfig.getContractPath())))) {
            filePathStream.forEach(contractPath -> {
                File contractFile = new File(String.valueOf(contractPath));
                if(contractFile.isDirectory()) return;
                byte[] contractBinary;
                try (FileInputStream inputStream = new FileInputStream(contractFile)) {
                    contractBinary = new byte[Math.toIntExact(contractFile.length())];
                    inputStream.read(contractBinary);

                    ContractId contractId = ContractId.of(contractBinary);
                    ContractMeta contractMeta = ContractClassLoader.loadContractById(
                            defaultConfig.getContractPath(), contractId);

                    if(contractMeta.getStateStore() !=null || contractMeta.getTxReceipt() !=null) {
                        String id = contractId.toString();
                        cIds.add(id);
                    }
                } catch (IOException e) {
                    log.warn(e.getMessage());
                }
            });
            return cIds;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}


