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
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class ContractClassLoaderTest {
    private static final String STEM_CONTRACT = "d399cd6d34288d04ba9e68ddfda9f5fe99dd778e";
    private static final String COIN_CONTRACT = "2e7e9fe79925fde90c61c3e13ad65140b8cd1522";
    private static final String NONE_CONTRACT = "91d64aa71525f24da7c1c63620705039b4fb93e9";

    @Test
    public void testContract() throws  IllegalAccessException, InstantiationException,
            InvocationTargetException {
        File contractNone =
                new File(".yggdrash/contract/91d64aa71525f24da7c1c63620705039b4fb93e9.class");
        ContractMeta noneContract = ContractClassLoader.loadContractClass(null, contractNone);
        Class<? extends Contract> none = noneContract.getContract();

        assertEquals(NONE_CONTRACT, noneContract.getContractId().toString());
        assertEquals("{}", invokeTest(none));
        Contract a = none.newInstance();
        Contract b = none.newInstance();
        assertNotEquals("Two Contract are not same instance.", a, b);
    }

    @Test
    public void testConvertContractClassToContractMeta() throws IOException {
        Class<? extends Contract> c = NoneContract.class;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(c);
        oos.flush();

        byte[] classData = bos.toByteArray();
        ContractId idByClassBinary = ContractId.of(classData);
        ContractMeta classMeta = new ContractMeta(classData, c);
        assertEquals(idByClassBinary, classMeta.getContractId());
    }

    @Test
    public void testLoadByHash() {
        // LOAD Stem Contract
        ContractMeta classMeta = ContractClassLoader.loadContractById(STEM_CONTRACT);
        assertNotNull(classMeta);
        assertEquals(STEM_CONTRACT, classMeta.getContractId().toString());
        assertEquals("io.yggdrash.core.contract.StemContract", classMeta.getContract().getName());

        // LOAD Coin Contract
        classMeta = ContractClassLoader.loadContractById(COIN_CONTRACT);
        assertNotNull(classMeta);
        assertEquals(COIN_CONTRACT, classMeta.getContractId().toString());
        assertEquals("io.yggdrash.core.contract.CoinContract", classMeta.getContract().getName());
    }

    private String invokeTest(Class a) throws InvocationTargetException, IllegalAccessException,
            InstantiationException {
        Object t = a.newInstance();
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
