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

import io.yggdrash.core.store.StoreBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ContractLoader extends ClassLoader {
    private static final Logger log = LoggerFactory.getLogger(ContractLoader.class);

    private final File contractRoot;
    private final List<Path> contractList = new ArrayList<Path>();

    public ContractLoader(String contractPath) {
        this.contractRoot = new File(contractPath);
        if (!contractRoot.exists()) {
            contractRoot.mkdirs();
        }
        load();
    }

    private void load() {
        try (Stream<Path> filePathStream= Files.walk(Paths.get(String.valueOf(contractRoot)))) {
            filePathStream.forEach(contractPath -> {
                if (Files.isRegularFile(contractPath)) {
                    contractList.add(contractPath);
                }

                System.out.println("contractPath" + contractPath);

                File contractFile = new File(String.valueOf(contractPath));
                byte[] classData;
                try (FileInputStream inputStream = new FileInputStream(contractFile)) {
                    classData = new byte[Math.toIntExact(contractFile.length())];
                    inputStream.read(classData);

                    Class contract = defineClass(null, classData, 0, classData.length);
                    new ContractManager(classData, contract);
                } catch (IOException e) {
                    log.warn(e.getMessage());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Path> getContractList() {
        return contractList;
    }

}
