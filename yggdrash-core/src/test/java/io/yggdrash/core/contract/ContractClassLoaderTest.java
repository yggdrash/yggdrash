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

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.Contract;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@Ignore
public class ContractClassLoaderTest {
    private static final Logger log = LoggerFactory.getLogger(ContractClassLoaderTest.class);
    private static final String NONE_CONTRACT = "3fccace3211be93c9a6372d8aa085e3914f08ca1";

    @Test
    @Ignore
    public void testContract() throws Exception {

        ContractVersion noneContractVersion = ContractVersion.of(NONE_CONTRACT);
        File contractNone = ContractMeta.contractFile(".yggdrash/contract", noneContractVersion);
        assertEquals(".yggdrash/contract/3f/" + NONE_CONTRACT + ".class",
                contractNone.getPath());

        ContractMeta noneContract = ContractClassLoader.loadContractClass(null, contractNone);
        Class<? extends Contract> none = noneContract.getContract();

        assertEquals(NONE_CONTRACT, noneContract.getContractVersion().toString());
        assertEquals("{}", invokeTest(none));
        Contract a = none.getDeclaredConstructor().newInstance();
        Contract b = none.getDeclaredConstructor().newInstance();
        assertNotEquals("Two Contract are not same instance.", a, b);
    }

    @Test
    public void testConvertContractClassToContractMeta() throws IOException {
        Class<? extends Contract> c = NoneContract.class;
        byte[] classData;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(c);
            oos.flush();

            classData = bos.toByteArray();
        }
        ContractVersion idByClassBinary = ContractVersion.of(classData);
        ContractMeta classMeta = new ContractMeta(classData, c);
        assertEquals(idByClassBinary, classMeta.getContractVersion());
    }

    @Test
    public void testLoadByHash() {
        // LOAD Stem Contract
        ContractMeta classMeta = ContractClassLoader.loadContractClass(StemContract.class);
        assertNotNull(classMeta);
        log.debug("StemContract.class id={}", classMeta.getContractVersion().toString());
        assertEquals("io.yggdrash.core.contract.StemContract", classMeta.getContract().getName());

        // LOAD Coin Contract
        classMeta = ContractClassLoader.loadContractClass(DPoAContract.class);
        assertNotNull(classMeta);
        log.debug("CoinContract.class id={}", classMeta.getContractVersion().toString());
        assertEquals("io.yggdrash.core.contract.CoinContract", classMeta.getContract().getName());
    }

    private String invokeTest(Class<? extends Contract> a) throws Exception {
        Object t = a.getDeclaredConstructor().newInstance();
        Method[] ms = a.getDeclaredMethods();
        for (Method m : ms) {
            if ("query".equals(m.getName())) {
                m.setAccessible(true);
                return m.invoke(t, new JsonObject()).toString();
            }
        }
        return null;
    }
}
