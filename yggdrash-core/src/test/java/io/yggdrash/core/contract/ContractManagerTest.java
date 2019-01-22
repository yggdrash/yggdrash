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

import io.yggdrash.common.config.DefaultConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContractManagerTest {
    Logger log = LoggerFactory.getLogger(ContractManagerTest.class);



    @Test
    public void loadTest() {
        DefaultConfig defaultConfig = new DefaultConfig();
        log.debug(defaultConfig.getContractPath());
        ContractManager manager = new ContractManager(defaultConfig.getContractPath());
        Path sampleContract = contractSample();
        if(sampleContract == null) {
            return;
        }
        String fileName = sampleContract.getFileName().toString();
        fileName = fileName.replaceAll(".class","");

        ContractId sample = ContractId.of(fileName);
        ContractMeta meta = manager.getContractById(sample);
        assert fileName.equals(meta.getContractId().toString());

        log.debug(meta.getContract().getSimpleName());

    }


    private Path contractSample() {
        DefaultConfig defaultConfig = new DefaultConfig();
        log.debug(defaultConfig.getContractPath());
        Path path = Paths.get(String.valueOf(defaultConfig.getContractPath()));
        try (Stream<Path> filePathStream = Files.walk(path)) {
            Optional<Path> file = filePathStream.filter(f -> {return !(new File(String.valueOf(f))).isDirectory();})
                    .findFirst();
            if(file.isPresent()) {
                return file.get();
            }

        } catch (IOException e) {
                e.printStackTrace();
        }
        return null;
    }

}
