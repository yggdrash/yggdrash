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

import io.yggdrash.common.LoadClassTest;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContractClassLoaderTest {
    private static final Logger log = LoggerFactory.getLogger(ContractClassLoaderTest.class);

    @Test
    public void testFilePath() throws IllegalAccessException, InstantiationException, InvocationTargetException {
        File d1 = new File("./src/test/resources/LoadClassTest1.class"); // test1
        Class a = ContractClassLoader.loadContractClass(null, d1);

        assertTrue("Test1".equals(invokeTest(a)));
        LoadClassTest t = new LoadClassTest(); // test3
        assertTrue("Test3".equals(t.test()));
        File d2 = new File("./src/test/resources/LoadClassTest2.class"); // test2
        Class b = ContractClassLoader.loadContractClass(null, d2);
        assertTrue("Test3".equals(t.test()));
        assertTrue(a.getName().equals(b.getName()) && a.getName().equals(t.getClass().getName()));
        assertFalse(b.isInstance(a.newInstance()));
        log.debug(a.getTypeName());
    }

    public String invokeTest(Class a) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        Object t = a.newInstance();
        Method[] ms = a.getDeclaredMethods();
        for(Method m : ms) {
            if("test".equals(m.getName())) {
                m.setAccessible(true);
                return m.invoke(t).toString();
            }
        }
        return null;
    }
}
