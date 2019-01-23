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
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        contractManager.getContractIds();
        log.debug(contractManager.getContractIds().toString());
    }

    @Test
    public void getContractById() {
        List<String> contractIdList = contractManager.getContractIds();
        if (contractIdList != null) {
            final String paramStr = "{\"contractId\" :" + contractIdList.get(0) +"}";
            contractManager.getContractById(createParams(paramStr));
        }
    }

    @Test
    public void getMethod() {
        List<String> contractIdList = contractManager.getContractIds();
        if (contractIdList != null) {
            final String paramStr = "{\"contractId\" :" + contractIdList.get(0) +"}";
            contractManager.getMethod(createParams(paramStr));
        }
    }

    @Test
    public void isContract() {
        List<String> contractIdList = contractManager.getContractIds();
        if (contractIdList != null) {
            final String paramStr = "{\"contractId\" :" + contractIdList.get(0) +"}";
            Boolean is = contractManager.isContract(createParams(paramStr));
            assertEquals(true, is);

            final String f = "{\"contractId\" : \"1378d5ac6e6b7b536165a9a9225684dc93206262\"}";
            Boolean isnt = contractManager.isContract(createParams(f));
            assertEquals(false, isnt);
        }
    }

    private JsonObject createParams(String paramStr) {
        return JsonUtil.parseJsonObject(paramStr);
    }
}


