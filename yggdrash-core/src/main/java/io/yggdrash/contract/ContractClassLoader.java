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

import io.yggdrash.crypto.HashUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class ContractClassLoader extends ClassLoader {

    public ContractClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class loadContract(String contractFullName, File contractFile) {
        byte[] classData = null;
        try {
            URL myUrl = contractFile.toURI().toURL();
            URLConnection connection = myUrl.openConnection();
            InputStream input = connection.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int data = input.read();

            while (data != -1) {
                buffer.write(data);
                data = input.read();
            }

            input.close();

            classData = buffer.toByteArray();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (classData == null) {
            return null;
        } else {
            return loadContract(contractFullName, classData);
        }
    }

    public Class loadContract(String contractFullName, byte[] b) {
        return defineClass(contractFullName, b, 0, b.length);
    }

    public static Class loadContractClass(String contractFullName, File contractFile) {
        ContractClassLoader loader = new ContractClassLoader(Object.class.getClassLoader());
        return loader.loadContract(contractFullName, contractFile);
    }

    public static byte[] contractHash(byte[] contractBytes) {
        return HashUtil.sha1(contractBytes);
    }

}
