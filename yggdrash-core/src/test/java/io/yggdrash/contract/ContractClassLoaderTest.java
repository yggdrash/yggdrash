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
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



public class ContractClassLoaderTest {
    private static final Logger log = LoggerFactory.getLogger(ContractClassLoaderTest.class);

    @Test
    public void testContract() throws  IllegalAccessException, InstantiationException,
            InvocationTargetException {
        File contractNone =
                new File("./resources/contract/79ff1978e131b6d4de263daa7f3b598ea84097b6.class");
        ContractMeta noneContract = ContractClassLoader.loadContractClass(null, contractNone);
        Class<Contract> none = noneContract.getContract();
        log.debug(String.valueOf(Hex.encodeHex(noneContract.getContractId().array())));
        assertTrue("{}".equals(invokeTest(none)));
        Contract a = none.newInstance();
        Contract b = none.newInstance();
        assertFalse("Two Contract are not same instance.", a.equals(b));
    }

    @Test
    public void testConvertContractClassToContractMeta() throws IOException {
        Class c = NoneContract.class.getClass();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(c);
        oos.flush();

        byte[] classData = bos.toByteArray();

        ContractMeta classMeta = new ContractMeta(classData, c);
        log.debug(Hex.encodeHexString(classMeta.getContractId().array()));

    }


    public String invokeTest(Class a) throws InvocationTargetException, IllegalAccessException,
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
