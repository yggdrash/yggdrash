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

package io.yggdrash.contract;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(ContractClassLoaderTest.class);

    @Test
    public void testContract() throws  IllegalAccessException, InstantiationException,
            InvocationTargetException {
        File contractNone =
                new File(".yggdrash/contract/9607aea1d4e358a594006c7926a07262b5258c31.class");
        ContractMeta noneContract = ContractClassLoader.loadContractClass(null, contractNone);
        Class<? extends Contract> none = noneContract.getContract();

        assertEquals("9607aea1d4e358a594006c7926a07262b5258c31",
                noneContract.getContractId().toString());
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
        ContractMeta classMeta = ContractClassLoader.loadContractById(
                "4fc0d50cba2f2538d6cda789aa4955e88c810ef5");
        assertNotNull(classMeta);
        assertEquals("4fc0d50cba2f2538d6cda789aa4955e88c810ef5",
                classMeta.getContractId().toString());
        assertEquals("io.yggdrash.contract.StemContract", classMeta.getContract().getName());

        // LOAD None Contract
        classMeta = ContractClassLoader.loadContractById(
                "9607aea1d4e358a594006c7926a07262b5258c31");
        assertNotNull(classMeta);
        assertEquals("9607aea1d4e358a594006c7926a07262b5258c31",
                classMeta.getContractId().toString());
        assertEquals("io.yggdrash.contract.NoneContract", classMeta.getContract().getName());
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
